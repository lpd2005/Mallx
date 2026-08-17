package com.mallx.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 角色与权限的多对多关联。
 */
@Data
@TableName("auth_role_permission")
public class AuthRolePermission {

    private Long roleId;

    private Long permissionId;
}
