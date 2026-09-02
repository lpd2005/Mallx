package com.mallx.order.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.mallx.cart.service.CartService;
import com.mallx.common.exception.BusinessException;
import com.mallx.coupon.service.CouponUserService;
import com.mallx.inventory.service.InventoryStockService;
import com.mallx.order.entity.MallOrder;
import com.mallx.order.entity.OrderItem;
import com.mallx.order.enums.OrderStatus;
import com.mallx.order.mapper.MallOrderMapper;
import com.mallx.order.service.OrderItemService;
import com.mallx.order.service.OrderOperationLogService;
import com.mallx.order.vo.OrderAfterSaleVO;
import com.mallx.user.service.UserAddressService;
import com.mallx.user.service.UserMembershipService;

import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class OrderAfterSaleTest {
    @Mock MallOrderMapper mapper;
    @Mock OrderItemService itemService;
    @Mock OrderOperationLogService logService;
    @Mock CartService cartService;
    @Mock UserAddressService addressService;
    @Mock UserMembershipService membershipService;
    @Mock InventoryStockService inventoryService;
    @Mock CouponUserService couponService;
    @Mock LambdaQueryChainWrapper<OrderItem> itemQuery;
    OrderServiceImpl service;

    @BeforeAll
    static void metadata() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "order-after-test"),
                MallOrder.class);
    }

    @BeforeEach
    void setup() {
        service =
                new OrderServiceImpl(
                        mapper,
                        itemService,
                        logService,
                        cartService,
                        addressService,
                        membershipService,
                        inventoryService,
                        couponService) {
                    @Override
                    public Class<MallOrder> getEntityClass() {
                        return MallOrder.class;
                    }

                    @Override
                    public Class<MallOrderMapper> getMapperClass() {
                        return MallOrderMapper.class;
                    }
                };
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
    }

    @Test
    void beginSavesOriginalStatusAndRefundAmount() {
        MallOrder order = locked(OrderStatus.WAIT_SHIP);
        persist();
        OrderAfterSaleVO result = service.beginAfterSale(1L, "MX1", "AS1", false);
        assertEquals(3, result.getOriginalStatus());
        assertEquals(new BigDecimal("85.00"), result.getPayAmount());
        assertEquals(7, order.getStatus());
        assertEquals("AS1", order.getAfterSaleNo());
    }

    @Test
    void shippedOrderRejectsRefundWithoutReturn() {
        locked(OrderStatus.SHIPPED);
        assertThrows(
                BusinessException.class, () -> service.beginAfterSale(1L, "MX1", "AS1", false));
    }

    @Test
    void unpaidAndAlreadyRefundedOrdersCannotApply() {
        MallOrder order = locked(OrderStatus.WAIT_PAY);
        assertThrows(
                BusinessException.class, () -> service.beginAfterSale(1L, "MX1", "AS1", false));
        order.setStatus(8);
        assertThrows(BusinessException.class, () -> service.beginAfterSale(1L, "MX1", "AS1", true));
    }

    @Test
    void activeAfterSaleBlocksShippingAndReceipt() {
        locked(OrderStatus.AFTER_SALE);
        assertThrows(BusinessException.class, () -> service.shipOrder("MX1", 99L));
        assertThrows(BusinessException.class, () -> service.confirmReceipt(1L, "MX1"));
    }

    @Test
    void rejectionRestoresOriginalOrderStatus() {
        MallOrder order = bound();
        persist();
        service.restoreAfterSale(1L, "MX1", "AS1", 99L);
        assertEquals(3, order.getStatus());
        assertEquals("AS1", order.getAfterSaleNo());
    }

    @Test
    void staleApplicationCannotRestoreNewAfterSale() {
        bound();
        assertThrows(
                BusinessException.class, () -> service.restoreAfterSale(1L, "MX1", "OLD-AS", 99L));
        verify(mapper, never()).updateById(any(MallOrder.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void refundRestocksOnceAndReturnsCoupon() {
        MallOrder order = bound();
        order.setCouponUserId(55L);
        persist();
        OrderItem item = new OrderItem();
        item.setSkuId(10L);
        item.setQuantity(2);
        when(itemService.lambdaQuery()).thenReturn(itemQuery);
        when(itemQuery.eq(any(SFunction.class), any())).thenReturn(itemQuery);
        when(itemQuery.orderByAsc(any(SFunction.class))).thenReturn(itemQuery);
        when(itemQuery.list()).thenReturn(List.of(item));
        service.completeAfterSale(1L, "MX1", "AS1", true);
        service.completeAfterSale(1L, "MX1", "AS1", true);
        assertEquals(8, order.getStatus());
        assertNotNull(order.getRefundedAt());
        verify(inventoryService, times(1)).restockAfterSale(10L, 2, "AS1");
        verify(couponService, times(1)).rollbackForOrder(55L, "MX1");
        verify(inventoryService, never()).releaseStock(anyLong(), anyInt(), anyString());
    }

    @Test
    void damagedReturnRefundDoesNotIncreaseSellableStock() {
        MallOrder order = bound();
        persist();
        service.completeAfterSale(1L, "MX1", "AS1", false);
        assertEquals(8, order.getStatus());
        verifyNoInteractions(inventoryService, itemService);
    }

    private MallOrder locked(OrderStatus status) {
        MallOrder order = new MallOrder();
        order.setId(10L);
        order.setOrderNo("MX1");
        order.setUserId(1L);
        order.setStatus(status.getValue());
        order.setPayAmount(new BigDecimal("85.00"));
        when(mapper.selectByOrderNoForUpdate("MX1")).thenReturn(order);
        return order;
    }

    private MallOrder bound() {
        MallOrder order = locked(OrderStatus.AFTER_SALE);
        order.setAfterSaleNo("AS1");
        order.setStatusBeforeAfterSale(3);
        return order;
    }

    private void persist() {
        when(mapper.updateById(any(MallOrder.class))).thenReturn(1);
        when(logService.save(any())).thenReturn(true);
    }
}
