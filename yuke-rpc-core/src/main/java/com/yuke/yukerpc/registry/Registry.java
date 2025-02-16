package com.yuke.yukerpc.registry;

import com.yuke.yukerpc.config.RegistryConfig;
import com.yuke.yukerpc.model.ServiceMetaInfo;

import java.util.List;

public interface Registry {

    /**
     * 初始化
     */
    void init(RegistryConfig registryConfig);

    /**
     * 注册服务（服务端）
     * @param serviceMetaInfo
     * @throws Exception
     */
    void register(ServiceMetaInfo serviceMetaInfo) throws Exception;

    /**
     * 注销服务（服务端）
     * @param serviceMetaInfo
     */
    void unRegister(ServiceMetaInfo serviceMetaInfo);

    /**
     * 服务发现（获取某服务的所有节点，消费端）
     * @param serviceKey
     * @return
     */
    List<ServiceMetaInfo> serviceDiscovery(String serviceKey);

    /**
     * 服务销毁
     */
    void destroy();

    /**
     * 心跳检测（服务端）
     */
    void heartBeat();

    /**
     * 监听（消费端）
     * @param serviceNodeKey
     */
    void watch(String serviceNodeKey,String serviceKey);
}
