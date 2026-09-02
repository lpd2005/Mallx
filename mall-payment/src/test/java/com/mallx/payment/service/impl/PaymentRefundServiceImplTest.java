package com.mallx.payment.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.mallx.common.exception.BusinessException;
import com.mallx.payment.entity.PaymentRecord;
import com.mallx.payment.entity.PaymentRefund;
import com.mallx.payment.mapper.PaymentRecordMapper;
import com.mallx.payment.mapper.PaymentRefundMapper;
import com.mallx.payment.service.PaymentService;

import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

@ExtendWith(MockitoExtension.class)
class PaymentRefundServiceImplTest {
    @Mock PaymentRefundMapper mapper;
    @Mock PaymentRecordMapper paymentMapper;
    @Mock PaymentService paymentService;
    PaymentRefundServiceImpl service;

    @BeforeAll
    static void metadata() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "refund-test"),
                PaymentRefund.class);
    }

    @BeforeEach
    void setup() {
        service =
                new PaymentRefundServiceImpl(paymentMapper, paymentService) {
                    @Override
                    public Class<PaymentRefund> getEntityClass() {
                        return PaymentRefund.class;
                    }

                    @Override
                    public Class<PaymentRefundMapper> getMapperClass() {
                        return PaymentRefundMapper.class;
                    }
                };
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
    }

    @Test
    void fullRefundCreatesLedgerAndChangesPayment() {
        PaymentRecord payment = paid();
        when(paymentMapper.selectByOrderNoForUpdate("MX1")).thenReturn(payment);
        when(mapper.selectOne(any())).thenReturn(null);
        when(mapper.insert(any(PaymentRefund.class))).thenReturn(1);
        when(paymentService.updateById(any(PaymentRecord.class))).thenReturn(true);
        assertNotNull(service.refundForAfterSale(1L, "MX1", "AS1", new BigDecimal("85.0")));
        assertEquals(4, payment.getStatus());
        assertNotNull(payment.getRefundedAt());
    }

    @Test
    void mismatchedAmountRejected() {
        when(paymentMapper.selectByOrderNoForUpdate("MX1")).thenReturn(paid());
        assertThrows(
                BusinessException.class,
                () -> service.refundForAfterSale(1L, "MX1", "AS1", new BigDecimal("100")));
        verifyNoInteractions(mapper, paymentService);
    }

    @Test
    void anotherUserCannotRefundPayment() {
        when(paymentMapper.selectByOrderNoForUpdate("MX1")).thenReturn(paid());
        assertThrows(
                BusinessException.class,
                () -> service.refundForAfterSale(2L, "MX1", "AS1", new BigDecimal("85")));
    }

    @Test
    void unpaidPaymentCannotBeRefunded() {
        PaymentRecord payment = paid();
        payment.setStatus(1);
        when(paymentMapper.selectByOrderNoForUpdate("MX1")).thenReturn(payment);
        when(mapper.selectOne(any())).thenReturn(null);
        assertThrows(
                BusinessException.class,
                () -> service.refundForAfterSale(1L, "MX1", "AS1", new BigDecimal("85")));
        verifyNoInteractions(paymentService);
    }

    @Test
    void sameAfterSaleRefundIsIdempotentButDifferentOneIsRejected() {
        PaymentRecord payment = paid();
        payment.setStatus(4);
        PaymentRefund refund = new PaymentRefund();
        refund.setAfterSaleNo("AS1");
        refund.setRefundNo("RF1");
        refund.setAmount(new BigDecimal("85.00"));
        when(paymentMapper.selectByOrderNoForUpdate("MX1")).thenReturn(payment);
        when(mapper.selectOne(any())).thenReturn(refund);
        assertEquals("RF1", service.refundForAfterSale(1L, "MX1", "AS1", new BigDecimal("85")));
        assertThrows(
                BusinessException.class,
                () -> service.refundForAfterSale(1L, "MX1", "AS2", new BigDecimal("85")));
        verifyNoInteractions(paymentService);
    }

    private PaymentRecord paid() {
        PaymentRecord payment = new PaymentRecord();
        payment.setPaymentNo("PAY1");
        payment.setOrderNo("MX1");
        payment.setUserId(1L);
        payment.setAmount(new BigDecimal("85.00"));
        payment.setChannel(1);
        payment.setStatus(2);
        return payment;
    }
}
