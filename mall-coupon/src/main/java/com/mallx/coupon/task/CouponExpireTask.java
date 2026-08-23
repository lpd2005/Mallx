package com.mallx.coupon.task;

import com.mallx.coupon.service.CouponTemplateService;
import com.mallx.coupon.service.CouponUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 定期收敛已结束模板和未使用优惠券的数据库状态。 */
@Component
@RequiredArgsConstructor
public class CouponExpireTask {

    private static final int BATCH_SIZE = 500;
    private final CouponTemplateService couponTemplateService;
    private final CouponUserService couponUserService;

    /** 默认每五分钟执行一次；实时业务校验不依赖该任务准时执行。 */
    @Scheduled(cron = "${mallx.coupon.expire-cron:0 */5 * * * *}")
    public void expireCoupons() {
        couponTemplateService.endExpiredTemplates(BATCH_SIZE);
        couponUserService.expireUnusedCoupons(BATCH_SIZE);
    }
}
