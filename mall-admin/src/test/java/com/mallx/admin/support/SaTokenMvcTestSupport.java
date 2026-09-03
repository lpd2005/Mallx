package com.mallx.admin.support;

import static org.mockito.Mockito.*;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;

import com.mallx.auth.config.SaTokenConfig;
import com.mallx.auth.config.SaTokenExceptionHandler;
import com.mallx.common.context.CurrentUserProvider;
import com.mallx.common.exception.GlobalExceptionHandler;

import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.ArrayList;
import java.util.List;

/** 加载真实SaTokenConfig及原生注解解析，仅模拟登录/权限数据来源，不需要MySQL或Redis。 */
public abstract class SaTokenMvcTestSupport {
    protected CurrentUserProvider users;
    protected StpLogic permissionLogic;
    protected MockedStatic<StpUtil> token;
    private MockedStatic<SaManager> manager;
    private final List<AnnotationConfigWebApplicationContext> contexts = new ArrayList<>();

    @BeforeEach
    void initializeSaToken() {
        users = mock(CurrentUserProvider.class);
        when(users.requireUserId()).thenReturn(100L);
        permissionLogic = mock(StpLogic.class);
        token = mockStatic(StpUtil.class);
        manager = mockStatic(SaManager.class);
        // Sa-Token的原生权限注解调用StpLogic.checkPermissionAnd，而不是StpUtil.checkPermission。
        manager.when(() -> SaManager.getStpLogic("", false)).thenReturn(permissionLogic);
    }

    protected MockMvc mvc(Object controller) {
        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        contexts.add(context);
        context.setServletContext(new MockServletContext());
        context.register(
                MvcConfiguration.class,
                SaTokenConfig.class,
                SaTokenExceptionHandler.class,
                GlobalExceptionHandler.class);
        context.addBeanFactoryPostProcessor(
                factory -> {
                    factory.registerSingleton("currentUserProvider", users);
                    factory.registerSingleton("testController", controller);
                });
        context.refresh();
        return MockMvcBuilders.webAppContextSetup(context).build();
    }

    @AfterEach
    void closeSaToken() {
        contexts.forEach(AnnotationConfigWebApplicationContext::close);
        if (manager != null) manager.close();
        if (token != null) token.close();
    }

    @Configuration
    @EnableWebMvc
    public static class MvcConfiguration {}
}
