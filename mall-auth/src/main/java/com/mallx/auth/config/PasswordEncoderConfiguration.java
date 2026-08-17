package com.mallx.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 统一密码摘要算法配置。
 */
@Configuration
public class PasswordEncoderConfiguration {

    /**
     * BCrypt 会为相同密码生成不同摘要，适合保存登录密码。
     *
     * @return BCrypt 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
