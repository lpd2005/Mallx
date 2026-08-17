package com.mallx.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 所有持久化业务模块共用的 MyBatis-Plus 基础配置。
 *
 * <p>各模块 Mapper 使用 {@code @Mapper} 标记，由 MyBatis 自动识别；
 * 不在公共模块进行全包扫描，避免将非 Mapper 接口误注册为数据库映射器。</p>
 */
@Configuration
public class MybatisPlusConfiguration {

    /**
     * 注册 MySQL 分页拦截器；后续列表接口使用 MyBatis-Plus {@code Page} 时自动生效。
     *
     * @return MyBatis-Plus 拦截器链
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
