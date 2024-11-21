package com.yuke.example.consumer;

import com.yuke.example.common.model.User;
import com.yuke.example.common.service.UserService;
import com.yuke.yukerpc.bootstrap.ConsumerBootstrap;
import com.yuke.yukerpc.config.RpcConfig;
import com.yuke.yukerpc.proxy.ServiceProxyFactory;
import com.yuke.yukerpc.utils.ConfigUtils;

public class ConsumerExample {

    public static void main(String[] args) {
        // 服务提供者初始化
        ConsumerBootstrap.init();
        // 获取代理
        UserService userService= ServiceProxyFactory.getProxy(UserService.class);
        User user = new User();
        user.setName("yuke");
        // 调用
        User newUser = userService.getUser(user);
        if (newUser!=null){
            System.out.println("用户名："+newUser.getName());
        }else {
            System.out.println("user==null");
        }
        short number = userService.getNumber();
        System.out.println(number);

    }
}
