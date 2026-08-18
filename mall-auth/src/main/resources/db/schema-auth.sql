-- MallX 认证与授权模块建表脚本（MySQL 8）。

CREATE TABLE IF NOT EXISTS auth_account (
    id BIGINT NOT NULL COMMENT '用户身份主键',
    phone VARCHAR(20) NOT NULL COMMENT '唯一登录手机号',
    password_hash VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码摘要',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '账号状态：1 启用，0 禁用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_auth_account_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='认证账号';

CREATE TABLE IF NOT EXISTS auth_role (
    id BIGINT NOT NULL COMMENT '角色主键',
    role_code VARCHAR(64) NOT NULL COMMENT '唯一角色编码',
    role_name VARCHAR(64) NOT NULL COMMENT '角色名称',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1 启用，0 禁用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_auth_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='认证角色';

CREATE TABLE IF NOT EXISTS auth_permission (
    id BIGINT NOT NULL COMMENT '权限主键',
    permission_code VARCHAR(128) NOT NULL COMMENT '唯一权限编码',
    permission_name VARCHAR(64) NOT NULL COMMENT '权限名称',
    resource VARCHAR(255) DEFAULT NULL COMMENT '资源路径',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_auth_permission_code (permission_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='认证权限';

CREATE TABLE IF NOT EXISTS auth_account_role (
    account_id BIGINT NOT NULL COMMENT '账号主键',
    role_id BIGINT NOT NULL COMMENT '角色主键',
    PRIMARY KEY (account_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账号角色关联';

CREATE TABLE IF NOT EXISTS auth_role_permission (
    role_id BIGINT NOT NULL COMMENT '角色主键',
    permission_id BIGINT NOT NULL COMMENT '权限主键',
    PRIMARY KEY (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联';
