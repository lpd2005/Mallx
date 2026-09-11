package com.mallx.product.publisher;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mallx.product.config.ProductSearchOutboxProperties;
import com.mallx.product.constant.ProductSearchMqNames;
import com.mallx.product.entity.ProductSearchOutbox;
import com.mallx.product.mapper.ProductSearchOutboxMapper;
import com.mallx.product.message.ProductSearchSyncMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/** 轮询事务 Outbox，并使用 Confirm/Return 维护投递状态。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductSearchOutboxPublisher {
    private static final int PENDING = 0;

    private final ProductSearchOutboxMapper outboxMapper;
    private final ProductSearchOutboxProperties properties;
    private final RabbitTemplate rabbitTemplate;

    @Scheduled(fixedDelayString = "${mallx.search.outbox.publish-interval-ms:5000}")
    public void publishPending() {
        LocalDateTime now = LocalDateTime.now();
        List<ProductSearchOutbox> events = outboxMapper.selectList(
                Wrappers.<ProductSearchOutbox>lambdaQuery()
                        .eq(ProductSearchOutbox::getStatus, PENDING)
                        .le(ProductSearchOutbox::getNextRetryAt, now)
                        .orderByAsc(ProductSearchOutbox::getId)
                        .last("LIMIT " + properties.getBatchSize()));
        events.forEach(event -> publish(event, now));
    }

    private void publish(ProductSearchOutbox event, LocalDateTime now) {
        LocalDateTime nextRetryAt = now.plusSeconds(properties.getRetryDelaySeconds());
        if (outboxMapper.claim(event.getId(), now, nextRetryAt) != 1) {
            return;
        }
        ProductSearchSyncMessage message =
                new ProductSearchSyncMessage(event.getEventId(), event.getSpuId(), event.getCreatedAt());
        CorrelationData correlationData = new CorrelationData(event.getEventId());
        try {
            rabbitTemplate.convertAndSend(
                    ProductSearchMqNames.EXCHANGE,
                    ProductSearchMqNames.ROUTING_KEY,
                    message,
                    correlationData);
        } catch (RuntimeException exception) {
            markFailure(event.getId(), exception.getMessage());
            return;
        }
        correlationData.getFuture().whenComplete((confirm, throwable) -> {
            if (throwable != null) {
                markFailure(event.getId(), throwable.getMessage());
                return;
            }
            if (confirm == null || !confirm.isAck()) {
                markFailure(event.getId(), confirm == null ? "Confirm结果为空" : confirm.getReason());
                return;
            }
            ReturnedMessage returned = correlationData.getReturned();
            if (returned != null) {
                markFailure(event.getId(), "消息无法路由：" + returned.getReplyText());
                return;
            }
            markSent(event.getId());
        });
    }

    private void markSent(Long id) {
        outboxMapper.markSent(id, LocalDateTime.now());
    }

    private void markFailure(Long id, String reason) {
        String message = reason == null ? "未知投递错误" : reason;
        String storedReason = message.length() > 500 ? message.substring(0, 500) : message;
        outboxMapper.markFailure(id, storedReason);
        log.error("商品搜索索引事件投递失败，outboxId={}, reason={}", id, message);
    }
}
