package com.mallx.auth.context;

import cn.dev33.satoken.stp.StpUtil;
import com.mallx.common.context.CurrentUserProvider;
import com.mallx.common.enums.ResultCode;
import com.mallx.common.exception.BusinessException;
import org.springframework.stereotype.Component;

/**
 * 基于 Sa-Token 的当前用户上下文实现。
 */
@Component
public class SaTokenCurrentUserProvider implements CurrentUserProvider {

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
        return Long.valueOf(StpUtil.getLoginId().toString());
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
