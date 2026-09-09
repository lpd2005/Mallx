package com.mallx.seckill.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** 秒杀商品展示信息。 */
@Data
@Builder
public class SeckillItemVO {
    /** 秒杀商品配置 ID，也是前台抢购接口参数。 */
    private Long itemId;
    /** 商品 SKU ID。 */
    private Long skuId;
    /** 秒杀成交价。 */
    private BigDecimal seckillPrice;
    /** 本场活动资格库存总数。 */
    private Integer totalStock;
    /** 已成功创建正式订单的数量。 */
    private Integer soldStock;
    /** 单用户累计限购数量。 */
    private Integer limitPerUser;
    /** 商品配置状态：0禁用、1启用。 */
    private Integer status;
}
