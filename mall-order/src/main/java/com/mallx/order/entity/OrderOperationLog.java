package com.mallx.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

import java.time.LocalDateTime;

/** 订单状态流转流水。 */
@Data
@TableName("order_operation_log")
public class OrderOperationLog {
    /** 流水主键。 */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long orderId;
    private String orderNo;

    /** 操作前状态；创建订单时为空。 */
    private Integer beforeStatus;

    /** 操作后状态。 */
    private Integer afterStatus;

    /** 操作类型，见 {@code OrderOperationType}。 */
    private Integer operationType;

    /** 操作人ID；系统超时任务操作时为空。 */
    private Long operatorId;

    private String remark;
    private LocalDateTime createdAt;
}
