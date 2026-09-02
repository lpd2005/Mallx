package com.mallx.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 订单主记录，保存成交金额、地址快照和当前状态。 */
@Data
@TableName("mall_order")
public class MallOrder {
    /** 订单主键。 */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 对外展示的订单号。 */
    private String orderNo;

    /** 下单用户ID。 */
    private Long userId;

    /** 订单状态，见 {@code OrderStatus}。 */
    private Integer status;

    /** 商品购买总件数。 */
    private Integer totalQuantity;

    /** 商品原价合计。 */
    private BigDecimal productAmount;

    /** 会员优惠金额。 */
    private BigDecimal memberDiscountAmount;

    /** 优惠券优惠金额。 */
    private BigDecimal couponDiscountAmount;

    /** 最终应付金额。 */
    private BigDecimal payAmount;

    /** 本单使用的用户优惠券ID。 */
    private Long couponUserId;

    /** 下单时的会员等级快照。 */
    private Integer memberLevelCode;

    /** 下单时的会员折扣率快照。 */
    private BigDecimal memberDiscountRate;

    /** 以下字段为下单时的收货地址快照，地址修改不会影响历史订单。 */
    private String recipientName;

    private String recipientMobile;
    private String provinceName;
    private String cityName;
    private String districtName;
    private String detailAddress;
    private String postalCode;

    /** 用户订单备注。 */
    private String remark;

    /** 最晚支付时间。 */
    private LocalDateTime expireAt;

    /** 支付成功时间。 */
    private LocalDateTime paidAt;

    /** 发货时间。 */
    private LocalDateTime shippedAt;

    /** 订单完成时间。 */
    private LocalDateTime completedAt;

    /** 订单取消或超时关闭时间。 */
    private LocalDateTime canceledAt;

    /** 取消或超时关闭原因。 */
    private String cancelReason;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
