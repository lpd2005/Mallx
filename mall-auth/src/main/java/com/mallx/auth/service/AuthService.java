package com.mallx.auth.service;

import com.mallx.auth.dto.LoginRequest;
import com.mallx.auth.vo.CurrentUserResponse;
import com.mallx.auth.vo.LoginResponse;

/**
 * 登录会话与认证账号服务。
 */
public interface AuthService {

    /**
     * 校验手机号和密码并创建 Sa-Token 会话。
     *
     * @param request 登录请求
     * @return 新会话的令牌信息
     */
    LoginResponse login(LoginRequest request);

    /** 注销当前会话。 */
    void logout();

    /**
     * 获取当前登录账号的安全展示信息。
     *
     * @return 当前账号
     */
    CurrentUserResponse currentUser();
}
