package com.mallx.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 与商品事务共同提交的搜索索引事件。 */
@Data
@TableName("product_search_outbox")
public class ProductSearchOutbox {
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    private String eventId;
    private Long spuId;
    private Integer status;
    private Integer retryCount;
    private LocalDateTime nextRetryAt;
    private String lastError;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
}
