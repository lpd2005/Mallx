package com.mallx.search.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 进入搜索同步死信队列后保留的失败事件。 */
@Data
@AllArgsConstructor
public class ProductSearchFailureVO {
    /** 与 Outbox、RabbitMQ CorrelationData 对应的事件编号。 */
    private String eventId;
    /** 同步失败的商品 SPU ID。 */
    private Long spuId;
    /** 死信被搜索模块记录的时间。 */
    private String failedAt;
}
