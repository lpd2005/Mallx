package com.mallx.user.service.impl;

import com.mallx.common.exception.BusinessException;
import com.mallx.user.dto.UpdateUserProfileRequest;
import com.mallx.user.entity.UserProfile;
import com.mallx.user.mapper.UserProfileMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 用户资料服务核心规则单元测试。 */
@ExtendWith(MockitoExtension.class)
class UserProfileServiceImplTest {

    @Mock
    private UserProfileMapper userProfileMapper;

    @InjectMocks
    private UserProfileServiceImpl userProfileService;

    /** 模拟 Spring 为 ServiceImpl 注入继承的 baseMapper 字段。 */
    @BeforeEach
    void injectBaseMapper() {
        ReflectionTestUtils.setField(userProfileService, "baseMapper", userProfileMapper);
    }

    /** 验证资料初始化委托给数据库幂等写入。 */
    @Test
    void initializeProfileShouldBeIdempotentAtDatabaseBoundary() {
        userProfileService.initializeProfile(1001L, "MallX用户");
        verify(userProfileMapper).insertIfAbsent(1001L, "MallX用户");
    }

    /** 验证 Service 层不会接受绕过 Controller 校验的非法性别。 */
    @Test
    void updateProfileShouldRejectInvalidGender() {
        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setNickname("测试用户");
        request.setGender(9);
        assertThrows(BusinessException.class, () -> userProfileService.updateProfile(1001L, request));
    }

    /** 验证地址并发控制会使用带 FOR UPDATE 的资料查询。 */
    @Test
    void lockProfileShouldReturnLockedRow() {
        UserProfile profile = new UserProfile();
        profile.setUserId(1001L);
        when(userProfileMapper.selectByIdForUpdate(1001L)).thenReturn(profile);
        userProfileService.lockProfile(1001L);
        verify(userProfileMapper).selectByIdForUpdate(1001L);
    }
}
