package com.mallx.order.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 开启超时订单扫描任务。 */
@Configuration
@EnableScheduling
public class OrderSchedulingConfig {}
