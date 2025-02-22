package com.example.employee.demos;

import com.example.common.service.EmployeeService;
import com.yuke.yukerpc.springboot.starter.annotation.RpcService;


@RpcService
public class EmployeeServiceImpl implements EmployeeService {

    @Override
    public String working() {
        System.out.println("Manager,I'm working");
        return "Manager,I'm working";
    }
}
