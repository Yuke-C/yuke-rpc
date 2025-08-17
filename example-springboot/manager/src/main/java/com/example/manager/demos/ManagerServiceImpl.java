package com.example.manager.demos;

import com.example.common.model.Employee;
import com.example.common.service.BossService;
import com.example.common.service.EmployeeService;
import com.example.common.service.ManagerService;
import com.yuke.yukerpc.springboot.starter.annotation.RpcReference;
import com.yuke.yukerpc.springboot.starter.annotation.RpcService;

import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

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

    @Override
    public List<Employee> getAllEmployee() {
        ArrayList<Employee> employees = new ArrayList<>();
        for (int i = 1; i < 4; i++) {
            Employee employee = new Employee();
            employee.setName("employee"+i);
            employee.setAge(20+i);
            employees.add(employee);
        }
        return employees;
    }
}
