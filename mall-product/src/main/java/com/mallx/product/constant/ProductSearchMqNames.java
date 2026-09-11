package com.mallx.product.constant;

/** 商品搜索索引同步使用的 RabbitMQ 拓扑名称。 */
public final class ProductSearchMqNames {
    public static final String EXCHANGE = "mallx.product.exchange";
    public static final String ROUTING_KEY = "mallx.product.search.sync";
    public static final String QUEUE = "mallx.product.search.sync.queue";
    public static final String DLX = "mallx.product.search.sync.dlx";
    public static final String DLQ_ROUTING_KEY = "mallx.product.search.sync.dead";
    public static final String DLQ = "mallx.product.search.sync.dlq";

    private ProductSearchMqNames() {}
}
