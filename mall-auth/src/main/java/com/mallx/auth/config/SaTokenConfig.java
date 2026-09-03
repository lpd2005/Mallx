package com.mallx.auth.config;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.strategy.SaAnnotationStrategy;
import com.mallx.common.context.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** 统一使用Sa-Token原生拦截器：先登录与账号状态，再后台准入，最后执行原生注解鉴权。 */
@Configuration
@RequiredArgsConstructor
public class SaTokenConfig implements WebMvcConfigurer {
    private final CurrentUserProvider currentUserProvider;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 原生拦截器默认先执行注解。这里分开注册，确保未登录/禁用账号先被拦截。
        registry.addInterceptor(
                        new SaInterceptor(
                                        handler -> {
                                            StpUtil.checkLogin();
                                            currentUserProvider.requireUserId();
                                        })
                                .isAnnotation(false))
                .addPathPatterns("/**")
                .excludePathPatterns("/auth/login", "/auth/logout")
                .order(0);

        registry.addInterceptor(
                        new SaInterceptor(
                                        handler -> {
                                            StpUtil.checkPermission("admin:access");
                                            requireAdminPermissionDeclaration(handler);
                                        })
                                .isAnnotation(false))
                .addPathPatterns("/admin", "/admin/**")
                .order(10);

        // 注解读取、类与方法权限组合、AND/OR判断全部交给Sa-Token，不再手工解析权限值。
        registry.addInterceptor(new SaInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns("/auth/login", "/auth/logout")
                .order(20);
    }

    /** 仅保留项目的默认拒绝规则：后台必须声明原生权限注解，不能用SaIgnore跳过鉴权。 */
    private void requireAdminPermissionDeclaration(Object handler) {
        if (!(handler instanceof HandlerMethod method)
                || !SaAnnotationStrategy.instance.isAnnotationPresent.apply(
                        method.getMethod(), SaCheckPermission.class)
                || SaAnnotationStrategy.instance.isAnnotationPresent.apply(
                        method.getMethod(), SaIgnore.class)) {
            throw new NotPermissionException("后台接口必须声明SaCheckPermission，且不能忽略鉴权");
        }
    }
}
