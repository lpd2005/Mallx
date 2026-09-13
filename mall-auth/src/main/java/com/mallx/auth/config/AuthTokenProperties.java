package com.mallx.auth.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** Access Token 与 Refresh Token 的有效期配置。 */
@Data
@Component
@ConfigurationProperties(prefix = "mallx.auth.token")
public class AuthTokenProperties {
    /** Access Token 用于访问业务接口，默认30分钟。 */
    private Duration accessTtl = Duration.ofMinutes(30);

    /** Refresh Token 只用于换取新令牌，默认7天。 */
    private Duration refreshTtl = Duration.ofDays(7);

    @PostConstruct
    public void validate() {
        if (accessTtl == null || accessTtl.isZero() || accessTtl.isNegative()) {
            throw new IllegalStateException("mallx.auth.token.access-ttl必须大于0");
        }
        if (refreshTtl == null || refreshTtl.isZero() || refreshTtl.isNegative()) {
            throw new IllegalStateException("mallx.auth.token.refresh-ttl必须大于0");
        }
        if (refreshTtl.compareTo(accessTtl) <= 0) {
            throw new IllegalStateException("Refresh Token有效期必须长于Access Token");
        }
    }
}
