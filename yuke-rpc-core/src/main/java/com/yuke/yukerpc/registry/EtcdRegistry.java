package com.yuke.yukerpc.registry;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.cron.CronUtil;
import cn.hutool.cron.task.Task;
import cn.hutool.json.JSONUtil;
import com.yuke.yukerpc.config.RegistryConfig;
import com.yuke.yukerpc.model.ServiceMetaInfo;
import io.etcd.jetcd.*;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.PutOption;
import io.etcd.jetcd.watch.WatchEvent;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class EtcdRegistry implements Registry{

    private Client client;

    public KV kvClient;

    /**
     * 本机注册的节点key信息（用于维护续期）
     */
    private final Set<String> localRegisterNodeKeySet=new HashSet<>();

    /**
     * 根节点
     */
    public static final String ETCD_ROOT_PATH="/rpc/";

    /**
     * 注册中心服务缓存
     */
    private final RegistryServiceMultiCache registryServiceCache=new RegistryServiceMultiCache();

    /**
     * 正在监听的key集合
     */
    private final Set<String> watchingKeySet=new ConcurrentHashSet<>();

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

        //添加节点信息到本地缓存
        localRegisterNodeKeySet.add(registerKey);

    }

    @Override
    public void unRegister(ServiceMetaInfo serviceMetaInfo) {
        String registerKey=ETCD_ROOT_PATH+serviceMetaInfo.getServiceNodeKey();
        kvClient.delete(ByteSequence.from(ETCD_ROOT_PATH+serviceMetaInfo.getServiceNodeKey(),StandardCharsets.UTF_8));
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
        //10秒续签一次
        CronUtil.schedule("*/10 * * * * *", new Task() {
            @Override
            public void execute() {
                for (String key : localRegisterNodeKeySet) {
                    try {
                        List<KeyValue> keyValues = kvClient.get(ByteSequence.from(key, StandardCharsets.UTF_8))
                                .get()
                                .getKvs();
                        //该节点已过期（需要重启节点才能重新注册）
                        if (CollUtil.isEmpty(keyValues)){
                            continue;
                        }
                        //节点未过期，重新注册（相当于续签）
                        KeyValue keyValue = keyValues.get(0);
                        String value = keyValue.getValue().toString(StandardCharsets.UTF_8);
                        ServiceMetaInfo serviceMetaInfo = JSONUtil.toBean(value, ServiceMetaInfo.class);
                        register(serviceMetaInfo);
                    }catch (Exception e){
                        throw new RuntimeException(key+"续签失败",e);
                    }
                }
            }
        });
        //支持秒级别定时任务
        CronUtil.setMatchSecond(true);
        CronUtil.start();
    }

    @Override
    public void watch(String serviceNodeKey,String serviceKey) {
        Watch watchClient = client.getWatchClient();
        boolean newWatch = watchingKeySet.add(serviceNodeKey);
        if(newWatch){
            watchClient.watch(ByteSequence.from(serviceNodeKey,StandardCharsets.UTF_8),response ->{
                for (WatchEvent event : response.getEvents()) {
                    switch (event.getEventType()){
                        //key删除时触发
                        case DELETE:
                            registryServiceCache.clearCache(serviceKey);
                            break;
                        case PUT:
                        default:
                            break;
                    }
                }
            });

        }
    }

}


