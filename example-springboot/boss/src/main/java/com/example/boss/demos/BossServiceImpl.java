package com.example.boss.demos;

import com.example.common.service.BossService;
import com.example.common.service.ManagerService;
import com.yuke.yukerpc.springboot.starter.annotation.RpcReference;
import com.yuke.yukerpc.springboot.starter.annotation.RpcService;

@RpcService
public class BossServiceImpl implements BossService {

    @RpcReference
    ManagerService managerService;

    @Override
    public String goToWork() {
        System.out.println("hello,go to work");
        return managerService.working();
    }
}
