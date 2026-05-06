package com.sky.service;

import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;

public interface UserService {

    /**
     * 微信登錄功能呢
     * @param userLoginDTO 用戶微信登錄的DTO
     * @return 一個User對象
     */
    User wxLogin(UserLoginDTO userLoginDTO);
}
