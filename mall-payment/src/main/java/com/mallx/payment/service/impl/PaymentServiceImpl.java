package com.mallx.payment.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mallx.common.enums.ResultCode;
import com.mallx.common.exception.BusinessException;
import com.mallx.order.enums.OrderStatus;
import com.mallx.order.service.OrderService;
import com.mallx.order.vo.OrderPaymentInfoVO;
import com.mallx.payment.entity.PaymentRecord;
import com.mallx.payment.enums.*;
import com.mallx.payment.mapper.PaymentRecordMapper;
import com.mallx.payment.service.PaymentService;
import com.mallx.payment.vo.PaymentVO;

import lombok.RequiredArgsConstructor;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** 模拟支付实现；支付单和订单确认在同一个数据库事务中提交。 */
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl extends ServiceImpl<PaymentRecordMapper, PaymentRecord>
        implements PaymentService {
    private final PaymentRecordMapper paymentMapper;
    private final OrderService orderService;

    /**
     * 创建支付单
     */
    @Override
    @Transactional
    public PaymentVO createPayment(Long userId, String orderNo) {
        requireUser(userId);
        OrderPaymentInfoVO order = orderService.getPaymentInfo(userId, orderNo);
        PaymentRecord existing = lambdaQuery().eq(PaymentRecord::getOrderNo, orderNo).one();
        if (existing != null) return toVO(existing);
        if (!OrderStatus.WAIT_PAY.matches(order.getStatus())) throw bad("订单当前不能创建支付单");
        if (!order.getExpireAt().isAfter(LocalDateTime.now())) throw bad("订单已超过支付时间");
        PaymentRecord p = new PaymentRecord();
        p.setPaymentNo("PAY" + IdWorker.getIdStr());
        p.setOrderNo(orderNo);
        p.setUserId(userId);
        p.setAmount(order.getPayAmount());
        p.setChannel(PaymentChannel.MOCK.getValue());
        p.setStatus(PaymentStatus.PENDING.getValue());
        try {
            if (!save(p)) throw new BusinessException(ResultCode.INTERNAL_ERROR, "支付单创建失败");
        } catch (DuplicateKeyException e) {
            PaymentRecord concurrent = lambdaQuery().eq(PaymentRecord::getOrderNo, orderNo).one();
            if (concurrent != null) return toVO(concurrent);
            throw e;
        }
        return toVO(p);
    }

    /**
     * 获取支付单
     */
    @Override
    @Transactional
    public PaymentVO getPayment(Long userId, String no) {
        PaymentRecord p = requireOwned(userId, no);
        if (PaymentStatus.PENDING.matches(p.getStatus())) {
            OrderPaymentInfoVO o = orderService.getPaymentInfo(userId, p.getOrderNo());
            if (OrderStatus.CANCELED.matches(o.getStatus()) || !o.getExpireAt().isAfter(LocalDateTime.now())) close(p);
        }
        return toVO(p);
    }

    /** 重复模拟支付通知读取已支付结果，不会再次扣减库存或更新订单。 */
    @Override
    @Transactional
    public PaymentVO mockPay(Long userId, String no, String requestNo) {
        requireUser(userId);
        String req = requireRequest(requestNo);
        PaymentRecord p = paymentMapper.selectByPaymentNoForUpdate(requireNo(no));
        if (p == null || !userId.equals(p.getUserId()))
            throw new BusinessException(ResultCode.NOT_FOUND, "支付单不存在");
        if (PaymentStatus.PAID.matches(p.getStatus())) return toVO(p);
        if (!PaymentStatus.PENDING.matches(p.getStatus())) throw bad("支付单当前不能支付");
        LocalDateTime now = LocalDateTime.now();
        OrderPaymentInfoVO order = orderService.getPaymentInfo(userId, p.getOrderNo());
        if (OrderStatus.CANCELED.matches(order.getStatus()) || !order.getExpireAt().isAfter(now)) {
            close(p);
            return toVO(p);
        }
        orderService.markPaid(p.getOrderNo(), p.getPaymentNo(), now);
        p.setStatus(PaymentStatus.PAID.getValue());
        p.setPaidRequestNo(req);
        p.setPaidAt(now);
        try {
            if (!updateById(p)) throw new BusinessException(ResultCode.INTERNAL_ERROR, "支付状态更新失败");
        } catch (DuplicateKeyException e) {
            throw bad("支付请求号已经被使用");
        }
        return toVO(p);
    }

    private PaymentRecord requireOwned(Long uid, String no) {
        requireUser(uid);
        PaymentRecord p =
                lambdaQuery()
                        .eq(PaymentRecord::getPaymentNo, requireNo(no))
                        .eq(PaymentRecord::getUserId, uid)
                        .one();
        if (p == null) throw new BusinessException(ResultCode.NOT_FOUND, "支付单不存在");
        return p;
    }

    private void close(PaymentRecord p) {
        p.setStatus(PaymentStatus.CLOSED.getValue());
        p.setClosedAt(LocalDateTime.now());
        if (!updateById(p)) throw new BusinessException(ResultCode.INTERNAL_ERROR, "支付单关闭失败");
    }

    private PaymentVO toVO(PaymentRecord p) {
        return PaymentVO.builder()
                .paymentNo(p.getPaymentNo())
                .orderNo(p.getOrderNo())
                .amount(p.getAmount())
                .channel(p.getChannel())
                .status(p.getStatus())
                .paidAt(p.getPaidAt())
                .closedAt(p.getClosedAt())
                .refundedAt(p.getRefundedAt())
                .createdAt(p.getCreatedAt())
                .build();
    }

    private void requireUser(Long id) {
        if (id == null || id <= 0) throw new BusinessException(ResultCode.UNAUTHORIZED, "当前用户身份无效");
    }

    private String requireNo(String s) {
        if (s == null || s.isBlank() || s.trim().length() > 64) throw bad("支付单号不正确");
        return s.trim();
    }

    private String requireRequest(String s) {
        if (s == null || s.isBlank() || s.trim().length() > 64) throw bad("支付请求号不能为空且不能超过64位");
        return s.trim();
    }

    private BusinessException bad(String s) {
        return new BusinessException(ResultCode.BAD_REQUEST, s);
    }
}
