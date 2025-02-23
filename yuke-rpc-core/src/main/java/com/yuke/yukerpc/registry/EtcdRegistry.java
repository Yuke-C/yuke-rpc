package com.yuke.yukerpc.registry;

import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.cron.CronUtil;
import cn.hutool.cron.task.Task;
import cn.hutool.json.JSONUtil;
import com.yuke.yukerpc.config.RegistryConfig;
import com.yuke.yukerpc.model.ServiceMetaInfo;
import io.etcd.jetcd.*;
import io.etcd.jetcd.lease.LeaseKeepAliveResponse;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.PutOption;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.watch.WatchEvent;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class EtcdRegistry implements Registry{

    private Client client;

    public KV kvClient;

    /**
     * 本机注册的节点key信息（用于维护续期）
     */
    private final Set<String> localRegisterNodeKeySet=new ConcurrentHashSet<>();

    /**
     * 根节点
     */
    public static final String ETCD_ROOT_PATH="/rpc/";

    /**
     * 注册中心服务缓存
     */
    private final RegistryServiceMultiCache registryServiceCache=new RegistryServiceMultiCache();

    /**
     * 添加用于保存租约ID和ServiceMetaInfo的映射
     */
    private final Map<String, Long> keyToLeaseIdMap = new ConcurrentHashMap<>();
    private final Map<String, ServiceMetaInfo> keyToServiceMetaInfoMap = new ConcurrentHashMap<>();

    /**
     * 正在监听的key集合
     */
    private final Set<String> watchingKeySet=new ConcurrentHashSet<>();

    /**
     * 保存Watcher以便后续关闭
     */
    private final Map<String, Watch.Watcher> watcherMap = new ConcurrentHashMap<>();

    @Override
    public void init(RegistryConfig registryConfig) {
        client = Client.builder()
                .endpoints(registryConfig.getAddress())
                .connectTimeout(Duration.ofMillis(registryConfig.getTimeout()))
                .build();
        kvClient = this.client.getKVClient();
        heartBeat();
    }

    @Override
    public void register(ServiceMetaInfo serviceMetaInfo) throws Exception {

        Lease leaseClient = client.getLeaseClient();

        //创建一个30秒的租约
        long leaseId = leaseClient.grant(30).get().getID();

        //设置要存储的键值对
        String registerKey = ETCD_ROOT_PATH + serviceMetaInfo.getServiceNodeKey();
        ByteSequence key = ByteSequence.from(registerKey, StandardCharsets.UTF_8);
        ByteSequence value = ByteSequence.from(JSONUtil.toJsonStr(serviceMetaInfo), StandardCharsets.UTF_8);

        //将键值对与租约关联起来，并设置过期时间
        PutOption putOption = PutOption.builder().withLeaseId(leaseId).build();
        kvClient.put(key,value,putOption).get();

        // 保存租约ID和服务元信息
        keyToLeaseIdMap.put(registerKey, leaseId);
        keyToServiceMetaInfoMap.put(registerKey, serviceMetaInfo);

        //添加节点信息到本地缓存
        localRegisterNodeKeySet.add(registerKey);

    }

    @Override
    public void unRegister(ServiceMetaInfo serviceMetaInfo) {
        String registerKey=ETCD_ROOT_PATH+serviceMetaInfo.getServiceNodeKey();
        kvClient.delete(ByteSequence.from(ETCD_ROOT_PATH+serviceMetaInfo.getServiceNodeKey(),StandardCharsets.UTF_8));

        // 删除租约ID和服务元信息
        keyToLeaseIdMap.remove(registerKey);
        keyToServiceMetaInfoMap.remove(registerKey);

        //从本地缓存移除
        localRegisterNodeKeySet.remove(registerKey);
    }

    @Override
    public List<ServiceMetaInfo> serviceDiscovery(String serviceKey) {
        //优先从缓存获取服务
        List<ServiceMetaInfo> cacheServiceMetaInfoList = registryServiceCache.readCache(serviceKey);
        if (cacheServiceMetaInfoList != null && cacheServiceMetaInfoList.size()!=0) {
            return cacheServiceMetaInfoList;
        }

        //前缀搜索，结尾一定要加‘/’
        String searchPrefix=ETCD_ROOT_PATH+serviceKey+"/";
        try{
            GetOption getOption = GetOption.builder().isPrefix(true).build();
            List<KeyValue> keyValues = kvClient.get(
                    ByteSequence.from(searchPrefix, StandardCharsets.UTF_8),
                    getOption)
                    .get()
                    .getKvs();
            //解析服务信息
            List<ServiceMetaInfo> serviceMetaInfoList = keyValues.stream()
                    .map(keyValue -> {
                        String key = keyValue.getKey().toString(StandardCharsets.UTF_8);
                        //监听key的变化
                        watch(key,serviceKey);
                        String value = keyValue.getValue().toString(StandardCharsets.UTF_8);
                        return JSONUtil.toBean(value, ServiceMetaInfo.class);
                    }).collect(Collectors.toList());

            registryServiceCache.writeCache(serviceKey,serviceMetaInfoList);
            return serviceMetaInfoList;
        }catch (Exception e){
            throw new RuntimeException("获取服务列表失败",e);
        }

    }

    @Override
    public void destroy() {
        System.out.println("当前节点下线");
        //下线节点
        //遍历本节点所有的key
        for (String key : localRegisterNodeKeySet) {
            try {
                kvClient.delete(ByteSequence.from(key,StandardCharsets.UTF_8)).get();
            }catch (Exception e){
                throw new RuntimeException(key+"节点下线失败");
            }
        }
        CronUtil.stop();
        // 关闭所有Watcher
        watcherMap.values().forEach(Watch.Watcher::close);
        watcherMap.clear();
        watchingKeySet.clear();
        //释放资源
        if (kvClient!=null){
            kvClient.close();
        }
        if (client!=null){
            client.close();
        }
    }

    @Override
    public void heartBeat() {
        CronUtil.schedule("*/10 * * * * *", (Task) () -> {
            for (String key : localRegisterNodeKeySet) {
                Long leaseId = keyToLeaseIdMap.get(key);
                if (leaseId == null) continue;

                try {
                    // 发送一次保活请求
                    LeaseKeepAliveResponse response = client.getLeaseClient().keepAliveOnce(leaseId).get();
                    System.out.println(key+"续约成功，租约ID: " + leaseId + ", 新的TTL: " + response.getTTL());
                } catch (Exception e) {
                    System.err.println("续约失败，尝试重新注册: " + key);
                    ServiceMetaInfo serviceMetaInfo = keyToServiceMetaInfoMap.get(key);
                    if (serviceMetaInfo != null) {
                        try {
                            // 尝试撤销旧租约（允许租约不存在）
                            try {
                                client.getLeaseClient().revoke(leaseId).get();
                            } catch (ExecutionException ex) {
                                if (ex.getCause() instanceof StatusRuntimeException) {
                                    StatusRuntimeException sre = (StatusRuntimeException) ex.getCause();
                                    if (sre.getStatus().getCode() == Status.Code.NOT_FOUND) {
                                        System.out.println(key+"租约已自动过期，无需撤销: " + leaseId);
                                    } else {
                                        throw ex;
                                    }
                                } else {
                                    throw ex;
                                }
                            } finally {
                                // 强制清理本地记录
                                keyToLeaseIdMap.remove(key);
                                keyToServiceMetaInfoMap.remove(key);
                                localRegisterNodeKeySet.remove(key);
                            }
                            // 重新注册服务
                            register(serviceMetaInfo);
                            System.out.println("重新注册成功: " + key);
                        } catch (Exception ex) {
                            System.err.println("重新注册失败: " + key);
                            ex.printStackTrace();
                        }
                    }
                }
            }
        });
        CronUtil.setMatchSecond(true);
        CronUtil.start();
    }

    @Override
    public void watch(String servicNodeKey,String serviceKey) {
        String watchPrefix=ETCD_ROOT_PATH+serviceKey+"/";
        boolean newWatch = watchingKeySet.add(watchPrefix);
        if (!newWatch) {
            return;
        }

        Watch watchClient = client.getWatchClient();
        WatchOption watchOption = WatchOption.builder()
                .isPrefix(true) // 启用前缀监听
                .build();

        Watch.Watcher watcher = watchClient.watch(
                ByteSequence.from(watchPrefix, StandardCharsets.UTF_8),
                watchOption,
                response -> {
                    for (WatchEvent event : response.getEvents()) {
                        switch (event.getEventType()) {
                            case DELETE:
                            case PUT:
                                registryServiceCache.clearCache(serviceKey);
                                break;
                            default:
                                break;
                        }
                    }
                });
        // 保存Watcher以便后续关闭
        watcherMap.put(watchPrefix, watcher);
    }

}


