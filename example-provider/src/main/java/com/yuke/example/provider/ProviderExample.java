package com.yuke.example.provider;

import com.yuke.example.common.service.UserService;
import com.yuke.yukerpc.RpcApplication;
import com.yuke.yukerpc.bootstrap.ProviderBootstrap;
import com.yuke.yukerpc.config.RegistryConfig;
import com.yuke.yukerpc.config.RpcConfig;
import com.yuke.yukerpc.constant.RpcConstant;
import com.yuke.yukerpc.model.ServiceMetaInfo;
import com.yuke.yukerpc.model.ServiceRegisterInfo;
import com.yuke.yukerpc.registry.LocalRegistry;
import com.yuke.yukerpc.registry.Registry;
import com.yuke.yukerpc.registry.RegistryFactory;
import com.yuke.yukerpc.server.VertxHttpServer;
import com.yuke.yukerpc.server.tcp.VertxTcpServer;
import com.yuke.yukerpc.utils.ConfigUtils;

import java.util.ArrayList;
import java.util.List;

public class ProviderExample {

    public static void main(String[] args) {
        // 要注册的服务
        List<ServiceRegisterInfo<?>> serviceRegisterInfoList = new ArrayList<>();
        ServiceRegisterInfo<UserService> serviceRegisterInfo = new ServiceRegisterInfo<>(UserService.class.getName(), UserServiceImpl.class);
        serviceRegisterInfoList.add(serviceRegisterInfo);

        // 服务提供者初始化
        ProviderBootstrap.init(serviceRegisterInfoList);
    }
}
