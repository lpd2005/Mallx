package com.mallx.order.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mallx.cart.service.CartService;
import com.mallx.cart.vo.*;
import com.mallx.common.enums.ResultCode;
import com.mallx.common.exception.BusinessException;
import com.mallx.coupon.service.CouponUserService;
import com.mallx.coupon.vo.CouponCalculationVO;
import com.mallx.inventory.service.InventoryStockService;
import com.mallx.order.dto.CreateOrderRequest;
import com.mallx.order.entity.*;
import com.mallx.order.enums.*;
import com.mallx.order.mapper.MallOrderMapper;
import com.mallx.order.service.*;
import com.mallx.order.vo.*;
import com.mallx.user.service.*;
import com.mallx.user.vo.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;

import java.math.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/** 普通订单编排实现，数据库事务统一保护订单、库存和优惠券。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<MallOrderMapper, MallOrder>
        implements OrderService {
    private static final int MAX_BATCH = 500; // 一次性处理的最大订单数
    private static final int PAY_MINUTES = 30;// 支付超时分钟数
    private final MallOrderMapper orderMapper;
    private final OrderItemService itemService;
    private final OrderOperationLogService logService;
    private final CartService cartService;
    private final UserAddressService addressService;
    private final UserMembershipService membershipService;
    private final InventoryStockService inventoryService;
    private final CouponUserService couponService;

    /** 从购物车已勾选项重新取价并创建订单；Redis购物车只在事务提交后清理。 */
    @Override
    @Transactional
    public OrderVO createOrder(Long userId, CreateOrderRequest request) {
        requireUser(userId); // 校验用户身份，无效用户直接抛出未授权异常
        UserAddressVO address = addressService.getAddress(userId, request.getAddressId()); // 查询并校验收货地址归属当前用户
        CartCheckoutVO checkout = cartService.checkout(userId); // 结算购物车：重新取价并校验上架与库存，返回勾选项
        List<CartItemVO> cartItems =
                checkout.getItems().stream()
                        .sorted(Comparator.comparing(CartItemVO::getSkuId)) // 按SKU升序排列，统一锁库存顺序防止并发死锁
                        .toList();
        String orderNo = "MX" + IdWorker.getIdStr(); // 生成全局唯一订单号，作为库存与优惠券的幂等键
        LocalDateTime now = LocalDateTime.now(); // 记录下单时间
        BigDecimal product = money(checkout.getProductAmount()); // 商品总额统一保留两位小数
        UserMembershipVO member = membershipService.getCurrentMembership(userId); // 查询当前会员信息获取折扣率
        BigDecimal rate =
                member.getDiscountRate() == null ? BigDecimal.ONE : member.getDiscountRate(); // 非会员折扣率按1（原价）处理
        BigDecimal memberAmount = product.multiply(rate).setScale(2, RoundingMode.HALF_UP); // 会员折后金额，即优惠券计算基数eligibleAmount
        BigDecimal memberDiscount = product.subtract(memberAmount); // 会员折扣省下的金额
        BigDecimal couponDiscount = BigDecimal.ZERO; // 优惠券优惠金额，默认未用券为0
        Long couponUserId = request.getCouponUserId(); // 取出用户选择的优惠券ID
        if (couponUserId != null) { // 仅当选择了优惠券才执行锁券
            CouponCalculationVO c =
                    couponService.lockForOrder(userId, couponUserId, orderNo, memberAmount); // 按会员折后金额锁定优惠券并计算优惠
            couponDiscount = c.getDiscountAmount(); // 记录优惠券优惠金额
        }
        for (CartItemVO item : cartItems)
            inventoryService.lockStock(item.getSkuId(), item.getQuantity(), orderNo); // 逐个SKU原子锁定库存，任一失败则整体回滚
        MallOrder order =
                buildOrder( // 组装订单主表实体（状态待支付，应付=总额-会员优惠-券优惠）
                        userId, orderNo, checkout, address, member, rate, product, memberDiscount, couponDiscount, request, now);
        if (!save(order)) throw new BusinessException(ResultCode.INTERNAL_ERROR, "订单创建失败"); // 保存订单主表，失败抛出异常回滚
        List<OrderItem> items = cartItems.stream().map(i -> buildItem(order, i)).toList(); // 按购物车项生成订单明细快照
        if (!itemService.saveBatch(items))
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "订单商品保存失败"); // 批量保存订单明细，失败抛出异常回滚
        if (couponUserId != null) couponService.confirmUsed(couponUserId, orderNo); // 订单数据落库成功后确认用券，券状态由锁定转为已使用
        saveLog(order, null, OrderStatus.WAIT_PAY, OrderOperationType.CREATE, userId, "创建订单"); // 记录订单创建操作流水
        afterCommitClearCart(userId, cartItems.stream().map(CartItemVO::getSkuId).toList()); // 注册事务提交后回调，成功提交才清理Redis购物车
        return toVO(order, items); // 组装订单视图返回
    }

    @Override
    public List<OrderVO> listOrders(Long userId, Integer status) {
        requireUser(userId);
        if (status != null && !OrderStatus.supports(status)) throw bad("订单状态不正确");
        List<MallOrder> orders =
                lambdaQuery()
                        .eq(MallOrder::getUserId, userId)
                        .eq(status != null, MallOrder::getStatus, status)
                        .orderByDesc(MallOrder::getCreatedAt)
                        .list();
        if (orders.isEmpty()) return List.of();
        Map<Long, List<OrderItem>> groups =
                itemService
                        .lambdaQuery()
                        .in(OrderItem::getOrderId, orders.stream().map(MallOrder::getId).toList())
                        .list()
                        .stream()
                        .collect(Collectors.groupingBy(OrderItem::getOrderId));
        return orders.stream()
                .map(o -> toVO(o, groups.getOrDefault(o.getId(), List.of())))
                .toList();
    }

    @Override
    public OrderVO getOrder(Long userId, String orderNo) {
        MallOrder o = requireOwned(userId, orderNo);
        return toVO(o, items(o.getId()));
    }

    /** 待支付订单取消时在同一事务释放库存并回滚优惠券。 */
    @Override
    @Transactional
    public void cancelOrder(Long userId, String orderNo, String reason) {
        requireUser(userId);
        MallOrder o = lockOrder(orderNo);
        if (!userId.equals(o.getUserId())) throw forbidden();
        cancelLocked(o, reason, userId, OrderOperationType.CANCEL);
    }

    /**
     * 把“已支付”的订单推进到“待发货”状态
     */
    @Override
    @Transactional
    public void prepareShipment(String orderNo, Long operatorId) {
        transition(
                orderNo,
                OrderStatus.PAID,
                OrderStatus.WAIT_SHIP,
                OrderOperationType.PREPARE_SHIPMENT,
                operatorId,
                "订单进入待发货");
    }

    /**
     * 把订单从“待发货”推进到“已发货”，并记录发货时间
     */
    @Override
    @Transactional
    public void shipOrder(String orderNo, Long operatorId) {
        MallOrder o =
                transition(
                        orderNo,
                        OrderStatus.WAIT_SHIP,
                        OrderStatus.SHIPPED,
                        OrderOperationType.SHIP,
                        operatorId,
                        "订单已发货");
        o.setShippedAt(LocalDateTime.now());
        updateRequired(o, "订单发货时间保存失败");
    }

    /**
     * 用户本人的操作，SHIPPED（已发货）→ COMPLETED（已完成）
     */
    @Override
    @Transactional
    public void confirmReceipt(Long userId, String orderNo) {
        requireUser(userId);
        MallOrder o = lockOrder(orderNo);
        if (!userId.equals(o.getUserId())) throw forbidden();
        requireStatus(o, OrderStatus.SHIPPED);
        Integer before = o.getStatus();
        o.setStatus(OrderStatus.COMPLETED.getValue());
        o.setCompletedAt(LocalDateTime.now());
        updateRequired(o, "确认收货失败");
        saveLog(o, before, OrderStatus.COMPLETED, OrderOperationType.RECEIVE, userId, "用户确认收货");
    }

    /**
     *  获取支付信息
     */
    @Override
    public OrderPaymentInfoVO getPaymentInfo(Long userId, String orderNo) {
        MallOrder o = requireOwned(userId, orderNo);
        return paymentInfo(o);
    }

    /** 支付模块调用；行锁与库存幂等流水共同保证重复支付通知安全。 */
    @Override
    @Transactional
    public void markPaid(String orderNo, String paymentNo, LocalDateTime paidAt) {
        MallOrder o = lockOrder(orderNo);
        if (!OrderStatus.WAIT_PAY.matches(o.getStatus())) {
            if (OrderStatus.PAID.matches(o.getStatus())
                    || OrderStatus.WAIT_SHIP.matches(o.getStatus())
                    || OrderStatus.SHIPPED.matches(o.getStatus())
                    || OrderStatus.COMPLETED.matches(o.getStatus())) return;
            throw bad("订单当前不能支付");
        }
        if (!o.getExpireAt().isAfter(LocalDateTime.now())) throw bad("订单已超过支付时间");
        for (OrderItem i : items(o.getId()))
            inventoryService.deductStock(i.getSkuId(), i.getQuantity(), o.getOrderNo());
        Integer before = o.getStatus();
        o.setStatus(OrderStatus.PAID.getValue());
        o.setPaidAt(paidAt == null ? LocalDateTime.now() : paidAt);
        updateRequired(o, "订单支付状态更新失败");
        saveLog(o, before, OrderStatus.PAID, OrderOperationType.PAY, null, "支付单" + paymentNo + "支付成功");
    }

    //TODO MQ延迟消息
    /** 当前阶段使用数据库定时扫描关闭超时待支付订单，后续再替换为MQ延迟消息。 */
    @Override
    @Transactional
    public int closeExpiredOrders(int batchSize) {
        if (batchSize <= 0 || batchSize > MAX_BATCH) throw bad("批次大小必须在1到500之间");
        LocalDateTime now = LocalDateTime.now();
        List<String> nos =
                lambdaQuery()
                        .select(MallOrder::getOrderNo)
                        .eq(MallOrder::getStatus, OrderStatus.WAIT_PAY.getValue())
                        .le(MallOrder::getExpireAt, now)
                        .orderByAsc(MallOrder::getExpireAt)
                        .last("LIMIT " + batchSize)
                        .list()
                        .stream()
                        .map(MallOrder::getOrderNo)
                        .toList();
        int count = 0;
        for (String no : nos) {
            MallOrder o = lockOrder(no);
            if (OrderStatus.WAIT_PAY.matches(o.getStatus()) && !o.getExpireAt().isAfter(now)) {
                cancelLocked(o, "订单超时未支付", null, OrderOperationType.TIMEOUT_CLOSE);
                count++;
            }
        }
        return count;
    }

    private void cancelLocked(MallOrder o, String reason, Long operator, OrderOperationType type) {
        if (OrderStatus.CANCELED.matches(o.getStatus())) return;
        requireStatus(o, OrderStatus.WAIT_PAY);
        for (OrderItem i : items(o.getId()))
            inventoryService.releaseStock(i.getSkuId(), i.getQuantity(), o.getOrderNo());
        if (o.getCouponUserId() != null)
            couponService.rollbackForOrder(o.getCouponUserId(), o.getOrderNo());
        Integer before = o.getStatus();
        o.setStatus(OrderStatus.CANCELED.getValue());
        o.setCanceledAt(LocalDateTime.now());
        o.setCancelReason(trim(reason, 255));
        updateRequired(o, "订单取消失败");
        saveLog(o, before, OrderStatus.CANCELED, type, operator, reason);
    }

    //
    private MallOrder transition(
            String no,
            OrderStatus expected,
            OrderStatus target,
            OrderOperationType type,
            Long operator,
            String remark) {
        MallOrder o = lockOrder(no);
        if (target.matches(o.getStatus())) return o;
        requireStatus(o, expected);
        Integer before = o.getStatus();
        o.setStatus(target.getValue());
        updateRequired(o, "订单状态更新失败");
        saveLog(o, before, target, type, operator, remark);
        return o;
    }

    private void requireStatus(MallOrder o, OrderStatus s) {
        if (!s.matches(o.getStatus())) throw bad("订单当前状态不允许该操作");
    }

    private MallOrder lockOrder(String no) {
        String n = requireNo(no);
        MallOrder o = orderMapper.selectByOrderNoForUpdate(n);
        if (o == null) throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        return o;
    }

    private MallOrder requireOwned(Long uid, String no) {
        requireUser(uid);
        MallOrder o =
                lambdaQuery()
                        .eq(MallOrder::getOrderNo, requireNo(no))
                        .eq(MallOrder::getUserId, uid)
                        .one();
        if (o == null) throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        return o;
    }

    private List<OrderItem> items(Long id) {
        return itemService
                .lambdaQuery()
                .eq(OrderItem::getOrderId, id)
                .orderByAsc(OrderItem::getId)
                .list();
    }

    private void updateRequired(MallOrder o, String msg) {
        if (!updateById(o)) throw new BusinessException(ResultCode.INTERNAL_ERROR, msg);
    }

    private void saveLog(
            MallOrder o,
            Integer before,
            OrderStatus after,
            OrderOperationType type,
            Long operator,
            String remark) {
        OrderOperationLog l = new OrderOperationLog();
        l.setOrderId(o.getId());
        l.setOrderNo(o.getOrderNo());
        l.setBeforeStatus(before);
        l.setAfterStatus(after.getValue());
        l.setOperationType(type.getValue());
        l.setOperatorId(operator);
        l.setRemark(trim(remark, 255));
        if (!logService.save(l)) throw new BusinessException(ResultCode.INTERNAL_ERROR, "订单流水保存失败");
    }

    private MallOrder buildOrder(
            Long uid,
            String no,
            CartCheckoutVO c,
            UserAddressVO a,
            UserMembershipVO m,
            BigDecimal rate,
            BigDecimal product,
            BigDecimal md,
            BigDecimal cd,
            CreateOrderRequest r,
            LocalDateTime now) {
        MallOrder o = new MallOrder();
        o.setOrderNo(no);
        o.setUserId(uid);
        o.setStatus(OrderStatus.WAIT_PAY.getValue());
        o.setTotalQuantity(c.getTotalQuantity());
        o.setProductAmount(product);
        o.setMemberDiscountAmount(md);
        o.setCouponDiscountAmount(cd);
        o.setPayAmount(
                product.subtract(md)
                        .subtract(cd)
                        .max(BigDecimal.ZERO)
                        .setScale(2, RoundingMode.HALF_UP));
        o.setCouponUserId(r.getCouponUserId());
        o.setMemberLevelCode(m.getLevelCode());
        o.setMemberDiscountRate(rate);
        o.setRecipientName(a.getRecipientName());
        o.setRecipientMobile(a.getRecipientMobile());
        o.setProvinceName(a.getProvinceName());
        o.setCityName(a.getCityName());
        o.setDistrictName(a.getDistrictName());
        o.setDetailAddress(a.getDetailAddress());
        o.setPostalCode(a.getPostalCode());
        o.setRemark(trim(r.getRemark(), 255));
        o.setExpireAt(now.plusMinutes(PAY_MINUTES));
        return o;
    }

    private OrderItem buildItem(MallOrder o, CartItemVO c) {
        OrderItem i = new OrderItem();
        i.setOrderId(o.getId());
        i.setOrderNo(o.getOrderNo());
        i.setSpuId(c.getSpuId());
        i.setSkuId(c.getSkuId());
        i.setProductName(c.getProductName());
        i.setSkuName(c.getSkuName());
        i.setSpecJson(c.getSpecJson());
        i.setImageOssKey(c.getImageOssKey());
        i.setUnitPrice(c.getSalePrice());
        i.setQuantity(c.getQuantity());
        i.setSubtotal(c.getSubtotal());
        return i;
    }

    private OrderVO toVO(MallOrder o, List<OrderItem> items) {
        return OrderVO.builder()
                .orderNo(o.getOrderNo())
                .status(o.getStatus())
                .totalQuantity(o.getTotalQuantity())
                .productAmount(o.getProductAmount())
                .memberDiscountAmount(o.getMemberDiscountAmount())
                .couponDiscountAmount(o.getCouponDiscountAmount())
                .payAmount(o.getPayAmount())
                .couponUserId(o.getCouponUserId())
                .memberLevelCode(o.getMemberLevelCode())
                .memberDiscountRate(o.getMemberDiscountRate())
                .recipientName(o.getRecipientName())
                .recipientMobile(o.getRecipientMobile())
                .fullAddress(
                        o.getProvinceName()
                                + o.getCityName()
                                + o.getDistrictName()
                                + o.getDetailAddress())
                .postalCode(o.getPostalCode())
                .remark(o.getRemark())
                .expireAt(o.getExpireAt())
                .paidAt(o.getPaidAt())
                .shippedAt(o.getShippedAt())
                .completedAt(o.getCompletedAt())
                .canceledAt(o.getCanceledAt())
                .cancelReason(o.getCancelReason())
                .createdAt(o.getCreatedAt())
                .items(items.stream().map(this::itemVO).toList())
                .build();
    }

    private OrderItemVO itemVO(OrderItem i) {
        return OrderItemVO.builder()
                .skuId(i.getSkuId())
                .spuId(i.getSpuId())
                .productName(i.getProductName())
                .skuName(i.getSkuName())
                .specJson(i.getSpecJson())
                .imageOssKey(i.getImageOssKey())
                .unitPrice(i.getUnitPrice())
                .quantity(i.getQuantity())
                .subtotal(i.getSubtotal())
                .build();
    }

    private OrderPaymentInfoVO paymentInfo(MallOrder o) {
        return OrderPaymentInfoVO.builder()
                .orderNo(o.getOrderNo())
                .userId(o.getUserId())
                .status(o.getStatus())
                .payAmount(o.getPayAmount())
                .expireAt(o.getExpireAt())
                .build();
    }

    private void afterCommitClearCart(Long uid, List<Long> ids) {
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            cartService.removePurchasedItems(uid, ids);
                        } catch (RuntimeException e) {
                            log.error("订单已提交但购物车清理失败,userId={},skuIds={}", uid, ids, e);
                        }
                    }
                });
    }

    private String requireNo(String no) {
        if (no == null || no.isBlank() || no.trim().length() > 64) throw bad("订单号不正确");
        return no.trim();
    }

    private void requireUser(Long id) {
        if (id == null || id <= 0) throw new BusinessException(ResultCode.UNAUTHORIZED, "当前用户身份无效");
    }

    private String trim(String s, int max) {
        if (s == null || s.isBlank()) return null;
        String v = s.trim();
        return v.length() > max ? v.substring(0, max) : v;
    }

    private BigDecimal money(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    private BusinessException bad(String m) {
        return new BusinessException(ResultCode.BAD_REQUEST, m);
    }

    private BusinessException forbidden() {
        return new BusinessException(ResultCode.FORBIDDEN, "无权操作该订单");
    }
}
