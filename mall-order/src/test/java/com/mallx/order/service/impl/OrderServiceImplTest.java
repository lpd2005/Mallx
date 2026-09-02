package com.mallx.order.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.mallx.cart.service.CartService;
import com.mallx.coupon.service.CouponUserService;
import com.mallx.inventory.service.InventoryStockService;
import com.mallx.order.entity.*;
import com.mallx.order.enums.OrderStatus;
import com.mallx.order.mapper.MallOrderMapper;
import com.mallx.order.service.*;
import com.mallx.user.service.*;

import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 订单支付确认和取消时库存、优惠券一致性测试。 */
@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {
    @BeforeAll
    static void metadata() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "order-test"),
                MallOrder.class);
    }

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

    @BeforeEach
    @SuppressWarnings("unchecked")
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
        when(itemService.lambdaQuery()).thenReturn(itemQuery);
        when(itemQuery.eq(any(SFunction.class), any())).thenReturn(itemQuery);
        when(itemQuery.orderByAsc(any(SFunction.class))).thenReturn(itemQuery);
        when(itemQuery.list()).thenReturn(List.of(item()));
    }

    @Test
    void markPaidShouldDeductLockedStockOnce() {
        MallOrder o = order(OrderStatus.WAIT_PAY);
        when(mapper.selectByOrderNoForUpdate("MX1")).thenReturn(o);
        when(mapper.updateById(any(MallOrder.class))).thenReturn(1);
        when(logService.save(any())).thenReturn(true);
        service.markPaid("MX1", "PAY1", LocalDateTime.now());
        assertEquals(OrderStatus.PAID.getValue(), o.getStatus());
        verify(inventoryService).deductStock(10L, 2, "MX1");
    }

    @Test
    void cancelShouldReleaseStockAndRollbackCoupon() {
        MallOrder o = order(OrderStatus.WAIT_PAY);
        o.setCouponUserId(99L);
        when(mapper.selectByOrderNoForUpdate("MX1")).thenReturn(o);
        when(mapper.updateById(any(MallOrder.class))).thenReturn(1);
        when(logService.save(any())).thenReturn(true);
        service.cancelOrder(1L, "MX1", "不想买了");
        assertEquals(OrderStatus.CANCELED.getValue(), o.getStatus());
        verify(inventoryService).releaseStock(10L, 2, "MX1");
        verify(couponService).rollbackForOrder(99L, "MX1");
    }

    private MallOrder order(OrderStatus s) {
        MallOrder o = new MallOrder();
        o.setId(2L);
        o.setOrderNo("MX1");
        o.setUserId(1L);
        o.setStatus(s.getValue());
        o.setExpireAt(LocalDateTime.now().plusMinutes(10));
        return o;
    }

    private OrderItem item() {
        OrderItem i = new OrderItem();
        i.setOrderId(2L);
        i.setSkuId(10L);
        i.setQuantity(2);
        i.setUnitPrice(new BigDecimal("10"));
        return i;
    }
}
