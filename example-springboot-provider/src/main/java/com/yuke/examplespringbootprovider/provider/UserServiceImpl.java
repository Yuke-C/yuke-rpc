package com.yuke.examplespringbootprovider.provider;

import com.yuke.example.common.model.User;
import com.yuke.example.common.service.UserService;
import com.yuke.yukerpc.springboot.starter.annotation.RpcService;
import org.springframework.stereotype.Service;

@Service
@RpcService
public class UserServiceImpl implements UserService {
    @Override
    public String getUser(String name) {
        System.out.println("用户名：" + name);
        return "provider:"+name;
    }

    @Override
    public short getNumber() {
        return 1;
    }
}
