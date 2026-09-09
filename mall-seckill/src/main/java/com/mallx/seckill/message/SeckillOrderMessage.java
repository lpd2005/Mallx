package com.mallx.seckill.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Redis 预扣成功后发送的异步下单消息。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeckillOrderMessage {
    /** 秒杀请求号，也是正式订单幂等号的一部分。 */
    private String requestNo;
    /** 活动 ID。 */
    private Long activityId;
    /** 秒杀商品配置 ID。 */
    private Long itemId;
    /** 需要创建订单的 SKU ID。 */
    private Long skuId;
    /** 下单用户 ID。 */
    private Long userId;
    /** 收货地址 ID。 */
    private Long addressId;
    /** 购买数量。 */
    private Integer quantity;
    /** Redis 预热时保存的秒杀价快照。 */
    private BigDecimal seckillPrice;
}
