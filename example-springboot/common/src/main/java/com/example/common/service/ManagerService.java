package com.example.common.service;

import com.example.common.model.Employee;

import java.util.List;

public interface ManagerService {

    String helloBoss();

    String working();

    String retryStrategyTest();

    List<Employee> getAllEmployee();
}
