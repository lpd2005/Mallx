-- MallX 会员模块建表及初始化脚本（MySQL 8）。
-- 会员只有 NORMAL 和 VIP 两种；新用户没有会员记录，非会员不写入 member_level。
-- 模块化单体阶段不建立跨模块物理外键，user_id 与 auth_account.id 保持业务一致。

CREATE TABLE IF NOT EXISTS member_level (
    level_code TINYINT NOT NULL COMMENT '会员等级：1 普通会员，2 VIP会员',
    level_name VARCHAR(32) NOT NULL COMMENT '会员名称',
    discount_rate DECIMAL(5,4) NOT NULL COMMENT '商品折扣率：0.9500 表示95折，0.8000 表示8折',
    priority INT NOT NULL DEFAULT 0 COMMENT '等级优先级，数值越大等级越高',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否允许新开通：0 否，1 是',
    description VARCHAR(255) DEFAULT NULL COMMENT '会员权益说明',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (level_code),
    CONSTRAINT chk_member_level_discount CHECK (discount_rate > 0 AND discount_rate <= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员等级配置';

-- 重复执行脚本时更新既定权益，但不覆盖运营人员设置的 enabled 状态。
INSERT INTO member_level (level_code, level_name, discount_rate, priority, enabled, description)
VALUES (1, '普通会员', 0.9500, 10, 1, '商品享受95折'),
       (2, 'VIP会员', 0.8000, 20, 1, '商品享受8折')
ON DUPLICATE KEY UPDATE
    level_name = VALUES(level_name),
    discount_rate = VALUES(discount_rate),
    priority = VALUES(priority),
    description = VALUES(description);

-- 新用户不插入本表；没有记录、已取消或已过期均表示非会员。
CREATE TABLE IF NOT EXISTS user_membership (
    user_id BIGINT NOT NULL COMMENT '用户ID，与 auth_account.id 一致',
    level_code TINYINT NOT NULL COMMENT '会员等级：1 普通会员，2 VIP会员',
    start_at DATETIME NOT NULL COMMENT '本次会员生效时间',
    expire_at DATETIME DEFAULT NULL COMMENT '到期时间，普通会员为空表示永久，VIP必须设置',
    status TINYINT NOT NULL COMMENT '状态：1 生效中，2 已过期，3 已取消',
    fallback_level_code TINYINT DEFAULT NULL COMMENT 'VIP到期后恢复的等级：1 普通会员；直接开通VIP时为空',
    fallback_start_at DATETIME DEFAULT NULL COMMENT '升级VIP前普通会员的生效时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (user_id),
    KEY idx_user_membership_expire (status, level_code, expire_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户当前会员状态';

CREATE TABLE IF NOT EXISTS user_membership_log (
    id BIGINT NOT NULL COMMENT '会员变更日志主键，由MyBatis-Plus雪花算法生成',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    before_level_code TINYINT DEFAULT NULL COMMENT '变更前会员等级：1 普通会员，2 VIP会员，非会员为空',
    after_level_code TINYINT DEFAULT NULL COMMENT '变更后会员等级：1 普通会员，2 VIP会员，非会员为空',
    change_type TINYINT NOT NULL COMMENT '变更类型：1 开通，2 升级，3 调整，4 续期，5 取消，6 到期',
    before_expire_at DATETIME DEFAULT NULL COMMENT '变更前到期时间',
    after_expire_at DATETIME DEFAULT NULL COMMENT '变更后到期时间',
    operator_id BIGINT DEFAULT NULL COMMENT '操作人ID，系统自动到期时为空',
    remark VARCHAR(255) DEFAULT NULL COMMENT '变更说明',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_membership_log_user (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户会员变更日志';
