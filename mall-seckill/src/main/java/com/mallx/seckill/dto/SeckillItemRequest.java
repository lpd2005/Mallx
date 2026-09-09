package com.mallx.seckill.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

import java.math.BigDecimal;

/** 为活动配置秒杀 SKU。 */
@Data
public class SeckillItemRequest {
    /** 参加活动的已上架 SKU。 */
    @NotNull(message = "SKU不能为空")
    private Long skuId;

    /** 秒杀成交价，业务层校验其不能高于商品当前销售价。 */
    @NotNull(message = "秒杀价格不能为空")
    @DecimalMin(value = "0.01", message = "秒杀价格必须大于0")
    private BigDecimal seckillPrice;

    /** 本场活动可抢占的资格库存。 */
    @NotNull(message = "秒杀库存不能为空")
    @Min(value = 1, message = "秒杀库存必须大于0")
    private Integer totalStock;

    /** 单个用户累计可购买的最大数量。 */
    @NotNull(message = "用户限购数量不能为空")
    @Min(value = 1, message = "用户限购数量必须大于0")
    private Integer limitPerUser;
}
