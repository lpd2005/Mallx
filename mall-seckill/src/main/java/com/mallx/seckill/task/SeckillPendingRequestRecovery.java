package com.mallx.seckill.task;

import com.mallx.seckill.constant.SeckillRedisKeys;
import com.mallx.seckill.message.SeckillOrderMessage;
import com.mallx.seckill.publisher.SeckillMessagePublisher;
import com.mallx.seckill.service.SeckillRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 扫描 Redis 待处理集合并恢复可能丢失的 MQ 投递。
 *
 * <p>恢复锁保证多个应用实例中同一时刻只有一个实例扫描；请求号和订单号保证重复投递安全。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillPendingRequestRecovery {
    private final SeckillRedisService redisService;
    private final SeckillMessagePublisher messagePublisher;
    private final RedissonClient redissonClient;

    /** 定期重投超过等待阈值仍处于处理中的请求。 */
    @Scheduled(fixedDelayString = "${mallx.seckill.pending-scan-interval-ms:5000}")
    public void retryPendingRequests() {
        RLock lock = redissonClient.getLock(SeckillRedisKeys.RECOVERY_LOCK);
        boolean locked = false;
        try {
            locked = lock.tryLock(0, 20, TimeUnit.SECONDS);
            if (!locked) {
                return;
            }
            List<SeckillOrderMessage> messages = redisService.findRetryableMessages();
            for (SeckillOrderMessage message : messages) {
                try {
                    redisService.markDispatched(message.getRequestNo());
                    messagePublisher.send(message);
                } catch (RuntimeException exception) {
                    log.error("秒杀待处理消息重投失败，requestNo={}", message.getRequestNo(), exception);
                }
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("秒杀待处理消息恢复任务被中断", exception);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
