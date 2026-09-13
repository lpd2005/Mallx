package com.mallx.order.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** 订单支付期限和数据库补偿扫描参数。 */
@Data
@Component
@ConfigurationProperties(prefix = "mallx.order")
public class OrderProperties {
    /** 待支付订单允许支付的时间，默认30分钟。 */
    private Duration paymentTimeout = Duration.ofMinutes(30);

    /** 单次数据库兜底扫描最多关闭的订单数量。 */
    private int compensationBatchSize = 100;
}
