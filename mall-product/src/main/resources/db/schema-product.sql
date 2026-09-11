-- MallX 商品中心建表脚本（MySQL 8）。库存不属于商品表，由 mall-inventory 独立维护。

CREATE TABLE IF NOT EXISTS product_category (
    id BIGINT NOT NULL COMMENT '分类ID',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父分类ID，0表示根分类',
    category_name VARCHAR(50) NOT NULL COMMENT '分类名称',
    category_level TINYINT NOT NULL COMMENT '分类层级，从1开始',
    sort INT NOT NULL DEFAULT 0 COMMENT '排序值，越小越靠前',
    icon_oss_key VARCHAR(1024) DEFAULT NULL COMMENT '图片完整访问URL（沿用OSS Key字段名）',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：0否，1是',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_category_parent_name (parent_id, category_name),
    KEY idx_product_category_parent (parent_id, enabled, sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类';

CREATE TABLE IF NOT EXISTS product_brand (
    id BIGINT NOT NULL COMMENT '品牌ID',
    brand_name VARCHAR(80) NOT NULL COMMENT '品牌名称',
    logo_oss_key VARCHAR(1024) DEFAULT NULL COMMENT '图片完整访问URL（沿用OSS Key字段名）',
    description VARCHAR(500) DEFAULT NULL COMMENT '品牌简介',
    sort INT NOT NULL DEFAULT 0 COMMENT '排序值',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：0否，1是',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_brand_name (brand_name),
    KEY idx_product_brand_enabled (enabled, sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品品牌';

CREATE TABLE IF NOT EXISTS product_spu (
    id BIGINT NOT NULL COMMENT 'SPU ID',
    category_id BIGINT NOT NULL COMMENT '所属叶子分类ID',
    brand_id BIGINT NOT NULL COMMENT '品牌ID',
    product_name VARCHAR(120) NOT NULL COMMENT '商品名称',
    subtitle VARCHAR(255) DEFAULT NULL COMMENT '商品副标题',
    main_image_oss_key VARCHAR(1024) DEFAULT NULL COMMENT '图片完整访问URL（沿用OSS Key字段名）',
    description VARCHAR(1000) DEFAULT NULL COMMENT '商品描述信息',
    min_price DECIMAL(12,2) DEFAULT NULL COMMENT '在售SKU最低价',
    max_price DECIMAL(12,2) DEFAULT NULL COMMENT '在售SKU最高价',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0草稿，1上架，2下架',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_product_spu_category (category_id, status),
    KEY idx_product_spu_brand (brand_id, status),
    KEY idx_product_spu_status (status, updated_at),
    CONSTRAINT fk_product_spu_category FOREIGN KEY (category_id) REFERENCES product_category (id),
    CONSTRAINT fk_product_spu_brand FOREIGN KEY (brand_id) REFERENCES product_brand (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标准商品单元SPU';

CREATE TABLE IF NOT EXISTS product_sku (
    id BIGINT NOT NULL COMMENT 'SKU ID',
    spu_id BIGINT NOT NULL COMMENT '所属SPU ID',
    sku_code VARCHAR(64) NOT NULL COMMENT '业务SKU编码',
    sku_name VARCHAR(160) NOT NULL COMMENT 'SKU展示名称',
    spec_json JSON NOT NULL COMMENT '销售规格JSON，例如颜色、容量',
    image_oss_key VARCHAR(1024) DEFAULT NULL COMMENT '图片完整访问URL（沿用OSS Key字段名）',
    sale_price DECIMAL(12,2) NOT NULL COMMENT '销售价',
    market_price DECIMAL(12,2) DEFAULT NULL COMMENT '划线价',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0草稿，1上架，2下架',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_sku_code (sku_code),
    KEY idx_product_sku_spu (spu_id, status),
    CONSTRAINT fk_product_sku_spu FOREIGN KEY (spu_id) REFERENCES product_spu (id),
    CONSTRAINT chk_product_sku_sale_price CHECK (sale_price >= 0),
    CONSTRAINT chk_product_sku_market_price CHECK (market_price IS NULL OR market_price >= sale_price)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存量单位SKU';

CREATE TABLE IF NOT EXISTS product_search_outbox (
    id BIGINT NOT NULL COMMENT 'Outbox主键',
    event_id VARCHAR(64) NOT NULL COMMENT '消息事件ID',
    spu_id BIGINT NOT NULL COMMENT '需要重建索引的SPU ID',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0待发送，1已发送',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '投递次数',
    next_retry_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下次允许投递时间',
    last_error VARCHAR(500) DEFAULT NULL COMMENT '最近一次投递失败原因',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at DATETIME DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_search_outbox_event (event_id),
    KEY idx_product_search_outbox_pending (status, next_retry_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品搜索索引事件Outbox';
