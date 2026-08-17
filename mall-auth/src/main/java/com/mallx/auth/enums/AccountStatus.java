package com.mallx.auth.enums;

import lombok.Getter;

/**
 * 认证账号状态。
 */
@Getter
public enum AccountStatus {

    /** 账号可正常登录。 */
    ENABLED(1),
    /** 账号被禁用，禁止创建新会话。 */
    DISABLED(0);

    /**
     * -- GETTER --
     */
    private final int value;

    AccountStatus(int value) {
        this.value = value;
    }

}
