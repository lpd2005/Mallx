-- MallX 后台模块（MySQL 8）。RBAC复用auth表，不重复创建管理员账号体系。
CREATE TABLE IF NOT EXISTS admin_operation_log (
    id BIGINT NOT NULL COMMENT '审计流水主键',
    operator_id BIGINT NOT NULL COMMENT '后台操作人账号ID',
    action VARCHAR(64) NOT NULL COMMENT '操作编码，如product:create-spu',
    target VARCHAR(128) NOT NULL COMMENT '对象主键或业务单号，不保存请求正文',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作成功时间',
    PRIMARY KEY (id),
    KEY idx_admin_log_operator (operator_id, created_at),
    KEY idx_admin_log_action (action, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='后台成功操作审计日志';
