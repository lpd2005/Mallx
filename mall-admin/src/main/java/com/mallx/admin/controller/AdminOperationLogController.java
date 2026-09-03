package com.mallx.admin.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;

import com.mallx.admin.dto.*;
import com.mallx.admin.service.*;
import com.mallx.admin.vo.*;
import com.mallx.common.result.*;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

/** 审计只读接口，不能通过后台删除审计记录。 */
@RestController
@RequestMapping("/admin/operation-logs")
@RequiredArgsConstructor
@SaCheckPermission("audit:read")
public class AdminOperationLogController {
    private final AdminQueryService queryService;

    @GetMapping
    public Result<PageResult<AdminOperationLogVO>> page(
            @RequestParam(required = false) Long operatorId,
            @Valid @ModelAttribute AdminPageQuery query) {
        return Result.success(queryService.logs(operatorId, query));
    }
}
