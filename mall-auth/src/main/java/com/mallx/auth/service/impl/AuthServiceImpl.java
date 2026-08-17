package com.mallx.auth.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mallx.auth.dto.LoginRequest;
import com.mallx.auth.entity.AuthAccount;
import com.mallx.auth.enums.AccountStatus;
import com.mallx.auth.mapper.AuthAccountMapper;
import com.mallx.auth.service.AuthService;
import com.mallx.auth.vo.CurrentUserResponse;
import com.mallx.auth.vo.LoginResponse;
import com.mallx.common.enums.ResultCode;
import com.mallx.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * 基于账号密码和 Sa-Token 的认证服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthAccountMapper authAccountMapper;

    private final PasswordEncoder passwordEncoder;

    @Override
    public LoginResponse login(LoginRequest request) {
        AuthAccount account = authAccountMapper.selectOne(new LambdaQueryWrapper<AuthAccount>()
                .eq(AuthAccount::getUsername, request.getUsername()));
        if (account == null || !passwordEncoder.matches(request.getPassword(), account.getPasswordHash())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "登录名或密码错误");
        }
        if (!Objects.equals(AccountStatus.ENABLED.getValue(), account.getStatus())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已被禁用");
        }

        // Sa-Token 负责生成、保存并向 Redis 同步当前登录会话。
        StpUtil.login(account.getId());
        log.info("User login succeeded, userId={}", account.getId());
        return new LoginResponse(account.getId(), StpUtil.getTokenName(), StpUtil.getTokenValue());
    }

    @Override
    public void logout() {
        if (StpUtil.isLogin()) {
            log.info("User logout, userId={}", currentUserId());
            StpUtil.logout();
        }
    }

    @Override
    public CurrentUserResponse currentUser() {
        if (!StpUtil.isLogin()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        Long userId = currentUserId();
        AuthAccount account = authAccountMapper.selectById(userId);
        if (account == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "登录账号不存在");
        }
        return new CurrentUserResponse(account.getId(), account.getUsername());
    }

    /**
     * 将 Sa-Token 保存的登录身份统一转换为项目使用的 Long 型用户 ID。
     *
     * @return 当前登录用户 ID
     */
    private Long currentUserId() {
        return Long.valueOf(StpUtil.getLoginId().toString());
    }
}
