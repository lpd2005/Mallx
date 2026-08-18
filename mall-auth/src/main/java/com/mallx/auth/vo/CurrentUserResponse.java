package com.mallx.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 当前登录账号的安全展示信息。
 */
@Data
@AllArgsConstructor
public class CurrentUserResponse {

    private Long userId;

    /** 脱敏后的登录手机号，例如 138****8000。 */
    private String maskedPhone;
}
