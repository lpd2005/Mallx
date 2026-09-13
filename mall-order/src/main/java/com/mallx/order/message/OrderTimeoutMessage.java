package com.mallx.order.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 创建订单后发送的延迟关单消息，只携带重新定位订单所需的最小数据。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderTimeoutMessage {
    /** 稳定业务主键，同时写入AMQP messageId。 */
    private String orderNo;

    /** 数据库中的最晚支付时间；最终是否过期仍以数据库记录为准。 */
    private LocalDateTime expireAt;
}
