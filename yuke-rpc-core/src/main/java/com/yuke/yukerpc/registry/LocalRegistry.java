package com.yuke.yukerpc.registry;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LocalRegistry {

    /**
     * 注册信息存储
     */
    public static final Map<String,Object> map = new ConcurrentHashMap<>();

    /**
     * 注册服务
     * @param serviceName
     * @param bean
     */
    public static void register(String serviceName,Object bean){
        map.put(serviceName, bean);
    }

    /**
     * 获取服务
     * @param serviceName
     * @return
     */
    public static Object get(String serviceName){
        return map.get(serviceName);
    }

    /**
     * 删除服务
     * @param serviceName
     */
    public static void remove(String serviceName){
        map.remove(serviceName);
    }
}
