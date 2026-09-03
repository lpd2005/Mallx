package com.mallx.admin.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.stp.StpUtil;

import com.mallx.admin.controller.*;
import com.mallx.admin.service.*;
import com.mallx.admin.support.SaTokenMvcTestSupport;
import com.mallx.common.enums.ResultCode;
import com.mallx.common.exception.BusinessException;
import com.mallx.common.result.Result;
import com.mallx.coupon.service.CouponTemplateService;
import com.mallx.order.service.OrderService;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;

/** 通过实际MVC注册链验证原生鉴权、路径范围、执行顺序和后台默认拒绝策略。 */
class SaTokenAuthorizationTest extends SaTokenMvcTestSupport {
    @RestController
    @SaCheckPermission("product:manage")
    static class ProductEndpoint {
        @GetMapping("/admin/example")
        public Result<Void> read() {
            return Result.success();
        }

        @GetMapping("/admin/combined")
        @SaCheckPermission("inventory:manage")
        public Result<Void> combined() {
            return Result.success();
        }

        @GetMapping("/admin/ignored")
        @SaIgnore
        public Result<Void> ignored() {
            return Result.success();
        }
    }

    @RestController
    static class UnannotatedEndpoints {
        @GetMapping({"/admin", "/admin/missing"})
        public Result<Void> missing() {
            return Result.success();
        }

        @GetMapping("/products/example")
        public Result<Void> front() {
            return Result.success();
        }

        @GetMapping({"/auth/login", "/auth/logout"})
        public Result<Void> publicAuth() {
            return Result.success();
        }
    }

    @Test
    void unannotatedAdminEndpointIsDenied() throws Exception {
        mvc(new UnannotatedEndpoints())
                .perform(get("/admin/missing"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
        verifyNoInteractions(permissionLogic);
    }

    @Test
    void bareAdminPathIsAlsoProtected() throws Exception {
        mvc(new UnannotatedEndpoints()).perform(get("/admin")).andExpect(status().isForbidden());
        token.verify(() -> StpUtil.checkPermission("admin:access"));
    }

    @Test
    void ordinaryUserCannotCallExistingCouponRoute() throws Exception {
        CouponTemplateService coupons = mock(CouponTemplateService.class);
        token.when(() -> StpUtil.checkPermission("admin:access"))
                .thenThrow(new NotPermissionException("admin:access"));
        mvc(new CouponTemplateController(mock(AdminOperationLogService.class), coupons))
                .perform(get("/admin/coupon-templates"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(coupons, permissionLogic);
    }

    @Test
    void entryPermissionAloneCannotShipOrder() throws Exception {
        OrderService orders = mock(OrderService.class);
        AdminOperationLogService logs = mock(AdminOperationLogService.class);
        doThrow(new NotPermissionException("order:manage"))
                .when(permissionLogic)
                .checkPermissionAnd("order:manage");
        mvc(new OrderManagementController(logs, mock(AdminQueryService.class), orders, users))
                .perform(post("/admin/orders/O1/ship"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(orders, logs);
    }

    @Test
    void grantedPermissionExecutesNativeClassAnnotation() throws Exception {
        mvc(new ProductEndpoint()).perform(get("/admin/example")).andExpect(status().isOk());
        token.verify(StpUtil::checkLogin);
        token.verify(() -> StpUtil.checkPermission("admin:access"));
        verify(permissionLogic).checkPermissionAnd("product:manage");
        verify(users, times(1)).requireUserId();
    }

    @Test
    void nativeClassAndMethodPermissionsBothApply() throws Exception {
        mvc(new ProductEndpoint()).perform(get("/admin/combined")).andExpect(status().isOk());
        verify(permissionLogic).checkPermissionAnd("product:manage");
        verify(permissionLogic).checkPermissionAnd("inventory:manage");
    }

    @Test
    void methodPermissionDoesNotOverrideDeniedClassPermission() throws Exception {
        doThrow(new NotPermissionException("product:manage"))
                .when(permissionLogic)
                .checkPermissionAnd("product:manage");
        mvc(new ProductEndpoint())
                .perform(get("/admin/combined"))
                .andExpect(status().isForbidden());
        verify(permissionLogic, never()).checkPermissionAnd("inventory:manage");
    }

    @Test
    void sessionEndpointDoesNotRequireDashboardPermission() throws Exception {
        AdminStatisticsService statistics = mock(AdminStatisticsService.class);
        doThrow(new NotPermissionException("dashboard:read"))
                .when(permissionLogic)
                .checkPermissionAnd("dashboard:read");
        mvc(new AdminDashboardController(users, statistics))
                .perform(get("/admin/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(100));
        verify(permissionLogic).checkPermissionAnd("admin:access");
        verify(permissionLogic, never()).checkPermissionAnd("dashboard:read");
        verifyNoInteractions(statistics);
    }

    @Test
    void dashboardStillRequiresItsOwnPermission() throws Exception {
        AdminStatisticsService statistics = mock(AdminStatisticsService.class);
        doThrow(new NotPermissionException("dashboard:read"))
                .when(permissionLogic)
                .checkPermissionAnd("dashboard:read");
        MockMvc mvc = mvc(new AdminDashboardController(users, statistics));
        mvc.perform(get("/admin/dashboard")).andExpect(status().isForbidden());
        mvc.perform(get("/admin/dashboard/top-products")).andExpect(status().isForbidden());
        verifyNoInteractions(statistics);
    }

    @Test
    void saIgnoreCannotBypassAdminGuards() throws Exception {
        mvc(new ProductEndpoint()).perform(get("/admin/ignored")).andExpect(status().isForbidden());
        token.verify(StpUtil::checkLogin);
        token.verify(() -> StpUtil.checkPermission("admin:access"));
        verifyNoInteractions(permissionLogic);
    }

    @Test
    void loginFailureStopsBeforePermissionChecks() throws Exception {
        token.when(StpUtil::checkLogin)
                .thenThrow(new NotLoginException("login", NotLoginException.NOT_TOKEN, "请先登录"));
        mvc(new ProductEndpoint())
                .perform(get("/admin/example"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
        token.verify(() -> StpUtil.checkPermission("admin:access"), never());
        verifyNoInteractions(users, permissionLogic);
    }

    @Test
    void disabledAccountStopsBeforePermissionChecks() throws Exception {
        when(users.requireUserId()).thenThrow(new BusinessException(ResultCode.FORBIDDEN, "账号已禁用"));
        mvc(new ProductEndpoint())
                .perform(get("/admin/example"))
                .andExpect(jsonPath("$.code").value(403));
        token.verify(() -> StpUtil.checkPermission("admin:access"), never());
        verifyNoInteractions(permissionLogic);
    }

    @Test
    void frontReadKeepsLoginRequirementWithoutAdminPermission() throws Exception {
        mvc(new UnannotatedEndpoints())
                .perform(get("/products/example"))
                .andExpect(status().isOk());
        token.verify(StpUtil::checkLogin);
        verify(users).requireUserId();
        token.verify(() -> StpUtil.checkPermission("admin:access"), never());
        verifyNoInteractions(permissionLogic);
    }

    @Test
    void publicLoginAndLogoutRemainExcluded() throws Exception {
        MockMvc mvc = mvc(new UnannotatedEndpoints());
        mvc.perform(get("/auth/login")).andExpect(status().isOk());
        mvc.perform(get("/auth/logout")).andExpect(status().isOk());
        token.verifyNoInteractions();
        verifyNoInteractions(users, permissionLogic);
    }

    @Test
    void contextPathDoesNotBypassAdminEntryCheck() throws Exception {
        token.when(() -> StpUtil.checkPermission("admin:access"))
                .thenThrow(new NotPermissionException("admin:access"));
        mvc(new ProductEndpoint())
                .perform(get("/mallx/admin/example").contextPath("/mallx"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(permissionLogic);
    }

    @Test
    void everyAdminRouteDeclaresNativePermissionAndCustomClassesAreGone() throws Exception {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        int routeCount = 0;
        for (org.springframework.beans.factory.config.BeanDefinition definition :
                scanner.findCandidateComponents("com.mallx.admin.controller")) {
            Class<?> controller = Class.forName(definition.getBeanClassName());
            for (Method method : controller.getDeclaredMethods()) {
                if (AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class)
                        == null) continue;
                routeCount++;
                SaCheckPermission permission = method.getAnnotation(SaCheckPermission.class);
                if (permission == null)
                    permission = controller.getAnnotation(SaCheckPermission.class);
                assertNotNull(permission, controller.getSimpleName() + "." + method.getName());
                assertTrue(permission.value().length > 0);
                assertFalse(method.isAnnotationPresent(SaIgnore.class));
                assertFalse(controller.isAnnotationPresent(SaIgnore.class));
            }
        }
        assertTrue(routeCount >= 40);
        for (String className :
                new String[] {
                    "com.mallx.admin.annotation.AdminPermission",
                    "com.mallx.admin.config.AdminPermissionInterceptor",
                    "com.mallx.admin.config.AdminWebConfiguration",
                    "com.mallx.order.controller.OrderManagementController",
                    "com.mallx.coupon.controller.CouponTemplateController",
                    "com.mallx.aftersale.controller.AfterSaleManagementController"
                }) {
            assertThrows(ClassNotFoundException.class, () -> Class.forName(className));
        }
    }
}
