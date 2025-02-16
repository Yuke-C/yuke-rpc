package com.example.examplespringbootprovider2.demos.provider;

import com.yuke.example.common.model.Manager;
import com.yuke.example.common.model.User;
import com.yuke.example.common.service.ManagerService;
import com.yuke.example.common.service.UserService;
import com.yuke.yukerpc.springboot.starter.annotation.RpcReference;
import com.yuke.yukerpc.springboot.starter.annotation.RpcService;
import org.springframework.stereotype.Service;

@Service
@RpcService
public class ManagerServiceImpl implements ManagerService {

    @RpcReference
    UserService userService;

    @Override
    public String getManager(String name) {
//        System.out.println("管理员：" + name);
//        System.out.println("用户名：" + userService.getUser(name.toUpperCase()));
        if (userService==null){
            return "11111111111111";
        }
        return "provider2:" + userService.getUser(name.toUpperCase());
    }

    @Override
    public short getAge() {
        return 9;
    }
}
