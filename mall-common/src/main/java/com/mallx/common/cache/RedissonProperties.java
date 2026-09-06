package com.mallx.common.cache;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Redisson 单节点连接参数。 */
@Data
@Component
@ConfigurationProperties(prefix = "mallx.cache.redisson")
public class RedissonProperties {
    private String host = "localhost";
    private int port = 6379;
    private int database;
    private String password;
    private int connectionPoolSize = 32;
    private int connectionMinimumIdleSize = 4;
}
