package com.yuke.example.common.service;

import com.yuke.example.common.model.Manager;

public interface ManagerService {
    /**
     * 获取用户
     * @param name
     * @return
     */
    String getManager(String name);

    /**
     * 新方法 - 获取年龄
     */
    short getAge();
}
