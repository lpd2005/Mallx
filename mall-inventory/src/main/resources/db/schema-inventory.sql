-- MallX 库存中心建表脚本（MySQL 8）。sku_id与product_sku.id业务关联，不建立跨模块物理外键。

CREATE TABLE IF NOT EXISTS inventory_stock (
    sku_id BIGINT NOT NULL COMMENT 'SKU ID',
    available_stock INT NOT NULL DEFAULT 0 COMMENT '可用库存',
    locked_stock INT NOT NULL DEFAULT 0 COMMENT '已锁定、尚未最终扣减的库存',
    sold_stock INT NOT NULL DEFAULT 0 COMMENT '已完成扣减的累计库存',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (sku_id),
    CONSTRAINT chk_inventory_available_nonnegative CHECK (available_stock >= 0),
    CONSTRAINT chk_inventory_locked_nonnegative CHECK (locked_stock >= 0),
    CONSTRAINT chk_inventory_sold_nonnegative CHECK (sold_stock >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SKU库存';

CREATE TABLE IF NOT EXISTS inventory_operation (
    id BIGINT NOT NULL COMMENT '库存操作流水ID',
    business_no VARCHAR(64) NOT NULL COMMENT '业务单号，例如订单号或入库单号',
    sku_id BIGINT NOT NULL COMMENT 'SKU ID',
    operation_type TINYINT NOT NULL COMMENT '操作类型：1增加可用，2锁定，3扣减，4释放，5售后回补',
    quantity INT NOT NULL COMMENT '操作数量，必须大于0',
    before_available INT DEFAULT NULL COMMENT '操作前可用库存',
    after_available INT DEFAULT NULL COMMENT '操作后可用库存',
    before_locked INT DEFAULT NULL COMMENT '操作前锁定库存',
    after_locked INT DEFAULT NULL COMMENT '操作后锁定库存',
    before_sold INT DEFAULT NULL COMMENT '操作前已售库存',
    after_sold INT DEFAULT NULL COMMENT '操作后已售库存',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '流水状态：0处理中，1成功',
    remark VARCHAR(255) DEFAULT NULL COMMENT '操作说明',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_inventory_operation_biz (business_no, sku_id, operation_type),
    KEY idx_inventory_operation_sku (sku_id, created_at),
    CONSTRAINT chk_inventory_operation_quantity CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存操作流水及幂等记录';
