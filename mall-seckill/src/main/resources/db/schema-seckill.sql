-- MallX 秒杀模块建表脚本（MySQL 8）。正式订单与库存由对应模块管理，不建立跨模块外键。
CREATE TABLE IF NOT EXISTS seckill_activity (
    id BIGINT NOT NULL COMMENT '秒杀活动主键',
    activity_name VARCHAR(100) NOT NULL COMMENT '活动名称',
    start_at DATETIME NOT NULL COMMENT '开始时间',
    end_at DATETIME NOT NULL COMMENT '结束时间',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0草稿，1已预热，2已关闭',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    PRIMARY KEY (id),
    KEY idx_seckill_activity_status_time (status, start_at, end_at),
    CONSTRAINT chk_seckill_activity_status CHECK (status IN (0, 1, 2)),
    CONSTRAINT chk_seckill_activity_time CHECK (end_at > start_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀活动';

CREATE TABLE IF NOT EXISTS seckill_item (
    id BIGINT NOT NULL COMMENT '秒杀商品主键',
    activity_id BIGINT NOT NULL COMMENT '活动ID',
    sku_id BIGINT NOT NULL COMMENT 'SKU ID',
    seckill_price DECIMAL(12,2) NOT NULL COMMENT '秒杀成交单价',
    total_stock INT NOT NULL COMMENT '本场秒杀资格库存',
    sold_stock INT NOT NULL DEFAULT 0 COMMENT '已成功创建订单的数量',
    limit_per_user INT NOT NULL DEFAULT 1 COMMENT '每个用户限购数量',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用或预留已释放，1启用或待释放',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_seckill_item_activity_sku (activity_id, sku_id),
    KEY idx_seckill_item_sku (sku_id),
    CONSTRAINT chk_seckill_item_status CHECK (status IN (0, 1)),
    CONSTRAINT chk_seckill_item_values CHECK (
        seckill_price > 0 AND total_stock > 0 AND sold_stock >= 0
        AND sold_stock <= total_stock AND limit_per_user > 0
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀活动商品';

CREATE TABLE IF NOT EXISTS seckill_order_request (
    id BIGINT NOT NULL COMMENT '秒杀请求主键',
    request_no VARCHAR(64) NOT NULL COMMENT '客户端查询使用的请求号',
    activity_id BIGINT NOT NULL COMMENT '活动ID',
    item_id BIGINT NOT NULL COMMENT '秒杀商品ID',
    sku_id BIGINT NOT NULL COMMENT 'SKU ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    address_id BIGINT NOT NULL COMMENT '下单地址ID',
    quantity INT NOT NULL COMMENT '购买数量',
    order_no VARCHAR(64) DEFAULT NULL COMMENT '成功后生成的正式订单号',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0处理中，1成功，2失败',
    fail_reason VARCHAR(200) DEFAULT NULL COMMENT '确定性业务失败原因',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_seckill_request_no (request_no),
    UNIQUE KEY uk_seckill_request_order_no (order_no),
    KEY idx_seckill_request_user (user_id, created_at),
    KEY idx_seckill_request_status (status, updated_at),
    CONSTRAINT chk_seckill_request_status CHECK (status IN (0, 1, 2)),
    CONSTRAINT chk_seckill_request_quantity CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀异步下单请求';
