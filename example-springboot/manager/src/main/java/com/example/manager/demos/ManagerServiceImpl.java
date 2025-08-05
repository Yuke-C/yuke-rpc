package com.example.manager.demos;

import com.example.common.service.BossService;
import com.example.common.service.EmployeeService;
import com.example.common.service.ManagerService;
import com.yuke.yukerpc.springboot.starter.annotation.RpcReference;
import com.yuke.yukerpc.springboot.starter.annotation.RpcService;

import java.sql.Time;

@RpcService
public class ManagerServiceImpl implements ManagerService {

    @RpcReference
    BossService bossService;
    @RpcReference
    EmployeeService employeeService;

    @Override
    public String helloBoss() {
        System.out.println("hello,Boss");
        return bossService.goToWork();
    }

    @Override
    public String working() {
        System.out.println("Boss,I'm working");
        return employeeService.working();
    }

    @Override
    public String retryStrategyTest() {
        try {
            Thread.sleep(20000);
        }catch (Exception e) {
            throw new RuntimeException();
        }
        return "success";
    }
}
