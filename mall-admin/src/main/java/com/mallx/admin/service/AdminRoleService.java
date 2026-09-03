package com.mallx.admin.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.mallx.admin.dto.RoleRequest;
import com.mallx.auth.entity.AuthRole;

import java.util.List;

/** RBAC角色与权限关联管理。 */
public interface AdminRoleService extends IService<AuthRole> {
    AuthRole createRole(RoleRequest request);

    AuthRole updateRole(Long roleId, RoleRequest request);

    void deleteRole(Long roleId);

    List<Long> permissionIds(Long roleId);

    void assignPermissions(Long roleId, List<Long> permissionIds);
}
