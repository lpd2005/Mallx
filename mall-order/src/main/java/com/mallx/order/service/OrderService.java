package com.mallx.order.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.mallx.order.dto.CreateOrderRequest;
import com.mallx.order.entity.MallOrder;
import com.mallx.order.vo.*;

import java.time.LocalDateTime;
import java.util.List;

/** 普通订单交易服务。 */
public interface OrderService extends IService<MallOrder> {
    OrderVO createOrder(Long userId, CreateOrderRequest request);

    List<OrderVO> listOrders(Long userId, Integer status);

    OrderVO getOrder(Long userId, String orderNo);

    void cancelOrder(Long userId, String orderNo, String reason);

    void prepareShipment(String orderNo, Long operatorId);

    void shipOrder(String orderNo, Long operatorId);

    void confirmReceipt(Long userId, String orderNo);

    OrderPaymentInfoVO getPaymentInfo(Long userId, String orderNo);

    void markPaid(String orderNo, String paymentNo, LocalDateTime paidAt);

    int closeExpiredOrders(int batchSize);
}
