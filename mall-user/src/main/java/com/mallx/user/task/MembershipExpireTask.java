package com.mallx.user.task;

import com.mallx.user.service.UserMembershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 定期把数据库中的过期VIP状态收敛为已过期。 */
@Component
@RequiredArgsConstructor
public class MembershipExpireTask {

    private static final int BATCH_SIZE = 500;
    private final UserMembershipService userMembershipService;

    /** 默认每五分钟执行一次，部署时可通过配置覆盖Cron表达式。 */
    @Scheduled(cron = "${mallx.user.membership-expire-cron:0 */5 * * * *}")
    public void expireVipMemberships() {
        userMembershipService.expireVipMemberships(BATCH_SIZE);
    }
}
