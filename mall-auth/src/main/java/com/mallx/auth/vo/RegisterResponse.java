package com.mallx.auth.vo;

import lombok.Builder;
import lombok.Data;

/** 注册结果只返回用户ID，不返回密码摘要、不自动签发登录令牌。 */
@Data
@Builder
public class RegisterResponse {
    private Long userId;
}
