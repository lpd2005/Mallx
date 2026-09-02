package com.mallx.payment.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.mallx.order.enums.OrderStatus;
import com.mallx.order.service.OrderService;
import com.mallx.order.vo.OrderPaymentInfoVO;
import com.mallx.payment.entity.PaymentRecord;
import com.mallx.payment.enums.*;
import com.mallx.payment.mapper.PaymentRecordMapper;
import com.mallx.payment.vo.PaymentVO;

import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 支付单创建、模拟支付和重复通知幂等测试。 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {
    @BeforeAll
    static void metadata() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "payment-test"),
                PaymentRecord.class);
    }

    @Mock PaymentRecordMapper mapper;
    @Mock OrderService orderService;
    PaymentServiceImpl service;

    @BeforeEach
    void setup() {
        service =
                new PaymentServiceImpl(mapper, orderService) {
                    @Override
                    public Class<PaymentRecord> getEntityClass() {
                        return PaymentRecord.class;
                    }

                    @Override
                    public Class<PaymentRecordMapper> getMapperClass() {
                        return PaymentRecordMapper.class;
                    }
                };
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
    }

    @Test
    void createShouldSnapshotOrderAmount() {
        when(orderService.getPaymentInfo(1L, "MX1")).thenReturn(order(OrderStatus.WAIT_PAY));
        when(mapper.selectOne(any())).thenReturn(null);
        when(mapper.insert(any(PaymentRecord.class))).thenReturn(1);
        PaymentVO vo = service.createPayment(1L, "MX1");
        assertEquals(new BigDecimal("88.00"), vo.getAmount());
        assertEquals(PaymentStatus.PENDING.getValue(), vo.getStatus());
    }

    @Test
    void mockPayShouldMarkOrderAndPaymentPaid() {
        PaymentRecord p = pending();
        when(mapper.selectByPaymentNoForUpdate("PAY1")).thenReturn(p);
        when(orderService.getPaymentInfo(1L, "MX1")).thenReturn(order(OrderStatus.WAIT_PAY));
        when(mapper.updateById(any(PaymentRecord.class))).thenReturn(1);
        PaymentVO vo = service.mockPay(1L, "PAY1", "REQ1");
        assertEquals(PaymentStatus.PAID.getValue(), vo.getStatus());
        verify(orderService).markPaid(eq("MX1"), eq("PAY1"), any());
    }

    @Test
    void repeatedPaidNotificationShouldBeIdempotent() {
        PaymentRecord p = pending();
        p.setStatus(PaymentStatus.PAID.getValue());
        when(mapper.selectByPaymentNoForUpdate("PAY1")).thenReturn(p);
        service.mockPay(1L, "PAY1", "REQ2");
        verify(orderService, never()).markPaid(any(), any(), any());
    }

    @Test
    void queryingRefundedPaymentMustNotCloseItAsExpired() {
        PaymentRecord payment = pending();
        payment.setStatus(PaymentStatus.REFUNDED.getValue());
        when(mapper.selectByPaymentNoForUpdate("PAY1")).thenReturn(payment);
        PaymentVO result = service.getPayment(1L, "PAY1");
        assertEquals(PaymentStatus.REFUNDED.getValue(), result.getStatus());
        verifyNoInteractions(orderService);
        verify(mapper, never()).updateById(any(PaymentRecord.class));
    }

    @Test
    void lockedPaymentQueryStillEnforcesOwnership() {
        when(mapper.selectByPaymentNoForUpdate("PAY1")).thenReturn(pending());
        assertThrows(com.mallx.common.exception.BusinessException.class,
                () -> service.getPayment(2L, "PAY1"));
        verifyNoInteractions(orderService);
    }

    private PaymentRecord pending() {
        PaymentRecord p = new PaymentRecord();
        p.setPaymentNo("PAY1");
        p.setOrderNo("MX1");
        p.setUserId(1L);
        p.setAmount(new BigDecimal("88.00"));
        p.setChannel(1);
        p.setStatus(1);
        return p;
    }

    private OrderPaymentInfoVO order(OrderStatus s) {
        return OrderPaymentInfoVO.builder()
                .orderNo("MX1")
                .userId(1L)
                .status(s.getValue())
                .payAmount(new BigDecimal("88.00"))
                .expireAt(LocalDateTime.now().plusMinutes(10))
                .build();
    }
}
