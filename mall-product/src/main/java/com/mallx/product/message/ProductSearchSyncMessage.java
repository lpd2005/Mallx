package com.mallx.product.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 商品搜索索引同步消息；消费者始终根据 SPU ID 读取 MySQL 最新状态。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductSearchSyncMessage {
    private String eventId; // 事件 ID
    private Long spuId;
    private LocalDateTime occurredAt; // 事件发生时间
}
