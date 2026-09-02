package com.mallx.payment.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.mallx.payment.entity.PaymentRefund;

import java.math.BigDecimal;

/** 仅供售后模块调用，不对前端开放任意金额退款接口。 */
public interface PaymentRefundService extends IService<PaymentRefund> {
    /** 参与售后外层事务，完成模拟整单退款并返回退款单号。 */
    String refundForAfterSale(Long userId, String orderNo, String afterSaleNo, BigDecimal amount);
}
