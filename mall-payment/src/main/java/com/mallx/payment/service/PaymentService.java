package com.mallx.payment.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.mallx.payment.entity.PaymentRecord;
import com.mallx.payment.vo.PaymentVO;

/** 支付单和模拟支付服务。 */
public interface PaymentService extends IService<PaymentRecord> {
    PaymentVO createPayment(Long userId, String orderNo);

    PaymentVO getPayment(Long userId, String paymentNo);

    PaymentVO mockPay(Long userId, String paymentNo, String requestNo);
}
