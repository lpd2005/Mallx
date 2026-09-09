package com.mallx.seckill.service;

import com.mallx.seckill.dto.SeckillPurchaseRequest;
import com.mallx.seckill.vo.SeckillResultVO;
import com.mallx.seckill.vo.SeckillSubmitVO;

/** 用户秒杀入口服务，负责资格预扣、消息投递和异步结果查询。 */
public interface SeckillPurchaseService {
    /** 提交抢购请求并返回用于轮询的请求号。 */
    SeckillSubmitVO submit(Long userId, Long itemId, SeckillPurchaseRequest request);

    /** 查询属于当前用户的秒杀异步处理结果。 */
    SeckillResultVO getResult(Long userId, String requestNo);
}
