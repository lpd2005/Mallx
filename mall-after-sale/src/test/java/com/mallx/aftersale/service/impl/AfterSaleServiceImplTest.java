package com.mallx.aftersale.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.mallx.aftersale.dto.AuditAfterSaleRequest;
import com.mallx.aftersale.dto.CreateAfterSaleRequest;
import com.mallx.aftersale.dto.ReceiveReturnRequest;
import com.mallx.aftersale.dto.ReturnShipmentRequest;
import com.mallx.aftersale.entity.AfterSale;
import com.mallx.aftersale.mapper.AfterSaleMapper;
import com.mallx.aftersale.service.AfterSaleLogService;
import com.mallx.aftersale.service.AfterSaleService;
import com.mallx.aftersale.vo.AfterSaleVO;
import com.mallx.common.exception.BusinessException;
import com.mallx.order.service.OrderService;
import com.mallx.order.vo.OrderAfterSaleVO;
import com.mallx.payment.service.PaymentRefundService;

import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDateTime;

import javax.sql.DataSource;

/** 流程、权限归属、重复请求和Spring事务回滚边界测试，不连接生产数据库。 */
@ExtendWith(MockitoExtension.class)
class AfterSaleServiceImplTest {
    @Mock AfterSaleMapper mapper;
    @Mock AfterSaleLogService logService;
    @Mock OrderService orderService;
    @Mock PaymentRefundService refundService;
    AfterSaleServiceImpl service;

    @BeforeAll
    static void metadata() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "after-sale-test"),
                AfterSale.class);
    }

    @BeforeEach
    void setup() {
        service =
                new AfterSaleServiceImpl(mapper, logService, orderService, refundService) {
                    @Override
                    public Class<AfterSale> getEntityClass() {
                        return AfterSale.class;
                    }

                    @Override
                    public Class<AfterSaleMapper> getMapperClass() {
                        return AfterSaleMapper.class;
                    }
                };
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
    }

    @Test
    void applySnapshotsRealOrderAmount() {
        CreateAfterSaleRequest request = application(1);
        when(orderService.beginAfterSale(eq(1L), eq("MX1"), anyString(), eq(false)))
                .thenReturn(
                        OrderAfterSaleVO.builder()
                                .orderId(10L)
                                .orderNo("MX1")
                                .originalStatus(2)
                                .payAmount(new BigDecimal("85.00"))
                                .build());
        when(mapper.insert(any(AfterSale.class))).thenReturn(1);
        when(logService.save(any())).thenReturn(true);
        AfterSaleVO result = service.apply(1L, request);
        assertEquals(new BigDecimal("85.00"), result.getRefundAmount());
        assertEquals(1, result.getStatus());
        assertEquals(1, result.getRestock());
        verify(mapper).insert(argThat((AfterSale sale) -> "MX1".equals(sale.getActiveOrderNo())));
    }

    @Test
    void unsupportedTypeRejectedBeforeOrderMutation() {
        assertThrows(BusinessException.class, () -> service.apply(1L, application(3)));
        verifyNoInteractions(orderService, refundService, mapper);
    }

    @Test
    void rejectionReleasesOrderAndOccupancy() {
        AfterSale sale = locked(1, 1);
        persistence();
        service.audit(99L, "AS1", audit(false));
        assertEquals(6, sale.getStatus());
        assertNull(sale.getActiveOrderNo());
        verify(orderService).restoreAfterSale(1L, "MX1", "AS1", 99L);
    }

    @Test
    void approvalOfReturnRequiresShippingBeforeRefund() {
        AfterSale sale = locked(2, 1);
        persistence();
        service.audit(99L, "AS1", audit(true));
        assertEquals(3, sale.getStatus());
        verifyNoInteractions(refundService);
    }

    @Test
    void canceledApplicationIsIdempotentAndDoesNotRefund() {
        AfterSale sale = locked(1, 1);
        persistence();
        service.cancel(1L, "AS1");
        service.cancel(1L, "AS1");
        assertEquals(7, sale.getStatus());
        assertNull(sale.getActiveOrderNo());
        verify(orderService, times(1)).restoreAfterSale(1L, "MX1", "AS1", 1L);
        verifyNoInteractions(refundService);
    }

    @Test
    void cannotCancelAfterReturnShipment() {
        locked(2, 4);
        assertThrows(BusinessException.class, () -> service.cancel(1L, "AS1"));
        verifyNoInteractions(orderService, refundService);
    }

    @Test
    void otherUserCannotModifyApplication() {
        locked(1, 1);
        assertThrows(BusinessException.class, () -> service.cancel(2L, "AS1"));
        verify(mapper, never()).updateById(any(AfterSale.class));
        verifyNoInteractions(orderService);
    }

    @Test
    void returnShipmentCannotBeOverwrittenByRetry() {
        AfterSale sale = locked(2, 3);
        persistence();
        ReturnShipmentRequest request = new ReturnShipmentRequest();
        request.setCarrier("测试物流");
        request.setTrackingNo("TRACK1");
        service.submitReturn(1L, "AS1", request);
        assertEquals(4, sale.getStatus());
        service.submitReturn(1L, "AS1", request);
        request.setTrackingNo("TRACK2");
        assertThrows(BusinessException.class, () -> service.submitReturn(1L, "AS1", request));
        verify(mapper, times(1)).updateById(any(AfterSale.class));
    }

    @Test
    void cannotRefundBeforeInspection() {
        locked(2, 4);
        assertThrows(BusinessException.class, () -> service.refund(99L, "AS1"));
        verifyNoInteractions(refundService);
    }

    @Test
    void inspectionRequiresExplicitRestockDecision() {
        assertThrows(
                BusinessException.class,
                () -> service.receiveReturn(99L, "AS1", new ReceiveReturnRequest()));
        verifyNoInteractions(mapper);
    }

    @Test
    void damagedReturnCanBeRefundedWithoutRestocking() {
        AfterSale sale = locked(2, 4);
        persistence();
        ReceiveReturnRequest request = new ReceiveReturnRequest();
        request.setRestock(false);
        request.setRemark("破损，不可再次销售");
        service.receiveReturn(99L, "AS1", request);
        assertEquals(2, sale.getStatus());
        assertEquals(0, sale.getRestock());
        when(refundService.refundForAfterSale(1L, "MX1", "AS1", sale.getRefundAmount()))
                .thenReturn("RF1");
        service.refund(99L, "AS1");
        verify(orderService).completeAfterSale(1L, "MX1", "AS1", false);
        assertEquals(5, sale.getStatus());
    }

    @Test
    void successfulRefundIsIdempotent() {
        AfterSale sale = locked(1, 2);
        sale.setRestock(1);
        persistence();
        when(refundService.refundForAfterSale(1L, "MX1", "AS1", sale.getRefundAmount()))
                .thenReturn("RF1");
        service.refund(99L, "AS1");
        AfterSaleVO repeated = service.refund(99L, "AS1");
        assertEquals("RF1", repeated.getRefundNo());
        assertEquals("MX1", sale.getActiveOrderNo());
        verify(refundService, times(1))
                .refundForAfterSale(1L, "MX1", "AS1", sale.getRefundAmount());
        verify(orderService, times(1)).completeAfterSale(1L, "MX1", "AS1", true);
    }

    @Test
    void paymentFailureDoesNotCompleteOrderOrApplication() {
        AfterSale sale = locked(1, 2);
        sale.setRestock(1);
        when(refundService.refundForAfterSale(any(), anyString(), anyString(), any()))
                .thenThrow(new IllegalStateException("模拟支付失败"));
        assertThrows(IllegalStateException.class, () -> service.refund(99L, "AS1"));
        verifyNoInteractions(orderService);
        verify(mapper, never()).updateById(any(AfterSale.class));
    }

    @Test
    void springTransactionRollsBackWhenOrderCompletionFails() throws Exception {
        AfterSale sale = locked(1, 2);
        sale.setRestock(1);
        when(refundService.refundForAfterSale(any(), anyString(), anyString(), any()))
                .thenReturn("RF1");
        doThrow(new IllegalStateException("库存回补失败"))
                .when(orderService)
                .completeAfterSale(1L, "MX1", "AS1", true);
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getAutoCommit()).thenReturn(true);
        ProxyFactory factory = new ProxyFactory(service);
        TransactionInterceptor transactionInterceptor = new TransactionInterceptor();
        transactionInterceptor.setTransactionManager(new DataSourceTransactionManager(dataSource));
        transactionInterceptor.setTransactionAttributeSource(
                new AnnotationTransactionAttributeSource());
        factory.addAdvice(transactionInterceptor);
        AfterSaleService proxy = (AfterSaleService) factory.getProxy();
        assertThrows(IllegalStateException.class, () -> proxy.refund(99L, "AS1"));
        verify(connection).rollback();
        verify(connection, never()).commit();
        verify(mapper, never()).updateById(any(AfterSale.class));
    }

    @Test
    void paginationAndIdentityAreValidated() {
        assertThrows(BusinessException.class, () -> service.pageMine(null, null, 1, 20));
        assertThrows(BusinessException.class, () -> service.pageMine(1L, null, 0, 20));
        assertThrows(BusinessException.class, () -> service.pageMine(1L, null, 1, 101));
        assertThrows(BusinessException.class, () -> service.pageMine(1L, 99, 1, 20));
    }

    private AfterSale locked(int type, int status) {
        AfterSale sale = new AfterSale();
        sale.setId(1L);
        sale.setAfterSaleNo("AS1");
        sale.setOrderNo("MX1");
        sale.setUserId(1L);
        sale.setActiveOrderNo("MX1");
        sale.setType(type);
        sale.setStatus(status);
        sale.setRefundAmount(new BigDecimal("85.00"));
        sale.setCreatedAt(LocalDateTime.now());
        when(mapper.selectByAfterSaleNoForUpdate("AS1")).thenReturn(sale);
        return sale;
    }

    private void persistence() {
        when(mapper.updateById(any(AfterSale.class))).thenReturn(1);
        when(logService.save(any())).thenReturn(true);
    }

    private CreateAfterSaleRequest application(int type) {
        CreateAfterSaleRequest request = new CreateAfterSaleRequest();
        request.setOrderNo("MX1");
        request.setType(type);
        request.setReason("需要整单退款");
        return request;
    }

    private AuditAfterSaleRequest audit(boolean approved) {
        AuditAfterSaleRequest request = new AuditAfterSaleRequest();
        request.setApproved(approved);
        request.setRemark(approved ? "同意" : "不符合申请条件");
        return request;
    }
}
