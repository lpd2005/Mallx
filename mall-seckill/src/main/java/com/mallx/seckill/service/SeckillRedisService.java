package com.mallx.seckill.service;

import com.mallx.seckill.entity.SeckillActivity;
import com.mallx.seckill.entity.SeckillItem;
import com.mallx.seckill.message.SeckillOrderMessage;
import com.mallx.seckill.vo.SeckillResultVO;

import java.util.List;

/**
 * 秒杀 Redis 数据访问服务。
 *
 * <p>资格库存不进入 Caffeine 或 JetCache，所有竞争性修改均通过 Redis Lua 原子完成。</p>
 */
public interface SeckillRedisService {
    /** 将活动元数据和剩余资格库存写入 Redis。 */
    void warmUp(SeckillActivity activity, List<SeckillItem> items);

    /** 禁止指定秒杀商品继续接受新请求。 */
    void disableItems(List<SeckillItem> items);

    /**
     * 原子抢占资格。
     *
     * @return 0 表示成功，其他数字对应 Lua 中定义的具体拒绝原因
     */
    long reserve(SeckillOrderMessage message);

    /** 从 Redis 请求 Hash 还原异步下单消息。 */
    SeckillOrderMessage getMessage(String requestNo);

    /** 原子标记订单创建成功并移除待恢复标记。 */
    void complete(String requestNo, Long itemId, String orderNo);

    /** 对确定性业务失败执行一次性库存和限购计数补偿。 */
    void compensate(SeckillOrderMessage message, String reason);

    /** 查询 Redis 中的异步请求状态。 */
    SeckillResultVO getResult(String requestNo);

    /** 判断指定活动商品是否仍有尚未完成或补偿的请求。 */
    boolean hasPendingRequests(Long itemId);

    /** 查询超过等待阈值且仍需要投递的消息。 */
    List<SeckillOrderMessage> findRetryableMessages();

    /** 记录一次投递尝试并刷新待处理时间。 */
    void markDispatched(String requestNo);
}
