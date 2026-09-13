package com.mallx.order.constant;

/** 订单超时消息使用的交换机、队列和路由名称。 */
public final class OrderMqNames {
    /** 新订单延迟消息进入的交换机。 */
    public static final String DELAY_EXCHANGE = "mallx.order.delay.exchange";
    /** 不设置消费者的延迟队列，消息到期后通过死信路由转发。 */
    public static final String DELAY_QUEUE = "mallx.order.delay.queue";
    /** 新订单延迟消息路由键。 */
    public static final String DELAY_ROUTING_KEY = "mallx.order.delay";

    /** 接收已到期消息的业务交换机。 */
    public static final String TIMEOUT_EXCHANGE = "mallx.order.timeout.exchange";
    /** 真正执行超时关闭的业务队列。 */
    public static final String TIMEOUT_QUEUE = "mallx.order.timeout.queue";
    /** 到期消息路由键。 */
    public static final String TIMEOUT_ROUTING_KEY = "mallx.order.timeout";

    /** 超时关闭消费重试耗尽后的死信交换机。 */
    public static final String TIMEOUT_DLX = "mallx.order.timeout.dlx";
    /** 超时关闭消费重试耗尽后的死信队列。 */
    public static final String TIMEOUT_DLQ = "mallx.order.timeout.dlq";
    /** 超时关闭死信路由键。 */
    public static final String TIMEOUT_DLQ_ROUTING_KEY = "mallx.order.timeout.dead";

    private OrderMqNames() {}
}
