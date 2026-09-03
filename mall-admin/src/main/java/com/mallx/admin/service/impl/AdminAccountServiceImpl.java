package com.mallx.admin.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mallx.admin.dto.AdminPageQuery;
import com.mallx.admin.mapper.AdminRbacMapper;
import com.mallx.admin.service.*;
import com.mallx.admin.support.AdminIds;
import com.mallx.admin.support.AdminPages;
import com.mallx.admin.vo.AdminAccountVO;
import com.mallx.auth.entity.*;
import com.mallx.auth.mapper.AuthAccountMapper;
import com.mallx.common.context.CurrentUserProvider;
import com.mallx.common.enums.ResultCode;
import com.mallx.common.exception.BusinessException;
import com.mallx.common.result.PageResult;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/** 账号管理；账号禁用由auth统一登录拦截器在下一次请求时生效。 */
@Service
@RequiredArgsConstructor
public class AdminAccountServiceImpl extends ServiceImpl<AuthAccountMapper, AuthAccount>
        implements AdminAccountService {
    private final AdminRbacMapper rbacMapper;
    private final AdminRoleService roleService;
    private final AdminPermissionService permissionService;
    private final CurrentUserProvider currentUserProvider;

    /** 分页查询账号列表，仅查询必要列，支持手机号模糊与状态过滤，按ID倒序。 */
    @Override
    public PageResult<AdminAccountVO> pageAccounts(AdminPageQuery query) {
        return AdminPages.result(
                lambdaQuery()
                        .select(
                                AuthAccount::getId,
                                AuthAccount::getPhone,
                                AuthAccount::getStatus,
                                AuthAccount::getCreatedAt)
                        .like(
                                StrUtil.isNotBlank(query.getKeyword()),
                                AuthAccount::getPhone,
                                query.getKeyword())
                        .eq(query.getStatus() != null, AuthAccount::getStatus, query.getStatus())
                        .orderByDesc(AuthAccount::getId)
                        .page(AdminPages.page(query)),
                AdminAccountVO.class);
    }

    /** 查询单个账号详情，不存在时抛出未找到异常。 */
    @Override
    public AdminAccountVO getAccount(Long accountId) {
        return BeanUtil.toBean(requireAccount(accountId), AdminAccountVO.class);
    }

    /** 启用或禁用指定账号；禁用后由统一登录拦截器在该账号下一次请求时生效。 */
    @Override
    @Transactional(
            rollbackFor = Exception.class,
            isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void changeStatus(Long accountId, Integer status) {
        requireMutableAccount(accountId);
        if (status == null || (status != 0 && status != 1))
            throw new BusinessException(ResultCode.BAD_REQUEST, "账号状态只能是0或1");
        lambdaUpdate()
                .eq(AuthAccount::getId, accountId)
                .set(AuthAccount::getStatus, status)
                .set(AuthAccount::getUpdatedAt, LocalDateTime.now())
                .update();
    }

    /** 查询账号已绑定的角色ID列表。 */
    @Override
    public List<Long> roleIds(Long accountId) {
        requireAccount(accountId);
        return rbacMapper.selectAccountRoleIds(accountId);
    }

    /**
     * 全量替换账号角色：先校验目标账号可变更、角色存在且启用，
     * 再确保待分配角色的权限不超出操作人自身权限（防提权），最后删旧插新。
     */
    @Override
    @Transactional(
            rollbackFor = Exception.class,
            isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void assignRoles(Long accountId, List<Long> roleIds) {
        Long systemRoleId = requireMutableAccount(accountId);
        List<Long> uniqueIds = AdminIds.distinct(roleIds);
        if (uniqueIds.contains(systemRoleId))
            throw new BusinessException(ResultCode.FORBIDDEN, "系统管理员只能由数据库受控初始化");
        if (!uniqueIds.isEmpty()) {
            List<AuthRole> roles = roleService.listByIds(uniqueIds);
            if (roles.size() != uniqueIds.size()
                    || roles.stream().anyMatch(role -> !Objects.equals(role.getStatus(), 1)))
                throw new BusinessException(ResultCode.BAD_REQUEST, "包含不存在或已禁用的角色");
            Set<String> ownedCodes = new HashSet<>(StpUtil.getPermissionList());
            for (Long roleId : uniqueIds) {
                List<Long> permissionIds = rbacMapper.selectRolePermissionIds(roleId);
                if (!permissionIds.isEmpty()
                        && permissionService.listByIds(permissionIds).stream()
                                .anyMatch(
                                        permission ->
                                                !ownedCodes.contains(
                                                        permission.getPermissionCode())))
                    throw new BusinessException(ResultCode.FORBIDDEN, "不能分配权限超出自己范围的角色");
            }
        }
        rbacMapper.deleteAccountRoles(accountId);
        for (Long roleId : uniqueIds) rbacMapper.insertAccountRole(accountId, roleId);
    }

    /** 禁止修改自己或保留管理员，防止误操作导致后台无人可用。 */
    private Long requireMutableAccount(Long accountId) {
        Long systemRoleId = rbacMapper.lockSystemRole();
        if (systemRoleId == null)
            throw new BusinessException(ResultCode.BAD_REQUEST, "请先执行后台RBAC初始化脚本");
        if (Objects.equals(accountId, currentUserProvider.requireUserId()))
            throw new BusinessException(ResultCode.FORBIDDEN, "不能修改自己的状态或角色");
        requireAccount(accountId);
        if (rbacMapper.selectAccountRoleIds(accountId).contains(systemRoleId))
            throw new BusinessException(ResultCode.FORBIDDEN, "保留系统管理员账号需通过受控数据库操作维护");
        return systemRoleId;
    }

    /** 按ID查询账号（限定安全列），不存在时抛出未找到异常。 */
    private AuthAccount requireAccount(Long accountId) {
        AuthAccount account =
                lambdaQuery()
                        .select(
                                AuthAccount::getId,
                                AuthAccount::getPhone,
                                AuthAccount::getStatus,
                                AuthAccount::getCreatedAt)
                        .eq(AuthAccount::getId, accountId)
                        .one();
        if (account == null) throw new BusinessException(ResultCode.NOT_FOUND, "账号不存在");
        return account;
    }
}
