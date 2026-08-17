package com.mallx.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录成功后返回的会话信息。
 */
@Data
@AllArgsConstructor
public class LoginResponse {

    /** 当前登录用户的身份主键。 */
    private Long userId;

    /** Sa-Token 的 HTTP 请求头名称。 */
    private String tokenName;

    /** 当前会话令牌。 */
    private String tokenValue;
}
