package com.mallx.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Data;
import lombok.ToString;

/** MVP手机号密码注册参数；暂不接入短信验证，不接收角色、账号状态或会员等级。 */
@Data
public class RegisterRequest {
    /** 与登录接口保持一致，当前仅支持中国大陆手机号。 */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 原始密码不做trim，避免悄悄改变用户凭证；BCrypt字节限制在服务层补充校验。 */
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 64, message = "密码长度必须为8到64位")
    @ToString.Exclude
    private String password;

    /** 可选，留空时由用户模块生成默认昵称。 */
    @Size(max = 32, message = "昵称长度不能超过32位")
    private String nickname;
}
