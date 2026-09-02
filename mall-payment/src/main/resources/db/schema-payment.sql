-- MallX 模拟支付模块建表脚本（MySQL 8）。
CREATE TABLE IF NOT EXISTS payment_record (
    id BIGINT NOT NULL COMMENT '支付记录主键',
    payment_no VARCHAR(64) NOT NULL COMMENT '支付单号',
    order_no VARCHAR(64) NOT NULL COMMENT '关联订单号',
    user_id BIGINT NOT NULL COMMENT '支付用户ID',
    amount DECIMAL(12,2) NOT NULL COMMENT '支付金额快照',
    channel TINYINT NOT NULL DEFAULT 1 COMMENT '支付渠道：1模拟支付',
    status TINYINT NOT NULL COMMENT '支付状态：1待支付，2已支付，3已关闭，4已退款',
    paid_request_no VARCHAR(64) DEFAULT NULL COMMENT '支付请求幂等号',
    paid_at DATETIME DEFAULT NULL COMMENT '支付成功时间',
    closed_at DATETIME DEFAULT NULL COMMENT '支付单关闭时间',
    refunded_at DATETIME DEFAULT NULL COMMENT '退款成功时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_no (payment_no),
    UNIQUE KEY uk_payment_order (order_no),
    UNIQUE KEY uk_payment_request (paid_request_no),
    KEY idx_payment_user (user_id, created_at),
    CONSTRAINT chk_payment_amount CHECK (amount >= 0),
    CONSTRAINT chk_payment_status CHECK (status IN (1, 2, 3, 4)),
    CONSTRAINT chk_payment_channel CHECK (channel IN (1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付记录';
