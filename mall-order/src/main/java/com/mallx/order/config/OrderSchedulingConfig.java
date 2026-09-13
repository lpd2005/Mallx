package com.mallx.order.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 开启订单超时数据库补偿任务。 */
@Configuration
@EnableScheduling
public class OrderSchedulingConfig {}
