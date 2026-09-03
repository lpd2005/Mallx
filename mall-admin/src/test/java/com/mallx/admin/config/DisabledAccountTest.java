package com.mallx.admin.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.dev33.satoken.stp.StpUtil;

import com.mallx.auth.context.SaTokenCurrentUserProvider;
import com.mallx.auth.entity.AuthAccount;
import com.mallx.auth.mapper.AuthAccountMapper;
import com.mallx.common.exception.BusinessException;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/** 旧Token仍然存在时，数据库禁用状态必须生效。 */
class DisabledAccountTest {
    @Test
    void disabledAccountCanStillLogOutItsOldSession() {
        com.mallx.common.context.CurrentUserProvider users = mock(com.mallx.common.context.CurrentUserProvider.class);
        when(users.isLogin()).thenReturn(true);
        com.mallx.auth.service.impl.AuthServiceImpl service = new com.mallx.auth.service.impl.AuthServiceImpl(
            mock(AuthAccountMapper.class), mock(org.springframework.security.crypto.password.PasswordEncoder.class), users);
        try (MockedStatic<StpUtil> token = mockStatic(StpUtil.class)) {
            token.when(StpUtil::getLoginId).thenReturn(1L);
            service.logout();
            verify(users, never()).requireUserId();
            token.verify(StpUtil::logout);
        }
    }
    @org.junit.jupiter.api.BeforeAll
    static void initializeLambdaMetadata() {
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
                new org.apache.ibatis.builder.MapperBuilderAssistant(
                        new com.baomidou.mybatisplus.core.MybatisConfiguration(),
                        "disabled-account-test"),
                AuthAccount.class);
    }

    @Test
    void disabledAccountCannotUseOldToken() {
        AuthAccountMapper mapper = mock(AuthAccountMapper.class);
        AuthAccount account = new AuthAccount();
        account.setId(1L);
        account.setStatus(0);
        when(mapper.selectOne(any())).thenReturn(account);
        try (MockedStatic<StpUtil> token = mockStatic(StpUtil.class)) {
            token.when(StpUtil::isLogin).thenReturn(true);
            token.when(StpUtil::getLoginId).thenReturn(1L);
            assertThrows(
                    BusinessException.class,
                    () -> new SaTokenCurrentUserProvider(mapper).requireUserId());
        }
    }

    @Test
    void enabledAccountReturnsActualLoginId() {
        AuthAccountMapper mapper = mock(AuthAccountMapper.class);
        AuthAccount account = new AuthAccount();
        account.setId(1L);
        account.setStatus(1);
        when(mapper.selectOne(any())).thenReturn(account);
        try (MockedStatic<StpUtil> token = mockStatic(StpUtil.class)) {
            token.when(StpUtil::isLogin).thenReturn(true);
            token.when(StpUtil::getLoginId).thenReturn(1L);
            assertEquals(1L, new SaTokenCurrentUserProvider(mapper).requireUserId());
        }
    }
}
