package com.yuke.examplespringbootconsumer.consumer;

import com.yuke.example.common.model.User;
import com.yuke.example.common.service.UserService;
import com.yuke.yukerpc.springboot.starter.annotation.RpcReference;
import org.springframework.stereotype.Service;

@Service
public class ExampleServiceImpl {

    @RpcReference
    UserService userService;

    public void test() {
        User user = new User();
        user.setName("yuke");
        User resultUser = userService.getUser(user);
        System.out.println(resultUser.getName());
    }
}
