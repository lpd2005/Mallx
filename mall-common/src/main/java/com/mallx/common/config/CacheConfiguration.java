package com.mallx.common.config;

import com.alicp.jetcache.anno.config.EnableMethodCache;

import org.springframework.context.annotation.Configuration;

/** JetCache 两级缓存入口；本地层由 Caffeine 实现，远端层复用 Spring Data Redis。 */
@Configuration
@EnableMethodCache(basePackages = "com.mallx")
public class CacheConfiguration {}
