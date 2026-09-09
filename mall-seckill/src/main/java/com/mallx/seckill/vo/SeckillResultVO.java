package com.mallx.seckill.vo;

import lombok.Builder;
import lombok.Data;

/** 秒杀异步结果。 */
@Data
@Builder
public class SeckillResultVO {
    /** 提交抢购时返回的请求号。 */
    private String requestNo;
    /** 异步状态：0处理中、1成功、2失败。 */
    private Integer status;
    /** 成功时生成的正式订单号。 */
    private String orderNo;
    /** 失败时对用户展示的业务原因。 */
    private String failReason;
}
