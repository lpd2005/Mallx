package com.mallx.auth.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 登录成功后返回的会话信息。
 */
@Data
@Builder
public class LoginResponse {

    /** 当前登录用户的身份主键。 */
    private Long userId;

    /** Sa-Token 的 HTTP 请求头名称。 */
    private String tokenName;

    /** 短期Access Token；保留tokenValue字段名以兼容现有客户端。 */
    private String tokenValue;

    /** Access Token剩余有效秒数。 */
    private Long accessTokenExpiresIn;

    /** 只允许提交到/auth/refresh的长期Refresh Token。 */
    private String refreshToken;

    /** Refresh Token剩余有效秒数。 */
    private Long refreshTokenExpiresIn;
}
