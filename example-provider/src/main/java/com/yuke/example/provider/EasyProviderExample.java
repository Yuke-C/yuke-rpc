package com.yuke.example.provider;

import com.yuke.example.common.service.UserService;
import com.yuke.yukerpc.registry.LocalRegistry;
import com.yuke.yukerpc.server.VertxHttpServer;

public class EasyProviderExample {

    public static void main(String[] args) {

        //注册服务
        LocalRegistry.register(UserService.class.getName(),UserServiceImpl.class);

        //启动web服务器
        VertxHttpServer httpServer = new VertxHttpServer();
        httpServer.doStart(8080);
    }
}
