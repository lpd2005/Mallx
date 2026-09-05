package com.mallx.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 下单时固化的商品成交快照。 */
@Data
@TableName("order_item")
public class OrderItem {
    /** 订单明细主键。 */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 订单主键。 */
    private Long orderId;

    /** 冗余订单号，便于排查和查询。 */
    private String orderNo;

    private Long spuId;
    private Long skuId;

    /** 以下商品字段均为成交时快照，商品后续改名、改价不会影响历史订单。 */
    private String productName;

    private String skuName;
    private String specJson;

    /** 图片完整访问URL；沿用OssKey字段名，不再保存对象Key。空值表示未设置图片。 */
    private String imageOssKey;

    /** 成交单价。 */
    private BigDecimal unitPrice;

    /** 购买数量。 */
    private Integer quantity;

    /** 明细小计。 */
    private BigDecimal subtotal;

    private LocalDateTime createdAt;
}
