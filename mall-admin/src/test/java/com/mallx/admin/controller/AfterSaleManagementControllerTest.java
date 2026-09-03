package com.mallx.admin.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.stp.StpUtil;

import com.mallx.admin.service.AdminOperationLogService;
import com.mallx.admin.support.SaTokenMvcTestSupport;
import com.mallx.aftersale.service.AfterSaleService;

import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

/** 权限由原生注解在HTTP入口校验，不再直接调用Controller方法来模拟注解生效。 */
class AfterSaleManagementControllerTest extends SaTokenMvcTestSupport {
    @Test
    void permissionFailureDoesNotInvokeRefundOrAuditService() throws Exception {
        AfterSaleService service = mock(AfterSaleService.class);
        AdminOperationLogService operations = mock(AdminOperationLogService.class);
        doThrow(new NotPermissionException("after-sale:manage"))
                .when(permissionLogic)
                .checkPermissionAnd("after-sale:manage");
        mvc(new AfterSaleManagementController(operations, service, users))
                .perform(post("/admin/after-sales/AS1/refund"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service, operations);
    }

    @Test
    void permittedManagerCanRefundWithoutRepeatedControllerPermissionCheck() throws Exception {
        AfterSaleService service = mock(AfterSaleService.class);
        AdminOperationLogService operations = mock(AdminOperationLogService.class);
        when(users.requireUserId()).thenReturn(99L);
        when(operations.execute(
                        anyString(),
                        anyString(),
                        org.mockito.ArgumentMatchers.<Supplier<Object>>any()))
                .thenAnswer(invocation -> invocation.<Supplier<Object>>getArgument(2).get());
        mvc(new AfterSaleManagementController(operations, service, users))
                .perform(post("/admin/after-sales/AS1/refund"))
                .andExpect(status().isOk());
        verify(permissionLogic, times(1)).checkPermissionAnd("after-sale:manage");
        token.verify(() -> StpUtil.checkPermission("after-sale:manage"), never());
        verify(service).refund(99L, "AS1");
        verify(operations)
                .execute(
                        eq("after-sale:refund"),
                        eq("AS1"),
                        org.mockito.ArgumentMatchers.<Supplier<Object>>any());
    }
}
