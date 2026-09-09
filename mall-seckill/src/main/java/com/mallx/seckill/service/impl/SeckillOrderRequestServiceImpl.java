package com.mallx.seckill.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mallx.common.enums.ResultCode;
import com.mallx.common.exception.BusinessException;
import com.mallx.order.service.OrderService;
import com.mallx.order.vo.OrderVO;
import com.mallx.seckill.entity.SeckillItem;
import com.mallx.seckill.entity.SeckillOrderRequest;
import com.mallx.seckill.enums.SeckillRequestStatus;
import com.mallx.seckill.mapper.SeckillOrderRequestMapper;
import com.mallx.seckill.message.SeckillOrderMessage;
import com.mallx.seckill.service.SeckillItemService;
import com.mallx.seckill.service.SeckillOrderRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 秒杀异步请求落库实现。
 *
 * <p>请求记录、正式订单、库存锁定和活动销量在同一数据库事务中提交；
 * 确定性订单号 {@code SK + requestNo} 使 MQ 重复消费不会生成多张订单。</p>
 */
@Service
@RequiredArgsConstructor
public class SeckillOrderRequestServiceImpl
        extends ServiceImpl<SeckillOrderRequestMapper, SeckillOrderRequest>
        implements SeckillOrderRequestService {
    private final OrderService orderService;
    private final SeckillItemService itemService;

    /** 创建正式订单并把请求状态从处理中推进到成功。 */
    @Override
    @Transactional
    public String createOrder(SeckillOrderMessage message) {
        SeckillOrderRequest record = findByRequestNo(message.getRequestNo());
        if (record != null && SeckillRequestStatus.SUCCESS.matches(record.getStatus())) {
            return record.getOrderNo();
        }
        if (record != null && SeckillRequestStatus.FAILED.matches(record.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "秒杀请求已失败");
        }
        if (record == null) {
            record = buildPending(message);
            if (!save(record)) {
                throw new BusinessException(ResultCode.INTERNAL_ERROR, "秒杀请求保存失败");
            }
        }
        SeckillItem item = itemService.getById(message.getItemId());
        if (item == null || !item.getActivityId().equals(message.getActivityId())
                || !item.getSkuId().equals(message.getSkuId())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "秒杀商品信息不一致");
        }
        OrderVO order = orderService.createSeckillOrder(message.getUserId(), message.getSkuId(),
                message.getQuantity(), message.getSeckillPrice(), message.getAddressId(), message.getRequestNo());
        record.setStatus(SeckillRequestStatus.SUCCESS.getValue());
        record.setOrderNo(order.getOrderNo());
        record.setFailReason(null);
        if (!updateById(record)) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "秒杀结果保存失败");
        }
        boolean counted = itemService.lambdaUpdate().eq(SeckillItem::getId, item.getId())
                .setSql("sold_stock = sold_stock + " + message.getQuantity()).update();
        if (!counted) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "秒杀销量更新失败");
        }
        return order.getOrderNo();
    }

    /** 保存最终业务失败状态；成功记录具有更高优先级，绝不被失败状态覆盖。 */
    @Override
    @Transactional
    public void markFailed(SeckillOrderMessage message, String reason) {
        SeckillOrderRequest record = findByRequestNo(message.getRequestNo());
        if (record != null && SeckillRequestStatus.SUCCESS.matches(record.getStatus())) {
            return;
        }
        if (record == null) {
            record = buildPending(message);
            record.setStatus(SeckillRequestStatus.FAILED.getValue());
            record.setFailReason(normalizeReason(reason));
            save(record);
            return;
        }
        record.setStatus(SeckillRequestStatus.FAILED.getValue());
        record.setFailReason(normalizeReason(reason));
        updateById(record);
    }

    /** 请求号在数据库中有唯一索引，因此最多返回一条记录。 */
    @Override
    public SeckillOrderRequest findByRequestNo(String requestNo) {
        if (requestNo == null || requestNo.isBlank()) {
            return null;
        }
        return lambdaQuery().eq(SeckillOrderRequest::getRequestNo, requestNo.trim()).one();
    }

    /** 由 MQ 消息构建处理中请求快照。 */
    private SeckillOrderRequest buildPending(SeckillOrderMessage message) {
        SeckillOrderRequest record = new SeckillOrderRequest();
        record.setRequestNo(message.getRequestNo());
        record.setActivityId(message.getActivityId());
        record.setItemId(message.getItemId());
        record.setSkuId(message.getSkuId());
        record.setUserId(message.getUserId());
        record.setAddressId(message.getAddressId());
        record.setQuantity(message.getQuantity());
        record.setStatus(SeckillRequestStatus.PENDING.getValue());
        return record;
    }

    /** 限制失败原因长度，防止底层异常信息无限写入数据库。 */
    private String normalizeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "秒杀下单失败";
        }
        String value = reason.trim();
        return value.length() > 200 ? value.substring(0, 200) : value;
    }
}
