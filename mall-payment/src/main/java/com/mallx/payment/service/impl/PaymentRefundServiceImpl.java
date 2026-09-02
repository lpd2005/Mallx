package com.mallx.payment.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mallx.common.enums.ResultCode;
import com.mallx.common.exception.BusinessException;
import com.mallx.payment.entity.PaymentRecord;
import com.mallx.payment.entity.PaymentRefund;
import com.mallx.payment.enums.PaymentChannel;
import com.mallx.payment.enums.PaymentStatus;
import com.mallx.payment.mapper.PaymentRecordMapper;
import com.mallx.payment.mapper.PaymentRefundMapper;
import com.mallx.payment.service.PaymentRefundService;
import com.mallx.payment.service.PaymentService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 模拟退款只修改本地数据库；真实支付渠道不能照搬这种本地事务实现。 */
@Service
@RequiredArgsConstructor
public class PaymentRefundServiceImpl extends ServiceImpl<PaymentRefundMapper, PaymentRefund>
        implements PaymentRefundService {
    private final PaymentRecordMapper paymentRecordMapper;
    private final PaymentService paymentService;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public String refundForAfterSale(Long userId, String orderNo, String afterSaleNo, BigDecimal amount) {
        if (userId == null
                || userId <= 0
                || orderNo == null
                || orderNo.isBlank()
                || orderNo.length() > 64
                || afterSaleNo == null
                || afterSaleNo.isBlank()
                || afterSaleNo.length() > 64
                || amount == null
                || amount.signum() < 0) {
            throw bad("退款参数不正确");
        }
        // 同一支付单的付款、退款共用这一行锁，防止重复退款。
        PaymentRecord payment = paymentRecordMapper.selectByOrderNoForUpdate(orderNo);
        if (payment == null || !userId.equals(payment.getUserId())) {
            throw bad("原支付单不存在或不属于当前用户");
        }
        if (!Integer.valueOf(PaymentChannel.MOCK.getValue()).equals(payment.getChannel())) {
            throw bad("当前仅支持模拟支付退款");
        }
        if (payment.getAmount().compareTo(amount) != 0) {
            throw bad("仅支持按原支付金额整单退款");
        }
        PaymentRefund existing = lambdaQuery().eq(PaymentRefund::getOrderNo, orderNo).one();
        if (existing != null) {
            if (!afterSaleNo.equals(existing.getAfterSaleNo())
                    || existing.getAmount().compareTo(amount) != 0
                    || !PaymentStatus.REFUNDED.matches(payment.getStatus())) {
                throw bad("订单已有其他退款记录或退款状态不一致");
            }
            return existing.getRefundNo();
        }
        if (!PaymentStatus.PAID.matches(payment.getStatus())) {
            throw bad("原支付单未支付或已经退款");
        }
        LocalDateTime now = LocalDateTime.now();
        PaymentRefund refund = new PaymentRefund();
        refund.setRefundNo("RF" + IdWorker.getIdStr());
        refund.setAfterSaleNo(afterSaleNo);
        refund.setPaymentNo(payment.getPaymentNo());
        refund.setOrderNo(orderNo);
        refund.setUserId(userId);
        refund.setAmount(payment.getAmount());
        refund.setRefundedAt(now);
        refund.setCreatedAt(now);
        if (!save(refund)) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "退款流水保存失败");
        }
        payment.setStatus(PaymentStatus.REFUNDED.getValue());
        payment.setRefundedAt(now);
        if (!paymentService.updateById(payment)) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "支付单退款状态更新失败");
        }
        return refund.getRefundNo();
    }

    private BusinessException bad(String message) {
        return new BusinessException(ResultCode.BAD_REQUEST, message);
    }
}
