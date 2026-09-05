-- MallX 普通订单模块建表脚本（MySQL 8）。跨模块ID只做业务关联，不建立物理外键。
CREATE TABLE IF NOT EXISTS mall_order (
    id BIGINT NOT NULL COMMENT '订单主键',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '下单用户ID',
    status TINYINT NOT NULL COMMENT '订单状态：1待支付，2已支付，3待发货，4已发货，5已完成，6已取消，7售后中，8已退款',
    after_sale_no VARCHAR(64) DEFAULT NULL COMMENT '最近一次售后单号',
    status_before_after_sale TINYINT DEFAULT NULL COMMENT '进入售后前的订单状态',
    refunded_at DATETIME DEFAULT NULL COMMENT '整单退款完成时间',
    total_quantity INT NOT NULL COMMENT '商品购买总件数',
    product_amount DECIMAL(12,2) NOT NULL COMMENT '商品原价合计',
    member_discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '会员优惠金额',
    coupon_discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '优惠券优惠金额',
    pay_amount DECIMAL(12,2) NOT NULL COMMENT '订单最终应付金额',
    coupon_user_id BIGINT DEFAULT NULL COMMENT '本单使用的用户优惠券ID',
    member_level_code TINYINT NOT NULL DEFAULT 0 COMMENT '下单时的会员等级快照：0非会员，1普通会员，2VIP会员',
    member_discount_rate DECIMAL(5,4) NOT NULL DEFAULT 1.0000 COMMENT '下单时的会员折扣率快照',
    recipient_name VARCHAR(64) NOT NULL COMMENT '收货人姓名快照',
    recipient_mobile VARCHAR(32) NOT NULL COMMENT '收货人手机号快照',
    province_name VARCHAR(64) NOT NULL COMMENT '省份名称快照',
    city_name VARCHAR(64) NOT NULL COMMENT '城市名称快照',
    district_name VARCHAR(64) NOT NULL COMMENT '区县名称快照',
    detail_address VARCHAR(255) NOT NULL COMMENT '详细收货地址快照',
    postal_code VARCHAR(16) DEFAULT NULL COMMENT '邮政编码快照',
    remark VARCHAR(255) DEFAULT NULL COMMENT '用户订单备注',
    expire_at DATETIME NOT NULL COMMENT '最晚支付时间',
    paid_at DATETIME DEFAULT NULL COMMENT '支付成功时间',
    shipped_at DATETIME DEFAULT NULL COMMENT '订单发货时间',
    completed_at DATETIME DEFAULT NULL COMMENT '订单完成时间',
    canceled_at DATETIME DEFAULT NULL COMMENT '订单取消或超时关闭时间',
    cancel_reason VARCHAR(255) DEFAULT NULL COMMENT '订单取消或超时关闭原因',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_order_user (user_id, created_at),
    KEY idx_order_status_expire (status, expire_at),
    CONSTRAINT chk_order_status CHECK (status IN (1, 2, 3, 4, 5, 6, 7, 8)),
    CONSTRAINT chk_order_amount CHECK (
        product_amount >= 0
        AND member_discount_amount >= 0
        AND coupon_discount_amount >= 0
        AND pay_amount >= 0
    ),
    CONSTRAINT chk_order_quantity CHECK (total_quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单主表';

CREATE TABLE IF NOT EXISTS order_item (
    id BIGINT NOT NULL COMMENT '订单明细主键',
    order_id BIGINT NOT NULL COMMENT '订单主键',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    spu_id BIGINT NOT NULL COMMENT 'SPU主键',
    sku_id BIGINT NOT NULL COMMENT 'SKU主键',
    product_name VARCHAR(255) NOT NULL COMMENT '下单时的商品名称快照',
    sku_name VARCHAR(255) NOT NULL COMMENT '下单时的SKU名称快照',
    spec_json JSON NOT NULL COMMENT '下单时的SKU规格JSON快照',
    image_oss_key VARCHAR(1024) DEFAULT NULL COMMENT '图片完整访问URL（沿用OSS Key字段名）',
    unit_price DECIMAL(12,2) NOT NULL COMMENT '商品成交单价',
    quantity INT NOT NULL COMMENT '购买数量',
    subtotal DECIMAL(12,2) NOT NULL COMMENT '明细成交小计',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_order_item_order (order_id),
    KEY idx_order_item_sku (sku_id),
    CONSTRAINT chk_order_item_quantity CHECK (quantity > 0),
    CONSTRAINT chk_order_item_amount CHECK (unit_price >= 0 AND subtotal >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单商品快照';

CREATE TABLE IF NOT EXISTS order_operation_log (
    id BIGINT NOT NULL COMMENT '订单操作流水主键',
    order_id BIGINT NOT NULL COMMENT '订单主键',
    order_no VARCHAR(64) NOT NULL COMMENT '订单号',
    before_status TINYINT DEFAULT NULL COMMENT '操作前订单状态，创建订单时为空',
    after_status TINYINT NOT NULL COMMENT '操作后订单状态',
    operation_type TINYINT NOT NULL COMMENT '操作类型：1创建，2支付，3备货，4发货，5收货，6取消，7超时关闭，8申请售后，9退出售后，10退款',
    operator_id BIGINT DEFAULT NULL COMMENT '操作人ID，系统任务操作时为空',
    remark VARCHAR(255) DEFAULT NULL COMMENT '操作说明',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_order_log_order (order_id, created_at),
    CONSTRAINT chk_order_log_type CHECK (operation_type IN (1, 2, 3, 4, 5, 6, 7, 8, 9, 10))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单状态流水';
