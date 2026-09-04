package com.mallx.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记允许游客访问的Controller方法，只跳过登录与账号状态校验。
 *
 * <p>不跳过Sa-Token权限注解，不允许用于后台接口；仅支持方法级声明，避免整类误放行。
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Public {}
