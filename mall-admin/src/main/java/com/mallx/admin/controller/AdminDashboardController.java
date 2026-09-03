package com.mallx.admin.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;

import com.mallx.admin.dto.*;
import com.mallx.admin.service.*;
import com.mallx.admin.vo.*;
import com.mallx.common.context.CurrentUserProvider;
import com.mallx.common.result.*;

import lombok.RequiredArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** 后台首页和会话信息；不保存重复统计表，直接聚合现有交易记录。 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminDashboardController {
    private final CurrentUserProvider currentUserProvider;
    private final AdminStatisticsService statisticsService;

    @GetMapping("/me")
    @SaCheckPermission("admin:access")
    public Result<AdminSessionVO> me() {
        return Result.success(
                AdminSessionVO.builder()
                        .userId(currentUserProvider.requireUserId())
                        .roles(StpUtil.getRoleList())
                        .permissions(StpUtil.getPermissionList())
                        .build());
    }

    @GetMapping("/dashboard")
    @SaCheckPermission("dashboard:read")
    public Result<AdminDashboardVO> summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate endDate) {
        return Result.success(statisticsService.summary(startDate, endDate));
    }

    @GetMapping("/dashboard/top-products")
    @SaCheckPermission("dashboard:read")
    public Result<List<AdminTopProductVO>> topProducts(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate endDate,
            @RequestParam(defaultValue = "10") int limit) {
        return Result.success(statisticsService.topProducts(startDate, endDate, limit));
    }
}
