package com.mallx.search.listener;

import com.mallx.product.constant.ProductSearchMqNames;
import com.mallx.product.message.ProductSearchSyncMessage;
import com.mallx.search.service.impl.ProductSearchFailureService;
import com.mallx.search.service.impl.ProductSearchIndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** 消费商品变更事件；业务队列失败重试耗尽后由死信监听器留档。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductSearchSyncListener {
    private final ProductSearchIndexService indexService;
    private final ProductSearchFailureService failureService;

    /**
     * 正常消费入口。消息只提供 SPU ID，索引服务会重新读取 MySQL 最新状态，
     * 因而重复投递和乱序投递最终都能收敛到同一份文档。
     */
    @RabbitListener(queues = ProductSearchMqNames.QUEUE)
    public void synchronize(ProductSearchSyncMessage message) {
        indexService.synchronize(message.getSpuId());
    }

    /** 消费重试耗尽后留档死信，使管理员可以查询并手动重放。 */
    @RabbitListener(queues = ProductSearchMqNames.DLQ)
    public void recordDeadLetter(ProductSearchSyncMessage message) {
        failureService.record(message);
        log.error("商品搜索索引同步进入死信队列，eventId={}, spuId={}",
                message.getEventId(), message.getSpuId());
    }
}
