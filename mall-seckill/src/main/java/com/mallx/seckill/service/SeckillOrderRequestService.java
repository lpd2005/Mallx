package com.mallx.seckill.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.mallx.seckill.entity.SeckillOrderRequest;
import com.mallx.seckill.message.SeckillOrderMessage;

/** MQ 消费阶段的秒杀请求持久化与正式订单创建服务。 */
public interface SeckillOrderRequestService extends IService<SeckillOrderRequest> {
    /** 幂等创建正式订单，重复调用返回同一个订单号。 */
    String createOrder(SeckillOrderMessage message);

    /** 持久化确定性业务失败结果，但不会覆盖已经成功的请求。 */
    void markFailed(SeckillOrderMessage message, String reason);

    /** 按全局唯一请求号查询持久化结果。 */
    SeckillOrderRequest findByRequestNo(String requestNo);
}
