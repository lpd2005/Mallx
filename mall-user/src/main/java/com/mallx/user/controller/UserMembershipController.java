package com.mallx.user.controller;

import com.mallx.common.context.CurrentUserProvider;
import com.mallx.common.result.Result;
import com.mallx.user.dto.ActivateVipMembershipRequest;
import com.mallx.user.service.UserMembershipService;
import com.mallx.user.vo.UserMembershipVO;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 当前登录用户会员接口。 */
@RestController
@RequestMapping("/users/me/membership")
@RequiredArgsConstructor
public class UserMembershipController {

    private final UserMembershipService userMembershipService;
    private final CurrentUserProvider currentUserProvider;

    /** 查询当前用户实际生效的会员等级和折扣。 */
    @GetMapping
    public Result<UserMembershipVO> getMembership() {
        return Result.success(userMembershipService.getCurrentMembership(currentUserProvider.requireUserId()));
    }

    /** 当前登录用户免费开通永久普通会员；重复请求保持普通会员状态不变。 */
    @PostMapping("/normal")
    public Result<UserMembershipVO> activateNormal() {
        Long userId = currentUserProvider.requireUserId();
        return Result.success(
                userMembershipService.activateNormal(userId, userId, "用户自助开通普通会员"));
    }

    /**
     * 当前MVP模拟开通或续费VIP。
     *
     * <p>真实支付接入后，应由支付成功回调触发会员激活，不能继续允许前端直接获得VIP权益。
     */
    @PostMapping("/vip")
    public Result<UserMembershipVO> activateVip(
            @Valid @RequestBody ActivateVipMembershipRequest request) {
        Long userId = currentUserProvider.requireUserId();
        return Result.success(
                userMembershipService.activateVipForMonths(
                        userId,
                        request.getDurationMonths(),
                        userId,
                        "MVP模拟开通或续费VIP"));
    }
}
