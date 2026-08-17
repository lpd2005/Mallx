package com.mallx.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * RBAC 角色定义，例如 USER、ADMIN、OPERATOR。
 */
@Data
@TableName("auth_role")
public class AuthRole {

    @TableId(value = "id")
    private Long id;

    /** 稳定、唯一的角色编码。 */
    private String roleCode;

    /** 管理端展示的角色名称。 */
    private String roleName;

    /** 角色状态：1 启用，0 禁用。 */
    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
