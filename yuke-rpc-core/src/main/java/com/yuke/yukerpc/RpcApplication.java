package com.yuke.yukerpc;

import com.yuke.yukerpc.config.RegistryConfig;
import com.yuke.yukerpc.config.RpcConfig;
import com.yuke.yukerpc.constant.RpcConstant;
import com.yuke.yukerpc.registry.Registry;
import com.yuke.yukerpc.registry.RegistryFactory;
import com.yuke.yukerpc.utils.ConfigUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * RPC框架应用
 * 相当于holder,存放了项目全局用到的变量。双检锁单例模式实现
 */
@Slf4j
public class RpcApplication {

    private static volatile RpcConfig rpcConfig;

    /**
     * 框架初始化，支持传入自定义配置
     * @param newRpcConfig
     */
    public static void init(RpcConfig newRpcConfig) {
        rpcConfig=newRpcConfig;
        log.info("rpc init,config={}",newRpcConfig.toString());
        //注册中心初始化
        RegistryConfig registryConfig = rpcConfig.getRegistryConfig();
        //根据配置信息获取注册中心实例
        Registry registry = RegistryFactory.getInstance(registryConfig.getRegistry());
        //根据注册中心地址创建注册中心连接信息
        registry.init(registryConfig);
        //创建并注册Shutdown Hook,JVM退出时执行操作(放到日志打印后执行未生效 ？？？ 2024-6-30)
        Runtime.getRuntime().addShutdownHook(new Thread(registry::destroy));

        log.info("registry init,config={}",registryConfig);

    }

    /**
     * 框架初始化，支持传入自定义配置
     * @param
     */

    public static void init() {
        RpcConfig newRpcConfig;
        try {
            //加载用户自定义配置文件
             newRpcConfig = ConfigUtils.loadConfig(RpcConfig.class, RpcConstant.DEFAULT_CONFIG_PREFIX);
        }catch (Exception e){
             newRpcConfig = new RpcConfig();
        }
        init(newRpcConfig);
    }

    /**
     * 获取配置
     * @return
     */
    public static RpcConfig getRpcConfig(){
        if (rpcConfig==null){
            synchronized (RpcApplication.class){
                if (rpcConfig==null){
                    init();
                }
            }
        }
        return rpcConfig;
    }
}
