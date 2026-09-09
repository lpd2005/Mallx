package com.mallx.seckill.config;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 秒杀请求结果保留和异常请求恢复参数。 */
@Data
@Component
@ConfigurationProperties(prefix = "mallx.seckill")
public class SeckillProperties {
    /** Redis 请求结果和用户累计购买记录的保留秒数，默认七天。 */
    private long requestTtlSeconds = 604_800L;

    /** 请求处于处理中超过该秒数后，允许恢复任务再次投递。 */
    private long pendingRetryDelaySeconds = 10L;

    /** 单次恢复任务最多扫描的请求数量，避免一次任务占用过多资源。 */
    private int pendingBatchSize = 100;
}
