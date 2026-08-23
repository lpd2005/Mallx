-- MallX 优惠券模块建表脚本（MySQL 8）。
-- 第一版支持全场满减券和折扣券；同一用户对同一模板最多领取一次。
-- 模块化单体阶段不建立跨模块物理外键，user_id 与 auth_account.id 保持业务一致。

CREATE TABLE IF NOT EXISTS coupon_template (
    id BIGINT NOT NULL COMMENT '优惠券模板ID，由MyBatis-Plus雪花算法生成',
    coupon_name VARCHAR(100) NOT NULL COMMENT '优惠券名称',
    coupon_type TINYINT NOT NULL COMMENT '优惠券类型：1满减券，2折扣券',
    threshold_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '最低可优惠金额，0表示无门槛',
    discount_amount DECIMAL(10,2) DEFAULT NULL COMMENT '满减金额，仅满减券使用',
    discount_rate DECIMAL(5,4) DEFAULT NULL COMMENT '折扣率，例如0.8000表示8折，仅折扣券使用',
    max_discount_amount DECIMAL(10,2) DEFAULT NULL COMMENT '折扣券最高优惠金额，为空表示不限制',
    total_stock INT NOT NULL COMMENT '总发行数量',
    received_stock INT NOT NULL DEFAULT 0 COMMENT '已领取数量',
    claim_start_at DATETIME NOT NULL COMMENT '领取开始时间',
    claim_end_at DATETIME NOT NULL COMMENT '领取结束时间',
    valid_start_at DATETIME NOT NULL COMMENT '使用开始时间',
    valid_end_at DATETIME NOT NULL COMMENT '使用结束时间',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '模板状态：0草稿，1发放中，2已停止，3已结束',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_coupon_template_center (status, claim_start_at, claim_end_at),
    CONSTRAINT chk_coupon_template_type CHECK (coupon_type IN (1, 2)),
    CONSTRAINT chk_coupon_template_status CHECK (status IN (0, 1, 2, 3)),
    CONSTRAINT chk_coupon_template_threshold CHECK (threshold_amount >= 0),
    CONSTRAINT chk_coupon_template_stock CHECK (total_stock > 0 AND received_stock >= 0 AND received_stock <= total_stock),
    CONSTRAINT chk_coupon_template_time CHECK (
        claim_start_at < claim_end_at
        AND valid_start_at < valid_end_at
        AND claim_end_at < valid_end_at
    ),
    CONSTRAINT chk_coupon_template_rule CHECK (
        (coupon_type = 1 AND discount_amount > 0 AND discount_rate IS NULL AND max_discount_amount IS NULL)
        OR
        (coupon_type = 2 AND discount_amount IS NULL AND discount_rate > 0 AND discount_rate < 1
            AND (max_discount_amount IS NULL OR max_discount_amount > 0))
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券模板';

CREATE TABLE IF NOT EXISTS coupon_user (
    id BIGINT NOT NULL COMMENT '用户优惠券ID，由MyBatis-Plus雪花算法生成',
    template_id BIGINT NOT NULL COMMENT '优惠券模板ID',
    user_id BIGINT NOT NULL COMMENT '领取用户ID',
    status TINYINT NOT NULL COMMENT '用户券状态：1未使用，2已锁定，3已使用，4已过期',
    order_no VARCHAR(64) DEFAULT NULL COMMENT '当前绑定订单号',
    received_at DATETIME NOT NULL COMMENT '领取时间',
    valid_start_at DATETIME NOT NULL COMMENT '使用开始时间快照',
    valid_end_at DATETIME NOT NULL COMMENT '使用结束时间快照',
    locked_at DATETIME DEFAULT NULL COMMENT '订单锁券时间',
    used_at DATETIME DEFAULT NULL COMMENT '确认使用时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_coupon_user_template (user_id, template_id),
    UNIQUE KEY uk_coupon_user_order (order_no),
    KEY idx_coupon_user_list (user_id, status, valid_end_at),
    CONSTRAINT chk_coupon_user_status CHECK (status IN (1, 2, 3, 4)),
    CONSTRAINT chk_coupon_user_time CHECK (valid_start_at < valid_end_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户持有的优惠券';

CREATE TABLE IF NOT EXISTS coupon_operation_log (
    id BIGINT NOT NULL COMMENT '优惠券操作流水ID，由MyBatis-Plus雪花算法生成',
    operation_no VARCHAR(128) NOT NULL COMMENT '幂等操作号',
    user_coupon_id BIGINT NOT NULL COMMENT '用户优惠券ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    order_no VARCHAR(64) DEFAULT NULL COMMENT '关联订单号',
    operation_type TINYINT NOT NULL COMMENT '操作类型：1领取，2锁定，3使用，4回滚，5过期',
    before_status TINYINT DEFAULT NULL COMMENT '操作前用户券状态',
    after_status TINYINT NOT NULL COMMENT '操作后用户券状态',
    remark VARCHAR(255) DEFAULT NULL COMMENT '操作说明',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_coupon_operation_no (operation_no),
    KEY idx_coupon_operation_user_coupon (user_coupon_id, created_at),
    KEY idx_coupon_operation_order (order_no),
    CONSTRAINT chk_coupon_operation_type CHECK (operation_type IN (1, 2, 3, 4, 5)),
    CONSTRAINT chk_coupon_operation_after_status CHECK (after_status IN (1, 2, 3, 4))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券状态变更及幂等流水';
