package com.mallx.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import lombok.Data;

import java.time.LocalDate;

/** 更新当前用户资料的请求。 */
@Data
public class UpdateUserProfileRequest {

    /** 用户昵称，完整更新时必填。 */
    @NotBlank(message = "昵称不能为空")
    @Size(max = 32, message = "昵称长度不能超过 32 位")
    private String nickname;

    /** 图片完整访问URL；沿用OssKey字段名，不再保存对象Key。空值表示未设置图片。 */
    @Size(max = 1024, message = "图片地址长度不能超过1024位")
    @org.hibernate.validator.constraints.URL(protocol = "https", message = "图片地址必须为完整HTTPS URL")
    private String avatarOssKey;

    /** 性别：0 未知、1 男、2 女。 */
    @NotNull(message = "性别不能为空")
    @Min(value = 0, message = "性别取值不正确")
    @Max(value = 2, message = "性别取值不正确")
    private Integer gender;

    /** 生日，可为空，但不能晚于当前日期。 */
    @PastOrPresent(message = "生日不能晚于当前日期")
    private LocalDate birthday;

    /** 个人简介；空字符串会统一转换为 {@code null}。 */
    @Size(max = 255, message = "个人简介长度不能超过 255 位")
    private String bio;
}
