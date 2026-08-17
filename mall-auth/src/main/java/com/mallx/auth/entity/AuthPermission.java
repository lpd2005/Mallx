package com.mallx.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * RBAC 权限点定义，例如 {@code product:update}。
 */
@Data
@TableName("auth_permission")
public class AuthPermission {

    @TableId(value = "id")
    private Long id;

    /** 稳定、唯一的权限编码。 */
    private String permissionCode;

    /** 权限展示名称。 */
    private String permissionName;

    /** 权限对应的资源路径，可为空。 */
    private String resource;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
