package com.mallx.user.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.mallx.user.dto.UpdateUserProfileRequest;
import com.mallx.user.entity.UserProfile;
import com.mallx.user.vo.UserProfileVO;

/** 用户资料服务。 */
public interface UserProfileService extends IService<UserProfile> {

    /**
     * 查询用户资料，缺失时为历史账号补建默认资料。
     *
     * @param userId 用户ID
     * @return 用户资料
     */
    UserProfileVO getProfile(Long userId);

    /**
     * 完整更新用户资料。
     *
     * @param userId 用户ID
     * @param request 新资料
     * @return 更新后的资料
     */
    UserProfileVO updateProfile(Long userId, UpdateUserProfileRequest request);

    /**
     * 供注册流程调用的幂等初始化能力。
     *
     * @param userId 与认证账号相同的用户ID
     * @param nickname 初始昵称，为空时生成默认昵称
     */
    void initializeProfile(Long userId, String nickname);

    /**
     * 锁定用户资料行，供同一用户的默认地址变更串行化。
     * 调用方必须处于数据库事务内，锁才会保持到事务结束。
     *
     * @param userId 用户ID
     * @return 已加排他锁的用户资料
     */
    UserProfile lockProfile(Long userId);
}
