package com.mallx.order.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.mallx.order.dto.CreateOrderRequest;
import com.mallx.order.entity.MallOrder;
import com.mallx.order.vo.*;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

/** 普通订单交易服务。 */
public interface OrderService extends IService<MallOrder> {
    OrderVO createOrder(Long userId, CreateOrderRequest request);

    /** 秒杀消费者调用的单商品幂等下单入口，requestNo 决定唯一订单号。 */
    OrderVO createSeckillOrder(Long userId, Long skuId, int quantity, BigDecimal seckillPrice, Long addressId, String requestNo);

    List<OrderVO> listOrders(Long userId, Integer status);

    OrderVO getOrder(Long userId, String orderNo);

    void cancelOrder(Long userId, String orderNo, String reason);

    void prepareShipment(String orderNo, Long operatorId);

    void shipOrder(String orderNo, Long operatorId);

    void confirmReceipt(Long userId, String orderNo);

    OrderPaymentInfoVO getPaymentInfo(Long userId, String orderNo);

    void markPaid(String orderNo, String paymentNo, LocalDateTime paidAt);

    /** 延迟消息调用的单订单关单入口；返回true表示本次实际完成关闭。 */
    boolean closeExpiredOrder(String orderNo);

    /** MQ漏投或消费异常时使用的数据库批量补偿入口。 */
    int closeExpiredOrders(int batchSize);

    /** 在调用方事务中锁定订单并进入售后；returnRequired区分未发货退款和退货退款。 */
    OrderAfterSaleVO beginAfterSale(Long userId, String orderNo, String afterSaleNo, boolean returnRequired);

    /** 售后驳回或撤销时恢复原履约状态。 */
    void restoreAfterSale(Long userId, String orderNo, String afterSaleNo, Long operatorId);

    /** 模拟退款成功后关闭整单，按验收结果回补库存并退还优惠券。 */
    void completeAfterSale(Long userId, String orderNo, String afterSaleNo, boolean restock);
}
