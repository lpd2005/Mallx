package com.mallx.seckill.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 秒杀入口只返回排队凭证，不同步等待订单创建。 */
@Data
@AllArgsConstructor
public class SeckillSubmitVO {
    /** 客户端后续查询结果使用的请求号。 */
    private String requestNo;
    /** 提交成功固定为处理中状态，正式结果需要异步查询。 */
    private Integer status;
}
