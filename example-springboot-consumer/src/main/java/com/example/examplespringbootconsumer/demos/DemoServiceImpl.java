package com.example.examplespringbootconsumer.demos;


import com.yuke.example.common.model.Manager;
import com.yuke.example.common.model.User;
import com.yuke.example.common.service.ManagerService;
import com.yuke.example.common.service.UserService;
import com.yuke.yukerpc.springboot.starter.annotation.RpcReference;
import org.springframework.stereotype.Service;

@Service
public class DemoServiceImpl {

    @RpcReference
    UserService userService;

    @RpcReference
    ManagerService managerService;

    public String name(String name){
        return managerService.getManager(name);
    }

    public String number(){

        return managerService.getAge()+"num:"+userService.getNumber();
    }
}
