package com.yuke.example.common.service;

import com.yuke.example.common.model.User;

public interface UserService {

    /**
     * 获取用户
     * @param name
     * @return
     */
    String getUser(String name);

    /**
     * 新方法 - 获取数字
     */
    short getNumber();
}
