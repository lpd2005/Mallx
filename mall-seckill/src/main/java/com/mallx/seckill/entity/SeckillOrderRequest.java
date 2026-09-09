package com.mallx.seckill.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

import java.time.LocalDateTime;

/** 秒杀请求持久化结果；正式订单仍由 mall-order 管理。 */
@Data
@TableName("seckill_order_request")
public class SeckillOrderRequest {
    /** 请求记录主键。 */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    /** Redis、MQ 和数据库之间共用的全局唯一请求号。 */
    private String requestNo;
    /** 活动 ID 快照。 */
    private Long activityId;
    /** 秒杀商品配置 ID 快照。 */
    private Long itemId;
    /** SKU ID 快照。 */
    private Long skuId;
    /** 发起请求的用户 ID。 */
    private Long userId;
    /** 下单时选择的收货地址 ID。 */
    private Long addressId;
    /** 本次购买数量。 */
    private Integer quantity;
    /** 创建成功后的正式订单号；处理中或失败时为空。 */
    private String orderNo;
    /** 0处理中、1成功、2失败。 */
    private Integer status;
    /** 确定性业务失败原因；系统异常保持处理中并继续恢复。 */
    private String failReason;
    /** 请求首次持久化时间。 */
    private LocalDateTime createdAt;
    /** 请求状态最后更新时间。 */
    private LocalDateTime updatedAt;
}
