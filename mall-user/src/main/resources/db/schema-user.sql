-- MallX 用户中心第一阶段建表脚本（MySQL 8）。
-- user_profile.user_id 与 auth_account.id 使用同一个身份主键；模块间不建立物理外键。

CREATE TABLE IF NOT EXISTS user_profile (
    user_id BIGINT NOT NULL COMMENT '用户ID，与 auth_account.id 一致',
    nickname VARCHAR(32) NOT NULL COMMENT '用户昵称',
    avatar_oss_key VARCHAR(255) DEFAULT NULL COMMENT '头像 OSS 对象 Key',
    gender TINYINT NOT NULL DEFAULT 0 COMMENT '性别：0 未知，1 男，2 女',
    birthday DATE DEFAULT NULL COMMENT '生日',
    bio VARCHAR(255) DEFAULT NULL COMMENT '个人简介',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户基本资料';

CREATE TABLE IF NOT EXISTS user_address (
    id BIGINT NOT NULL COMMENT '地址主键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    recipient_name VARCHAR(50) NOT NULL COMMENT '收货人姓名',
    recipient_mobile VARCHAR(20) NOT NULL COMMENT '收货人手机号',
    province_code VARCHAR(12) NOT NULL COMMENT '省级行政区编码',
    province_name VARCHAR(32) NOT NULL COMMENT '省级行政区名称',
    city_code VARCHAR(12) NOT NULL COMMENT '市级行政区编码',
    city_name VARCHAR(32) NOT NULL COMMENT '市级行政区名称',
    district_code VARCHAR(12) NOT NULL COMMENT '区县行政区编码',
    district_name VARCHAR(32) NOT NULL COMMENT '区县行政区名称',
    detail_address VARCHAR(255) NOT NULL COMMENT '详细地址',
    postal_code VARCHAR(12) DEFAULT NULL COMMENT '邮政编码',
    address_tag VARCHAR(20) DEFAULT NULL COMMENT '地址标签',
    is_default TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认：0 否，1 是',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 否，1 是',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_user_address_user (user_id, deleted, updated_at),
    KEY idx_user_address_default (user_id, is_default, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收货地址';

-- 会员等级只保存真正的两种会员；非会员是“没有有效会员”的计算状态，不单独占一条等级配置。
CREATE TABLE IF NOT EXISTS member_level (
    level_code TINYINT NOT NULL COMMENT '会员等级：1 普通会员，2 VIP会员',
    level_name VARCHAR(32) NOT NULL COMMENT '会员名称',
    discount_rate DECIMAL(5,4) NOT NULL COMMENT '商品折扣率：0.9500 表示95折',
    priority INT NOT NULL DEFAULT 0 COMMENT '等级优先级，数值越大等级越高',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否允许新开通：0 否，1 是',
    description VARCHAR(255) DEFAULT NULL COMMENT '会员权益说明',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (level_code),
    CONSTRAINT chk_member_level_discount CHECK (discount_rate > 0 AND discount_rate <= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员等级配置';

INSERT INTO member_level (level_code, level_name, discount_rate, priority, enabled, description)
VALUES (1, '普通会员', 0.9500, 10, 1, '商品享受95折'),
       (2, 'VIP会员', 0.8000, 20, 1, '商品享受8折')
ON DUPLICATE KEY UPDATE
    level_name = VALUES(level_name),
    discount_rate = VALUES(discount_rate),
    priority = VALUES(priority),
    description = VALUES(description);

-- 新用户不插入本表；不存在记录、已取消或已过期都表示非会员。
CREATE TABLE IF NOT EXISTS user_membership (
    user_id BIGINT NOT NULL COMMENT '用户ID，与 auth_account.id 一致',
    level_code TINYINT NOT NULL COMMENT '会员等级：1 普通会员，2 VIP会员',
    start_at DATETIME NOT NULL COMMENT '本次会员生效时间',
    expire_at DATETIME DEFAULT NULL COMMENT '到期时间，普通会员为空表示永久，VIP必须有值',
    status TINYINT NOT NULL COMMENT '状态：1 生效中，2 已过期，3 已取消',
    fallback_level_code TINYINT DEFAULT NULL COMMENT 'VIP到期后恢复的等级：1 普通会员；直接开通VIP时为空',
    fallback_start_at DATETIME DEFAULT NULL COMMENT '升级VIP前普通会员的生效时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    KEY idx_user_membership_expire (status, level_code, expire_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户当前会员状态';

CREATE TABLE IF NOT EXISTS user_membership_log (
    id BIGINT NOT NULL COMMENT '变更日志主键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    before_level_code TINYINT DEFAULT NULL COMMENT '变更前会员等级：1 普通会员，2 VIP会员，非会员为空',
    after_level_code TINYINT DEFAULT NULL COMMENT '变更后会员等级：1 普通会员，2 VIP会员，非会员为空',
    change_type TINYINT NOT NULL COMMENT '变更类型：1 开通，2 升级，3 调整，4 续期，5 取消，6 到期',
    before_expire_at DATETIME DEFAULT NULL COMMENT '变更前到期时间',
    after_expire_at DATETIME DEFAULT NULL COMMENT '变更后到期时间',
    operator_id BIGINT DEFAULT NULL COMMENT '操作人ID，系统自动到期时为空',
    remark VARCHAR(255) DEFAULT NULL COMMENT '变更说明',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_membership_log_user (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户会员变更日志';
