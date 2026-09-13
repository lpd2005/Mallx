package com.mallx.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 使用Refresh Token换取一对新令牌。 */
@Data
public class RefreshTokenRequest {
    @NotBlank(message = "Refresh Token不能为空")
    @Size(max = 128, message = "Refresh Token格式不正确")
    private String refreshToken;
}
