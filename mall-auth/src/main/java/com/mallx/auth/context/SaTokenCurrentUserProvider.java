package com.mallx.auth.context;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mallx.auth.entity.AuthAccount;
import com.mallx.auth.enums.AccountStatus;
import com.mallx.auth.mapper.AuthAccountMapper;
import com.mallx.common.context.CurrentUserProvider;
import com.mallx.common.enums.ResultCode;
import com.mallx.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

/** 基于 Sa-Token 的当前用户上下文实现。 */
@Component
@RequiredArgsConstructor
public class SaTokenCurrentUserProvider implements CurrentUserProvider {
    private final AuthAccountMapper authAccountMapper;

    /**
     * 获取当前登录用户的 Long 型身份主键。
     *
     * @return 当前用户 ID
     */
    @Override
    public Long requireUserId() {
        if (!StpUtil.isLogin()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        Long userId = Long.valueOf(StpUtil.getLoginId().toString());
        // 每次请求检查数据库状态，禁用账号即使还持有旧Token也不能继续访问。
        AuthAccount account =
                authAccountMapper.selectOne(
                        new LambdaQueryWrapper<AuthAccount>()
                                .select(AuthAccount::getId, AuthAccount::getStatus)
                                .eq(AuthAccount::getId, userId));
        if (account == null
                || !Objects.equals(account.getStatus(), AccountStatus.ENABLED.getValue())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号不存在或已禁用");
        }
        return userId;
    }

    /**
     * 委托 Sa-Token 判断当前请求的登录态。
     *
     * @return 已登录返回 {@code true}
     */
    @Override
    public boolean isLogin() {
        return StpUtil.isLogin();
    }
}
