package com.mallx.admin.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.bean.BeanUtil;

import com.mallx.admin.dto.*;
import com.mallx.admin.service.*;
import com.mallx.admin.vo.*;
import com.mallx.common.result.*;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 角色CRUD、账号角色关联和角色权限关联；权限目录由代码与种子SQL维护。 */
@RestController
@RequestMapping("/admin/rbac")
@RequiredArgsConstructor
@SaCheckPermission("rbac:manage")
public class AdminRbacController {
    private final AdminAccountService accountService;
    private final AdminRoleService roleService;
    private final AdminPermissionService permissionService;
    private final AdminQueryService queryService;
    private final AdminOperationLogService operationLogService;

    @GetMapping("/roles")
    public Result<PageResult<AdminRoleVO>> roles(@Valid @ModelAttribute AdminPageQuery query) {
        return Result.success(queryService.roles(query));
    }

    @PostMapping("/roles")
    public Result<AdminRoleVO> create(@Valid @RequestBody RoleRequest request) {
        return Result.success(
                BeanUtil.toBean(
                        operationLogService.execute(
                                "rbac:create-role", null, () -> roleService.createRole(request)),
                        AdminRoleVO.class));
    }

    @PutMapping("/roles/{roleId}")
    public Result<AdminRoleVO> update(
            @PathVariable Long roleId, @Valid @RequestBody RoleRequest request) {
        return Result.success(
                BeanUtil.toBean(
                        operationLogService.execute(
                                "rbac:update-role",
                                roleId.toString(),
                                () -> roleService.updateRole(roleId, request)),
                        AdminRoleVO.class));
    }

    @DeleteMapping("/roles/{roleId}")
    public Result<Void> delete(@PathVariable Long roleId) {
        operationLogService.execute(
                "rbac:delete-role", roleId.toString(), () -> roleService.deleteRole(roleId));
        return Result.success();
    }

    @GetMapping("/permissions")
    public Result<List<AdminPermissionVO>> permissions() {
        return Result.success(
                BeanUtil.copyToList(permissionService.list(), AdminPermissionVO.class));
    }

    @GetMapping("/roles/{roleId}/permissions")
    public Result<List<Long>> rolePermissions(@PathVariable Long roleId) {
        return Result.success(roleService.permissionIds(roleId));
    }

    @PutMapping("/roles/{roleId}/permissions")
    public Result<Void> assignPermissions(
            @PathVariable Long roleId, @Valid @RequestBody AssignIdsRequest request) {
        operationLogService.execute(
                "rbac:assign-permissions",
                roleId.toString(),
                () -> roleService.assignPermissions(roleId, request.getIds()));
        return Result.success();
    }

    @GetMapping("/users/{userId}/roles")
    public Result<List<Long>> userRoles(@PathVariable Long userId) {
        return Result.success(accountService.roleIds(userId));
    }

    @PutMapping("/users/{userId}/roles")
    public Result<Void> assignRoles(
            @PathVariable Long userId, @Valid @RequestBody AssignIdsRequest request) {
        operationLogService.execute(
                "rbac:assign-roles",
                userId.toString(),
                () -> accountService.assignRoles(userId, request.getIds()));
        return Result.success();
    }
}
