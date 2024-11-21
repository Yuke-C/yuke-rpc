package com.yuke.yukerpc.registry;

import com.yuke.yukerpc.spi.SpiLoader;

/**
 * 注册中心工厂（用于获取注册中心对象）
 */
public class RegistryFactory {

    static {
        //读取resources下资源文件配置信息，放入loaderMap：接口名 =>（key => 实现类）
        SpiLoader.load(Registry.class);
    }

    /**
     * 默认注册中心
     */
    public static final Registry DEFAULT_REGISTRY=new EtcdRegistry();

    /**
     * 获取实例
     */
    public static Registry getInstance(String key){
        //从loaderMap中获取实例
        return SpiLoader.getInstance(Registry.class,key);
    }

}
