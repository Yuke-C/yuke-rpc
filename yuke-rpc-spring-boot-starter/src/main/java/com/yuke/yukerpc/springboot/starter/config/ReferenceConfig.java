package com.yuke.yukerpc.springboot.starter.config;

import com.yuke.yukerpc.springboot.starter.annotation.RpcReference;

import java.util.HashMap;
import java.util.Map;

public class ReferenceConfig {

    private Class<?> interfaceClass;

    private String serviceVersion;

    private String loadBalancer;

    private String retryStrategy;

    private String tolerantStrategy;

    private boolean mock;

    // 从注解创建配置对象
    public static ReferenceConfig fromAnnotation(RpcReference rpcReference) {
        ReferenceConfig config = new ReferenceConfig();
        config.interfaceClass = rpcReference.interfaceClass();
        config.serviceVersion = rpcReference.serviceVersion();
        config.loadBalancer = rpcReference.loadBalancer();
        config.retryStrategy = rpcReference.retryStrategy();
        config.tolerantStrategy = rpcReference.tolerantStrategy();
        config.mock = rpcReference.mock();
        return config;
    }

    // 获取所有配置属性的 Map 表示
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("interfaceClass", interfaceClass);
        map.put("serviceVersion", serviceVersion);
        map.put("loadBalancer", loadBalancer);
        map.put("retryStrategy", retryStrategy);
        map.put("tolerantStrategy", tolerantStrategy);
        map.put("mock", mock);
        return map;
    }

    public Class<?> getInterfaceClass() {
        return interfaceClass;
    }

    public String getServiceVersion() {
        return serviceVersion;
    }

    public String getLoadBalancer() {
        return loadBalancer;
    }

    public String getRetryStrategy() {
        return retryStrategy;
    }

    public String getTolerantStrategy() {
        return tolerantStrategy;
    }

    public boolean isMock() {
        return mock;
    }
}