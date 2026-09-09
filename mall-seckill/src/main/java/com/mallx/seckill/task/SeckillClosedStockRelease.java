package com.mallx.seckill.task;

import com.mallx.seckill.service.SeckillActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 已关闭活动剩余库存释放任务。
 *
 * <p>关闭接口只负责立即停止新请求；仍有 MQ 在途请求时，任务会等其全部形成终态后再释放余额。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillClosedStockRelease {
    private final SeckillActivityService activityService;

    /** 周期性处理因存在待处理请求而未能在关闭接口中立即释放的库存。 */
    @Scheduled(fixedDelayString = "${mallx.seckill.closed-stock-release-interval-ms:10000}")
    public void releaseClosedStock() {
        int closed = activityService.closeExpiredActivities();
        int released = activityService.releaseClosedReservations();
        if (closed > 0 || released > 0) {
            log.info("秒杀活动到期清理完成，activityCount={}, itemCount={}", closed, released);
        }
    }
}
