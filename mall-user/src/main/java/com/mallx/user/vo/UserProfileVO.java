package com.mallx.user.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 当前用户资料响应。 */
@Data
@Builder
public class UserProfileVO {
    /** 用户ID。 */
    private Long userId;

    /** 用户昵称。 */
    private String nickname;

    /** 图片完整访问URL；沿用OssKey字段名，不再保存对象Key。空值表示未设置图片。 */
    private String avatarOssKey;

    /** 性别：0 未知、1 男、2 女。 */
    private Integer gender;

    /** 用户生日。 */
    private LocalDate birthday;

    /** 个人简介。 */
    private String bio;

    /** 资料创建时间。 */
    private LocalDateTime createdAt;

    /** 资料最后更新时间。 */
    private LocalDateTime updatedAt;
}
