package com.mallx.user.controller;

import com.mallx.common.context.CurrentUserProvider;
import com.mallx.common.result.Result;
import com.mallx.user.service.UserMembershipService;
import com.mallx.user.vo.UserMembershipVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
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
}
