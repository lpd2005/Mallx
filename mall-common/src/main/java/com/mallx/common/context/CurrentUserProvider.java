package com.mallx.common.context;

/**
 * 当前登录用户上下文的公共契约。
 *
 * <p>业务模块只依赖此接口，不感知 Sa-Token 等具体认证框架。</p>
 */
public interface CurrentUserProvider {

    /**
     * 获取当前已登录用户 ID。
     *
     * @return 当前用户 ID
     * @throws com.mallx.common.exception.BusinessException 未登录时抛出
     */
    Long requireUserId();

    /**
     * 判断当前请求是否存在有效登录会话。
     *
     * @return 已登录返回 {@code true}
     */
    boolean isLogin();
}
