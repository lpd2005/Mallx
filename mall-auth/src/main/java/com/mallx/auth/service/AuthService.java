package com.mallx.auth.service;

import com.mallx.auth.dto.LoginRequest;
import com.mallx.auth.dto.RefreshTokenRequest;
import com.mallx.auth.dto.RegisterRequest;
import com.mallx.auth.vo.CurrentUserResponse;
import com.mallx.auth.vo.LoginResponse;
import com.mallx.auth.vo.RegisterResponse;

/** 登录会话与认证账号服务。 */
public interface AuthService {

    /** 创建普通账号及用户资料，注册成功后由用户单独登录。 */
    RegisterResponse register(RegisterRequest request);

    /**
     * 校验手机号和密码并创建 Sa-Token 会话。
     *
     * @param request 登录请求
     * @return 新会话的令牌信息
     */
    LoginResponse login(LoginRequest request);

    /** 使用一次性Refresh Token轮换一对新令牌。 */
    LoginResponse refresh(RefreshTokenRequest request);

    /** 注销当前会话。 */
    void logout();

    /**
     * 获取当前登录账号的安全展示信息。
     *
     * @return 当前账号
     */
    CurrentUserResponse currentUser();
}
