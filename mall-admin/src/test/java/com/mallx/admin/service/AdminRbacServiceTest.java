package com.mallx.admin.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.dev33.satoken.stp.StpUtil;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.mallx.admin.mapper.AdminRbacMapper;
import com.mallx.admin.service.impl.*;
import com.mallx.auth.entity.*;
import com.mallx.auth.mapper.*;
import com.mallx.common.context.CurrentUserProvider;
import com.mallx.common.exception.BusinessException;

import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

/** RBAC保护边界及全量替换关联的验证。 */
class AdminRbacServiceTest {
    private AdminRbacMapper rbac;
    private AdminPermissionService permissions;
    private AdminRoleService roleReader;
    private AuthAccountMapper accounts;
    private AuthRoleMapper roles;
    private CurrentUserProvider users;
    private AdminAccountServiceImpl accountService;
    private AdminRoleServiceImpl roleService;

    @BeforeAll
    static void tables() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "account-test"),
                AuthAccount.class);
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "role-test"),
                AuthRole.class);
    }

    @BeforeEach
    void setup() {
        rbac = mock(AdminRbacMapper.class);
        permissions = mock(AdminPermissionService.class);
        roleReader = mock(AdminRoleService.class);
        accounts = mock(AuthAccountMapper.class);
        roles = mock(AuthRoleMapper.class);
        users = mock(CurrentUserProvider.class);
        when(users.requireUserId()).thenReturn(1L);
        when(rbac.lockSystemRole()).thenReturn(99L);
        when(rbac.selectAccountRoleIds(anyLong())).thenReturn(List.of());
        accountService =
                new AdminAccountServiceImpl(rbac, roleReader, permissions, users) {
                    @Override
                    public Class<AuthAccount> getEntityClass() {
                        return AuthAccount.class;
                    }

                    @Override
                    public Class<AuthAccountMapper> getMapperClass() {
                        return AuthAccountMapper.class;
                    }
                };
        roleService = new AdminRoleServiceImpl(rbac, permissions);
        ReflectionTestUtils.setField(accountService, "baseMapper", accounts);
        ReflectionTestUtils.setField(roleService, "baseMapper", roles);
        AuthAccount account = new AuthAccount();
        account.setId(2L);
        account.setStatus(1);
        when(accounts.selectOne(any())).thenReturn(account);
        AuthRole role = new AuthRole();
        role.setId(3L);
        role.setStatus(1);
        when(roles.selectById(3L)).thenReturn(role);
    }

    @Test
    void cannotDisableSelf() {
        assertThrows(BusinessException.class, () -> accountService.changeStatus(1L, 0));
        verifyNoInteractions(accounts);
    }

    @Test
    void cannotDisableSystemAdministrator() {
        when(rbac.selectAccountRoleIds(2L)).thenReturn(List.of(99L));
        assertThrows(BusinessException.class, () -> accountService.changeStatus(2L, 0));
        verify(accounts, never()).update(any(AuthAccount.class), any());
    }

    @Test
    void cannotGrantSystemRoleViaHttpService() {
        assertThrows(BusinessException.class, () -> accountService.assignRoles(2L, List.of(99L)));
        verify(rbac, never()).deleteAccountRoles(anyLong());
    }

    @Test
    void missingSeedFailsClosed() {
        when(rbac.lockSystemRole()).thenReturn(null);
        assertThrows(BusinessException.class, () -> accountService.changeStatus(2L, 0));
    }

    @Test
    void invalidStatusIsRejected() {
        assertThrows(BusinessException.class, () -> accountService.changeStatus(2L, 8));
        verify(accounts, never()).update(any(AuthAccount.class), any());
    }

    @Test
    void nullRolesDoesNotClearExistingLinks() {
        assertThrows(BusinessException.class, () -> accountService.assignRoles(2L, null));
        verify(rbac, never()).deleteAccountRoles(anyLong());
    }

    @Test
    void emptyRolesExplicitlyClearsLinks() {
        accountService.assignRoles(2L, List.of());
        verify(rbac).deleteAccountRoles(2L);
        verify(rbac, never()).insertAccountRole(anyLong(), anyLong());
    }

    @Test
    void rolesAreDeduplicatedBeforeReplacingLinks() {
        AuthRole role = new AuthRole();
        role.setId(3L);
        role.setStatus(1);
        when(roleReader.listByIds(List.of(3L))).thenReturn(List.of(role));
        when(rbac.selectRolePermissionIds(3L)).thenReturn(List.of());
        try (MockedStatic<StpUtil> token = mockStatic(StpUtil.class)) {
            token.when(StpUtil::getPermissionList).thenReturn(List.of());
            accountService.assignRoles(2L, List.of(3L, 3L));
            verify(rbac).deleteAccountRoles(2L);
            verify(rbac, times(1)).insertAccountRole(2L, 3L);
        }
    }

    @Test
    void cannotAssignDisabledRole() {
        AuthRole role = new AuthRole();
        role.setId(3L);
        role.setStatus(0);
        when(roleReader.listByIds(List.of(3L))).thenReturn(List.of(role));
        assertThrows(BusinessException.class, () -> accountService.assignRoles(2L, List.of(3L)));
        verify(rbac, never()).deleteAccountRoles(anyLong());
    }

    @Test
    void assignedRoleCannotBeDeleted() {
        when(rbac.countRoleAccounts(3L)).thenReturn(1L);
        assertThrows(BusinessException.class, () -> roleService.deleteRole(3L));
        verify(rbac, never()).deleteRolePermissions(anyLong());
    }

    @Test
    void systemRoleCannotBeDeletedOrReauthorized() {
        assertThrows(BusinessException.class, () -> roleService.deleteRole(99L));
        assertThrows(BusinessException.class, () -> roleService.assignPermissions(99L, List.of()));
        verify(rbac, never()).deleteRolePermissions(anyLong());
    }

    @Test
    void unknownPermissionsCannotClearPreviousLinks() {
        when(permissions.listByIds(List.of(10L))).thenReturn(List.of());
        assertThrows(
                BusinessException.class, () -> roleService.assignPermissions(3L, List.of(10L)));
        verify(rbac, never()).deleteRolePermissions(anyLong());
    }

    @Test
    void cannotGrantPermissionOutsideOwnScope() {
        AuthPermission permission = new AuthPermission();
        permission.setId(10L);
        permission.setPermissionCode("rbac:manage");
        when(permissions.listByIds(List.of(10L))).thenReturn(List.of(permission));
        try (MockedStatic<StpUtil> token = mockStatic(StpUtil.class)) {
            token.when(StpUtil::getPermissionList).thenReturn(List.of("product:manage"));
            assertThrows(
                    BusinessException.class, () -> roleService.assignPermissions(3L, List.of(10L)));
            verify(rbac, never()).deleteRolePermissions(anyLong());
        }
    }

    @Test
    void allowedPermissionsAreReplacedOnce() {
        AuthPermission permission = new AuthPermission();
        permission.setId(10L);
        permission.setPermissionCode("product:manage");
        when(permissions.listByIds(List.of(10L))).thenReturn(List.of(permission));
        try (MockedStatic<StpUtil> token = mockStatic(StpUtil.class)) {
            token.when(StpUtil::getPermissionList).thenReturn(List.of("product:manage"));
            roleService.assignPermissions(3L, List.of(10L, 10L));
            verify(rbac).deleteRolePermissions(3L);
            verify(rbac).insertRolePermission(3L, 10L);
        }
    }
}
