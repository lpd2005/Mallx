-- 模拟整单退款成功流水。执行前应已创建payment_record。
CREATE TABLE IF NOT EXISTS payment_refund (
    id BIGINT NOT NULL COMMENT '退款记录主键',
    refund_no VARCHAR(64) NOT NULL COMMENT '退款单号',
    after_sale_no VARCHAR(64) NOT NULL COMMENT '售后单号，同时作为退款幂等键',
    payment_no VARCHAR(64) NOT NULL COMMENT '原支付单号',
    order_no VARCHAR(64) NOT NULL COMMENT '原订单号，当前一个订单只允许整单退款一次',
    user_id BIGINT NOT NULL COMMENT '原支付用户ID',
    amount DECIMAL(12,2) NOT NULL COMMENT '实际整单退款金额，零元支付可退零元',
    refunded_at DATETIME NOT NULL COMMENT '模拟退款完成时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '流水创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_refund_no (refund_no),
    UNIQUE KEY uk_refund_after_sale (after_sale_no),
    UNIQUE KEY uk_refund_order (order_no),
    KEY idx_refund_user (user_id, created_at),
    CONSTRAINT chk_refund_amount CHECK (amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模拟整单退款成功流水';
