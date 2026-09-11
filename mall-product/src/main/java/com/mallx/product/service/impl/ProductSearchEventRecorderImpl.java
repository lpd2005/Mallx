package com.mallx.product.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.mallx.product.entity.ProductSearchOutbox;
import com.mallx.product.mapper.ProductSearchOutboxMapper;
import com.mallx.product.service.ProductSearchEventRecorder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;

/** 只负责写入领域 Outbox，事件投递由独立调度器完成。 */
@Service
@RequiredArgsConstructor
public class ProductSearchEventRecorderImpl implements ProductSearchEventRecorder {
    private static final int PENDING = 0;

    private final ProductSearchOutboxMapper outboxMapper;

    /**
     * 记录商品搜索索引事件。
     * @param spuId 商品SPU ID
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(Long spuId) {
        if (spuId == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        ProductSearchOutbox event = new ProductSearchOutbox();
        event.setId(IdWorker.getId());
        event.setEventId(UUID.randomUUID().toString());
        event.setSpuId(spuId);
        event.setStatus(PENDING);
        event.setRetryCount(0);
        event.setNextRetryAt(now);
        event.setCreatedAt(now);
        outboxMapper.insert(event);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordAll(Collection<Long> spuIds) {
        if (spuIds == null) {
            return;
        }
        spuIds.stream().filter(Objects::nonNull).distinct().forEach(this::record);
    }
}
