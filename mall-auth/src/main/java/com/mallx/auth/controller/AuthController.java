package com.mallx.auth.controller;

import com.mallx.auth.dto.LoginRequest;
import com.mallx.auth.dto.RegisterRequest;
import com.mallx.auth.service.AuthService;
import com.mallx.auth.vo.CurrentUserResponse;
import com.mallx.auth.vo.LoginResponse;
import com.mallx.auth.vo.RegisterResponse;
import com.mallx.common.annotation.Public;
import com.mallx.common.result.Result;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 认证与会话接口。 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 游客注册普通账号；不在注册接口创建Redis登录会话。 */
    @Public
    @PostMapping("/register")
    public Result<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(authService.register(request));
    }

    /**
     * 手机号与密码登录。
     *
     * @param request 登录参数
     * @return 会话令牌
     */
    @Public
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    /**
     * 注销当前会话。
     *
     * @return 无数据成功响应
     */
    @Public
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.success();
    }

    /**
     * 获取当前登录账号。
     *
     * @return 当前账号安全信息
     */
    @GetMapping("/me")
    public Result<CurrentUserResponse> currentUser() {
        return Result.success(authService.currentUser());
    }

    // TODO 双令牌设计
}
