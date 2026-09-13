package com.mallx.order.task;

import com.mallx.order.config.OrderProperties;
import com.mallx.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** MQ漏投或消费异常时使用的低频数据库补偿任务。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutCompensationTask {
    private static final String COMPENSATION_LOCK = "mallx:order:v1:lock:timeout-compensation";

    private final OrderService service;
    private final OrderProperties properties;
    private final RedissonClient redissonClient;

    /** 多实例只允许一个节点扫描；正常关单由RabbitMQ延迟消息触发。 */
    @Scheduled(cron = "${mallx.order.compensation-cron:0 */10 * * * *}")
    public void close() {
        RLock lock = redissonClient.getLock(COMPENSATION_LOCK);
        if (!lock.tryLock()) {
            return;
        }
        try {
            int count = service.closeExpiredOrders(properties.getCompensationBatchSize());
            if (count > 0) {
                log.warn("数据库补偿任务关闭超时订单，count={}", count);
            }
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
