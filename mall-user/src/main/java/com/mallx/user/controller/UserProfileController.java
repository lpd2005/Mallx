package com.mallx.user.controller;

import com.mallx.common.result.Result;
import com.mallx.common.context.CurrentUserProvider;
import com.mallx.user.dto.UpdateUserProfileRequest;
import com.mallx.user.service.UserProfileService;
import com.mallx.user.vo.UserProfileVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 当前用户资料接口。 */
@RestController
@RequestMapping("/users/me")
@RequiredArgsConstructor
public class UserProfileController {

    /** 用户资料业务服务。 */
    private final UserProfileService userProfileService;
    /** 当前登录用户身份读取器。 */
    private final CurrentUserProvider currentUserProvider;

    /**
     * 查询当前登录用户的业务资料；历史账号没有资料时会幂等初始化。
     *
     * @return 当前用户资料
     */
    @GetMapping
    public Result<UserProfileVO> getProfile() {
        return Result.success(userProfileService.getProfile(currentUserProvider.requireUserId()));
    }

    /**
     * 完整更新当前登录用户的业务资料。
     *
     * @param request 已通过 Bean Validation 校验的资料
     * @return 更新后的用户资料
     */
    @PutMapping
    public Result<UserProfileVO> updateProfile(@Valid @RequestBody UpdateUserProfileRequest request) {
        return Result.success(userProfileService.updateProfile(currentUserProvider.requireUserId(), request));
    }
}
