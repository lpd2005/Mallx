package com.mallx.common.mq;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 业务消息的通用信封，具体交换机、队列及消费者仍归属各业务模块。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageEnvelope<T> {

    /** 消息唯一标识，用于追踪和消费幂等。 */
    private String messageId;

    /** 关联的业务主键，例如订单号或商品 ID。 */
    private String businessId;

    /** 业务事件发生时间。 */
    private Instant occurredAt;

    /** 业务模块定义的消息载荷。 */
    private T payload;
}
