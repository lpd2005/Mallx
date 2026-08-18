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
