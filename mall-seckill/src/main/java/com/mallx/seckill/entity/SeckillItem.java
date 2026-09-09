package com.mallx.seckill.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 活动中的一个秒杀 SKU。 */
@Data
@TableName("seckill_item")
public class SeckillItem {
    /** 秒杀商品配置主键。 */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    /** 所属秒杀活动 ID。 */
    private Long activityId;
    /** 参与秒杀的商品 SKU ID。 */
    private Long skuId;
    /** 秒杀成交单价，不叠加会员折扣和优惠券。 */
    private BigDecimal seckillPrice;
    /** 本场活动投入 Redis 的资格库存总数。 */
    private Integer totalStock;
    /** 已成功创建正式订单的数量，用于 Redis 数据丢失后的库存重建。 */
    private Integer soldStock;
    /** 一个用户在本场活动中最多可购买的数量。 */
    private Integer limitPerUser;
    /** 0禁用或预留库存已释放、1启用或仍等待关闭释放。 */
    private Integer status;
    /** 创建时间。 */
    private LocalDateTime createdAt;
    /** 最后更新时间。 */
    private LocalDateTime updatedAt;
}
