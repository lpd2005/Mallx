package com.mallx.aftersale.controller;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.stp.StpUtil;

import com.mallx.aftersale.service.AfterSaleService;
import com.mallx.common.context.CurrentUserProvider;
import com.mallx.common.exception.BusinessException;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/** 管理权限在调用业务Service前校验，普通登录用户不能退款。 */
class AfterSaleManagementControllerTest {
    @Test
    void permissionFailureDoesNotInvokeRefundService() {
        AfterSaleService service = mock(AfterSaleService.class);
        CurrentUserProvider users = mock(CurrentUserProvider.class);
        when(users.requireUserId()).thenReturn(1L);
        AfterSaleManagementController controller =
                new AfterSaleManagementController(service, users);
        try (MockedStatic<StpUtil> permissions = mockStatic(StpUtil.class)) {
            permissions
                    .when(() -> StpUtil.checkPermission("after-sale:manage"))
                    .thenThrow(new NotPermissionException("after-sale:manage"));
            assertThrows(BusinessException.class, () -> controller.refund("AS1"));
            verifyNoInteractions(service);
        }
    }

    @Test
    void permittedManagerCanInvokeRefund() {
        AfterSaleService service = mock(AfterSaleService.class);
        CurrentUserProvider users = mock(CurrentUserProvider.class);
        when(users.requireUserId()).thenReturn(99L);
        AfterSaleManagementController controller =
                new AfterSaleManagementController(service, users);
        try (MockedStatic<StpUtil> permissions = mockStatic(StpUtil.class)) {
            controller.refund("AS1");
            permissions.verify(() -> StpUtil.checkPermission("after-sale:manage"));
            verify(service).refund(99L, "AS1");
        }
    }
}
