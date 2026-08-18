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
import com.mallx.common.context.CurrentUserProvider;
import com.mallx.common.enums.ResultCode;
import com.mallx.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * 基于手机号、密码和 Sa-Token 的认证服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthAccountMapper authAccountMapper;

    private final PasswordEncoder passwordEncoder;

    /** 复用统一当前用户上下文，避免业务模块各自调用 Sa-Token。 */
    private final CurrentUserProvider currentUserProvider;

    @Override
    public LoginResponse login(LoginRequest request) {
        AuthAccount account = authAccountMapper.selectOne(new LambdaQueryWrapper<AuthAccount>()
                .eq(AuthAccount::getPhone, request.getPhone().trim()));
        if (account == null || !passwordEncoder.matches(request.getPassword(), account.getPasswordHash())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "手机号或密码错误");
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
        if (currentUserProvider.isLogin()) {
            log.info("User logout, userId={}", currentUserProvider.requireUserId());
            StpUtil.logout();
        }
    }

    @Override
    public CurrentUserResponse currentUser() {
        Long userId = currentUserProvider.requireUserId();
        AuthAccount account = authAccountMapper.selectById(userId);
        if (account == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "登录账号不存在");
        }
        return new CurrentUserResponse(account.getId(), maskPhone(account.getPhone()));
    }

    /**
     * 对手机号中间四位进行脱敏，避免当前账号接口直接暴露完整登录凭证。
     *
     * @param phone 完整手机号
     * @return 脱敏手机号
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

}
