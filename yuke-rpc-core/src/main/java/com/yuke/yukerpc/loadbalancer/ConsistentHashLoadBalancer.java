package com.yuke.yukerpc.loadbalancer;

import com.yuke.yukerpc.model.ServiceMetaInfo;

import java.util.*;

/**
 * 一致性哈希负载均衡器
 */
public class ConsistentHashLoadBalancer implements LoadBalancer{

    /**
     * 一致性 Hash 环，存放虚拟节点
     */
    private final HashMap<String,TreeMap<Integer,ServiceMetaInfo>> map=new HashMap<>();

    /**
     * 虚拟节点数
     */
    public static final int VIRTUAL_NODE_NUM=100;

    @Override
    public ServiceMetaInfo select(Map<String, Object> requestParams, List<ServiceMetaInfo> serviceMetaInfoList) {
        if (serviceMetaInfoList.isEmpty()) {
            return null;
        }
        //构建一致性Hash环
        String serviceName = serviceMetaInfoList.get(0).getServiceName();
        TreeMap<Integer, ServiceMetaInfo> virtualNodes;
        if (!map.containsKey(serviceName)){
            virtualNodes = new TreeMap<>();
            map.put(serviceName,virtualNodes);
        }
        virtualNodes = map.get(serviceName);

        //哈希环最大哈希值
        int maxHash=0;
        Random random = new Random();

        //构建虚拟节点环
        for (ServiceMetaInfo serviceMetaInfo : serviceMetaInfoList) {
            for (int i = 0; i < VIRTUAL_NODE_NUM; i++) {
                int hash = getHash(serviceMetaInfo.getServiceAddress() + "#" + i+"-"+random.nextInt());
                maxHash=Math.max(maxHash,hash);
                virtualNodes.put(hash,serviceMetaInfo);
            }
        }

        // 获取调用请求的 hash 值
        int hash=getHash(requestParams)%maxHash;

        // 选择最接近且大于等于调用请求 hash 值的虚拟节点
        Map.Entry<Integer, ServiceMetaInfo> entry = virtualNodes.ceilingEntry(hash);
        if (entry == null){
            // 如果没有大于等于调用请求 hash 值的虚拟节点，则返回环首部的节点
            entry=virtualNodes.firstEntry();
        }

        return entry.getValue();
    }

    /**
     * Hash算法
     * @param key
     * @return
     */
    private int getHash(Object key){
        return key.hashCode()>0?key.hashCode():-key.hashCode();
    }
}
