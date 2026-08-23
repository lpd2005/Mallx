package com.mallx.coupon.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 开启优惠券模板和用户券的到期状态维护任务。 */
@Configuration
@EnableScheduling
public class CouponSchedulingConfig {
}
