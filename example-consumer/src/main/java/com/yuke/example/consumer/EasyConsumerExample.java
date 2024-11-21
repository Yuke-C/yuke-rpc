package com.yuke.example.consumer;

import com.yuke.example.common.model.User;
import com.yuke.example.common.service.UserService;
import com.yuke.yukerpc.proxy.ServiceProxyFactory;

public class EasyConsumerExample {

    public static void main(String[] args) {

        UserService userService= ServiceProxyFactory.getProxy(UserService.class);

        User user = new User();
        user.setName("yuke");
        User newUser = userService.getUser(user);
        if (newUser!=null){
            System.out.println(newUser.getName());
        }else {
            System.out.println("user==null");
        }
    }
}
