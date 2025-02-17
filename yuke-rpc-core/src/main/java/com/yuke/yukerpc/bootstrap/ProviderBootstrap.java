package com.yuke.yukerpc.bootstrap;

import com.yuke.yukerpc.RpcApplication;
import com.yuke.yukerpc.config.RegistryConfig;
import com.yuke.yukerpc.config.RpcConfig;
import com.yuke.yukerpc.model.ServiceMetaInfo;
import com.yuke.yukerpc.model.ServiceRegisterInfo;
import com.yuke.yukerpc.registry.LocalRegistry;
import com.yuke.yukerpc.registry.Registry;
import com.yuke.yukerpc.registry.RegistryFactory;
import com.yuke.yukerpc.server.tcp.VertxTcpServer;

import java.util.List;

/**
 * 服务提供者启动类（初始化）
 */
public class ProviderBootstrap {

    /**
     * 初始化
     */
    public static void init(List<ServiceRegisterInfo<?>> serviceRegisterInfoList) {
        // RPC 框架初始化（配置和注册中心）
        RpcApplication.init();
        // 全局配置
        final RpcConfig rpcConfig = RpcApplication.getRpcConfig();
        //注册服务
        for (ServiceRegisterInfo<?> serviceRegisterInfo : serviceRegisterInfoList) {
            String serviceName =serviceRegisterInfo.getServiceName();
            // 本地注册
            LocalRegistry.register(serviceName,serviceRegisterInfo.getBean());

            //注册服务到注册中心
            RegistryConfig registryConfig = rpcConfig.getRegistryConfig();
            Registry registry = RegistryFactory.getInstance(registryConfig.getRegistry());
            ServiceMetaInfo serviceMetaInfo = new ServiceMetaInfo();
            serviceMetaInfo.setServiceName(serviceName);
            serviceMetaInfo.setServiceHost(rpcConfig.getServerHost());
            serviceMetaInfo.setServicePort(rpcConfig.getServerPort());
//          serviceMetaInfo.setServiceAddress(serviceMetaInfo.getServiceAddress());
            try {
                registry.register(serviceMetaInfo);
            }catch (Exception e){
                throw new RuntimeException(serviceName + " 服务注册失败", e);
            }

            //启动 TCP 服务
            VertxTcpServer vertxTcpServer = new VertxTcpServer();
            vertxTcpServer.doStart(rpcConfig.getServerPort());
            //启动web服务
//          VertxHttpServer httpServer = new VertxHttpServer();
//          httpServer.doStart(rpcConfig.getServerPort());
        }
    }
}
