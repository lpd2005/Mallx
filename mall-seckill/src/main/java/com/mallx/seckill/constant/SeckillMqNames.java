package com.mallx.seckill.constant;

/** 秒杀消息拓扑名称。 */
public final class SeckillMqNames {
    /** 秒杀异步下单业务交换机。 */
    public static final String ORDER_EXCHANGE = "mallx.seckill.order.exchange";
    /** 秒杀异步下单业务队列。 */
    public static final String ORDER_QUEUE = "mallx.seckill.order.queue";
    /** 创建秒杀订单的路由键。 */
    public static final String ORDER_ROUTING_KEY = "mallx.seckill.order.create";
    /** 消费重试耗尽后的死信交换机。 */
    public static final String ORDER_DLX = "mallx.seckill.order.dlx";
    /** 秒杀订单死信队列。 */
    public static final String ORDER_DLQ = "mallx.seckill.order.dlq";
    /** 秒杀订单死信路由键。 */
    public static final String ORDER_DLQ_ROUTING_KEY = "mallx.seckill.order.dead";

    /** 常量类不允许实例化。 */
    private SeckillMqNames() {}
}
