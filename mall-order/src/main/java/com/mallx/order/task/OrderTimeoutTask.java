package com.mallx.order.task;

import com.mallx.order.service.OrderService;

import lombok.RequiredArgsConstructor;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 当前阶段使用数据库扫描关闭超时待支付订单。 */
@Component
@RequiredArgsConstructor
public class OrderTimeoutTask {
    private final OrderService service;

    @Scheduled(cron = "${mallx.order.timeout-cron:0 * * * * *}")
    public void close() {
        service.closeExpiredOrders(100);
    }
}
