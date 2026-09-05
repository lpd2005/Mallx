package com.mallx.admin.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.bean.BeanUtil;

import com.mallx.admin.dto.*;
import com.mallx.admin.service.*;
import com.mallx.admin.vo.*;
import com.mallx.common.context.CurrentUserProvider;
import com.mallx.common.result.*;
import com.mallx.user.service.UserMembershipService;
import com.mallx.user.service.UserProfileService;
import com.mallx.user.vo.UserProfileVO;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

/** 后台用户管理，不提供密码查询、重置或代用户购买会员的接口。 */
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@SaCheckPermission("user:manage")
public class AdminUserController {
    private final AdminAccountService accountService;
    private final UserProfileService profileService;
    private final UserMembershipService membershipService;
    private final AdminOperationLogService operationLogService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    public Result<PageResult<AdminAccountVO>> page(@Valid @ModelAttribute AdminPageQuery query) {
        return Result.success(accountService.pageAccounts(query));
    }

    @GetMapping("/{userId}")
    public Result<AdminAccountVO> get(@PathVariable Long userId) {
        return Result.success(accountService.getAccount(userId));
    }

    /** getById只读查询，历史账号没有资料时返回null，不触发补建。 */
    @GetMapping("/{userId}/profile")
    public Result<UserProfileVO> profile(@PathVariable Long userId) {
        accountService.getAccount(userId);
        return Result.success(BeanUtil.toBean(profileService.getById(userId), UserProfileVO.class));
    }

    @PutMapping("/{userId}/status")
    public Result<Void> status(
            @PathVariable Long userId, @Valid @RequestBody ChangeStatusRequest request) {
        operationLogService.execute(
                "user:status",
                userId.toString(),
                () -> accountService.changeStatus(userId, request.getStatus()));
        return Result.success();
    }

    /**
     * 取消目标用户的全部会员权益，包括VIP覆盖前保留的普通会员身份。
     *
     * <p>底层取消方法保持幂等；目标用户已经是非会员时仍返回成功。
     */
    @PostMapping("/{userId}/membership/cancel")
    public Result<Void> cancelMembership(
            @PathVariable Long userId,
            @Valid @RequestBody CancelMembershipRequest request) {
        Long operatorId = currentUserProvider.requireUserId();
        operationLogService.execute(
                "user:membership:cancel",
                userId.toString(),
                () -> {
                    accountService.getAccount(userId);
                    membershipService.cancelMembership(userId, operatorId, request.getRemark());
                });
        return Result.success();
    }
}
