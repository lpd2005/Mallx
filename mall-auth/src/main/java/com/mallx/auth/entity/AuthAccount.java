package com.mallx.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录账号与凭证信息。
 *
 * <p>用户头像、昵称、地址等业务资料归 {@code mall-user} 模块维护。</p>
 */
@Data
@TableName("auth_account")
public class AuthAccount {

    /** 用户身份主键，由 MyBatis-Plus 的 ASSIGN_ID 策略生成。 */
    @TableId(value = "id")
    private Long id;

    /** 全局唯一登录名。 */
    private String username;

    /** BCrypt 加密后的密码摘要，禁止返回给前端。 */
    private String passwordHash;

    /** 账号状态：1 启用，0 禁用。 */
    private Integer status;

    /** 创建时间。 */
    private LocalDateTime createdAt;

    /** 最后更新时间。 */
    private LocalDateTime updatedAt;
}
