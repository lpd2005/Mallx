package com.mallx.user.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 开启会员到期状态维护任务。会员权益正确性本身仍由查询时的实时校验保证。 */
@Configuration
@EnableScheduling
public class MembershipSchedulingConfig {
}
