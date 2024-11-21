package com.yuke.example.common.service;

import com.yuke.example.common.model.User;

public interface UserService {

    /**
     * 获取用户
     * @param user
     * @return
     */
    User getUser(User user);

    /**
     * 新方法 - 获取数字
     */
    default short getNumber(){
        return 1;
    }
}
