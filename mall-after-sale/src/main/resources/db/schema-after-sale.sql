-- MallX整单售后模块（MySQL 8），跨模块ID只做业务关联。
CREATE TABLE IF NOT EXISTS after_sale (
    id BIGINT NOT NULL COMMENT '售后主键',
    after_sale_no VARCHAR(64) NOT NULL COMMENT '售后单号',
    order_id BIGINT NOT NULL COMMENT '原订单主键',
    order_no VARCHAR(64) NOT NULL COMMENT '原订单号',
    user_id BIGINT NOT NULL COMMENT '申请用户ID',
    type TINYINT NOT NULL COMMENT '售后类型：1未发货仅退款，2退货退款',
    status TINYINT NOT NULL COMMENT '售后状态：1待审核，2待退款，3待寄回，4待验收，5已完成，6已驳回，7已撤销',
    active_order_no VARCHAR(64) DEFAULT NULL COMMENT '订单占位键，处理中和退款完成保留，驳回或撤销置空',
    original_order_status TINYINT NOT NULL COMMENT '申请前订单状态：2已支付，3待发货，4已发货，5已完成',
    refund_amount DECIMAL(12,2) NOT NULL COMMENT '原订单实际应付金额快照，不接受客户端定价',
    reason VARCHAR(500) NOT NULL COMMENT '用户申请原因',
    audit_remark VARCHAR(255) DEFAULT NULL COMMENT '审核说明',
    auditor_id BIGINT DEFAULT NULL COMMENT '审核操作人ID',
    restock TINYINT DEFAULT NULL COMMENT '是否回补可销售库存：0否，1是；退货验收前为空',
    return_carrier VARCHAR(64) DEFAULT NULL COMMENT '退货物流公司',
    return_tracking_no VARCHAR(64) DEFAULT NULL COMMENT '退货物流单号',
    refund_no VARCHAR(64) DEFAULT NULL COMMENT '成功退款单号',
    audited_at DATETIME DEFAULT NULL COMMENT '审核时间',
    return_shipped_at DATETIME DEFAULT NULL COMMENT '用户提交退货物流时间',
    received_at DATETIME DEFAULT NULL COMMENT '仓库验收确认时间',
    completed_at DATETIME DEFAULT NULL COMMENT '退款完成时间',
    canceled_at DATETIME DEFAULT NULL COMMENT '用户撤销时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_after_sale_no (after_sale_no),
    UNIQUE KEY uk_after_sale_active_order (active_order_no),
    KEY idx_after_sale_user (user_id, created_at),
    KEY idx_after_sale_order (order_no, created_at),
    KEY idx_after_sale_status (status, created_at),
    CONSTRAINT chk_after_sale_type CHECK (type IN (1, 2)),
    CONSTRAINT chk_after_sale_status CHECK (status IN (1, 2, 3, 4, 5, 6, 7)),
    CONSTRAINT chk_after_sale_original_status CHECK (original_order_status IN (2, 3, 4, 5)),
    CONSTRAINT chk_after_sale_amount CHECK (refund_amount >= 0),
    CONSTRAINT chk_after_sale_restock CHECK (restock IN (0, 1)),
    CONSTRAINT chk_after_sale_active_order CHECK (
        (status IN (1, 2, 3, 4, 5) AND active_order_no IS NOT NULL AND active_order_no = order_no)
        OR (status IN (6, 7) AND active_order_no IS NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='整单售后申请';

CREATE TABLE IF NOT EXISTS after_sale_log (
    id BIGINT NOT NULL COMMENT '售后流水主键',
    after_sale_no VARCHAR(64) NOT NULL COMMENT '售后单号',
    before_status TINYINT DEFAULT NULL COMMENT '操作前状态，创建申请时为空',
    after_status TINYINT NOT NULL COMMENT '操作后状态，含义与售后主表一致',
    operation_type TINYINT NOT NULL COMMENT '操作类型：1申请，2通过，3驳回，4寄回，5验收，6退款，7撤销',
    operator_id BIGINT NOT NULL COMMENT '操作人ID',
    remark VARCHAR(500) DEFAULT NULL COMMENT '申请、审核或处理说明',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (id),
    KEY idx_after_sale_log_no (after_sale_no, id),
    CONSTRAINT chk_after_sale_log_type CHECK (operation_type IN (1, 2, 3, 4, 5, 6, 7))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='售后操作审计流水';
