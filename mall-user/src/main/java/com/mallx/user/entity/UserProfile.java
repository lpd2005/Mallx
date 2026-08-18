package com.mallx.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 用户业务资料，不包含登录凭证和账号状态。 */
@Data
@TableName("user_profile")
public class UserProfile {

    /** 用户身份主键，与认证模块的 {@code auth_account.id} 保持一致。 */
    @TableId(value = "user_id", type = IdType.INPUT)
    private Long userId;
    /** 用户对外展示的昵称。 */
    private String nickname;
    /** 头像在阿里云 OSS 中的对象 Key，不保存文件二进制。 */
    private String avatarOssKey;
    /** 性别：0 未知、1 男、2 女。 */
    private Integer gender;
    /** 用户生日。 */
    private LocalDate birthday;
    /** 用户个人简介。 */
    private String bio;
    /** 资料创建时间。 */
    private LocalDateTime createdAt;
    /** 资料最后更新时间。 */
    private LocalDateTime updatedAt;
}
