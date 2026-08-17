package com.mallx.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 登录请求参数。
 */
@Data
public class LoginRequest {

    @NotBlank(message = "登录名不能为空")
    @Size(max = 64, message = "登录名长度不能超过 64 位")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(max = 128, message = "密码长度不能超过 128 位")
    private String password;
}
