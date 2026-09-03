package com.mallx.admin.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mallx.admin.dto.RoleRequest;
import com.mallx.admin.mapper.AdminRbacMapper;
import com.mallx.admin.service.*;
import com.mallx.admin.support.AdminIds;
import com.mallx.auth.entity.*;
import com.mallx.auth.mapper.AuthRoleMapper;
import com.mallx.common.enums.ResultCode;
import com.mallx.common.exception.BusinessException;

import lombok.RequiredArgsConstructor;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/** 普通角色可配置，保留系统角色不可改删；角色删除与授权替换使用同一行锁。 */
@Service
@RequiredArgsConstructor
public class AdminRoleServiceImpl extends ServiceImpl<AuthRoleMapper, AuthRole>
        implements AdminRoleService {
    private final AdminRbacMapper rbacMapper;
    private final AdminPermissionService permissionService;

    /** 创建普通角色；抢全局锁串行化，角色编码唯一冲突转为业务异常。 */
    @Override
    @Transactional(
            rollbackFor = Exception.class,
            isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public AuthRole createRole(RoleRequest request) {
        lockRbac();
        validate(request);
        AuthRole role = BeanUtil.toBean(request, AuthRole.class);
        role.setCreatedAt(LocalDateTime.now());
        role.setUpdatedAt(role.getCreatedAt());
        try {
            save(role);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "角色编码已存在");
        }
        return role;
    }

    /** 更新角色编码、名称与状态；重新启用时校验角色已绑权限不超出操作人自身权限，防止绕过授权校验。 */
    @Override
    @Transactional(
            rollbackFor = Exception.class,
            isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public AuthRole updateRole(Long roleId, RoleRequest request) {
        Long systemRoleId = lockRbac();
        requireEditable(roleId, systemRoleId);
        validate(request);
        AuthRole role = requireRole(roleId);
        if (Objects.equals(request.getStatus(), 1)) {
            requireOwnedPermissions(rbacMapper.selectRolePermissionIds(roleId));
        }
        BeanUtil.copyProperties(request, role);
        role.setUpdatedAt(LocalDateTime.now());
        try {
            updateById(role);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "角色编码已存在");
        }
        return role;
    }

    /** 删除角色；已关联账号的角色必须先解除关联，删除前清空角色权限绑定。 */
    @Override
    @Transactional(
            rollbackFor = Exception.class,
            isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void deleteRole(Long roleId) {
        Long systemRoleId = lockRbac();
        requireEditable(roleId, systemRoleId);
        requireRole(roleId);
        if (rbacMapper.countRoleAccounts(roleId) > 0)
            throw new BusinessException(ResultCode.BAD_REQUEST, "角色已关联账号，请先解除关联");
        rbacMapper.deleteRolePermissions(roleId);
        removeById(roleId);
    }

    /** 查询角色当前绑定的权限ID列表。 */
    @Override
    public List<Long> permissionIds(Long roleId) {
        requireRole(roleId);
        return rbacMapper.selectRolePermissionIds(roleId);
    }

    /** 全量替换角色权限：权限必须全部存在且不超出操作人自身权限范围，最后删旧插新。 */
    @Override
    @Transactional(
            rollbackFor = Exception.class,
            isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void assignPermissions(Long roleId, List<Long> permissionIds) {
        Long systemRoleId = lockRbac();
        requireEditable(roleId, systemRoleId);
        requireRole(roleId);
        List<Long> uniqueIds = AdminIds.distinct(permissionIds);
        requireOwnedPermissions(uniqueIds);
        rbacMapper.deleteRolePermissions(roleId);
        for (Long permissionId : uniqueIds) rbacMapper.insertRolePermission(roleId, permissionId);
    }

    /** 以系统角色行锁串行化RBAC管理操作，防止并发删除角色导致悬空关联。 */
    private Long lockRbac() {
        Long roleId = rbacMapper.lockSystemRole();
        if (roleId == null) throw new BusinessException(ResultCode.BAD_REQUEST, "请先执行后台RBAC初始化脚本");
        return roleId;
    }

    /** 授权和启用角色都要检查权限范围，避免通过重新启用高权限角色绕过授权校验。 */
    private void requireOwnedPermissions(List<Long> permissionIds) {
        if (permissionIds.isEmpty()) return;
        List<AuthPermission> permissions = permissionService.listByIds(permissionIds);
        if (permissions.size() != permissionIds.size())
            throw new BusinessException(ResultCode.BAD_REQUEST, "包含不存在的权限");
        Set<String> ownedCodes = new HashSet<>(StpUtil.getPermissionList());
        if (permissions.stream()
                .anyMatch(permission -> !ownedCodes.contains(permission.getPermissionCode())))
            throw new BusinessException(ResultCode.FORBIDDEN, "不能授予或启用自己未拥有的权限");
    }

    /** 校验目标角色不是保留的系统角色，系统角色不可修改或删除。 */
    private void requireEditable(Long roleId, Long systemRoleId) {
        if (Objects.equals(roleId, systemRoleId))
            throw new BusinessException(ResultCode.FORBIDDEN, "保留系统角色不允许修改或删除");
    }

    /** 按ID查询角色，不存在时抛出未找到异常。 */
    private AuthRole requireRole(Long roleId) {
        AuthRole role = getById(roleId);
        if (role == null) throw new BusinessException(ResultCode.NOT_FOUND, "角色不存在");
        return role;
    }

    /** 校验角色请求参数：编码为大写格式且非保留编码，名称与状态合法。 */
    private void validate(RoleRequest request) {
        if (request == null
                || request.getRoleCode() == null
                || !request.getRoleCode().matches("[A-Z][A-Z0-9_]{1,63}")
                || "SYSTEM_ADMIN".equalsIgnoreCase(request.getRoleCode())
                || request.getRoleName() == null
                || request.getRoleName().isBlank()
                || request.getRoleName().length() > 64
                || !Set.of(0, 1).contains(request.getStatus() == null ? -1 : request.getStatus()))
            throw new BusinessException(ResultCode.BAD_REQUEST, "角色参数无效或使用了保留编码");
    }
}
