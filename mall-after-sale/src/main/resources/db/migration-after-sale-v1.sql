-- 仅用于已经执行过旧版订单建表脚本的数据库；执行一次，勿在新建库重复执行。
-- 本脚本不删除业务数据。MySQL DDL会隐式提交，执行前请备份并确认约束名称。
-- 完成升级后另执行schema-refund.sql和schema-after-sale.sql创建新表。
ALTER TABLE mall_order
    ADD COLUMN after_sale_no VARCHAR(64) DEFAULT NULL COMMENT '最近一次售后单号',
    ADD COLUMN status_before_after_sale TINYINT DEFAULT NULL COMMENT '进入售后前的订单状态',
    ADD COLUMN refunded_at DATETIME DEFAULT NULL COMMENT '整单退款完成时间',
    MODIFY COLUMN status TINYINT NOT NULL COMMENT '订单状态：1待支付，2已支付，3待发货，4已发货，5已完成，6已取消，7售后中，8已退款',
    DROP CHECK chk_order_status,
    ADD CONSTRAINT chk_order_status CHECK (status IN (1, 2, 3, 4, 5, 6, 7, 8));

ALTER TABLE order_operation_log
    MODIFY COLUMN operation_type TINYINT NOT NULL COMMENT '操作类型：1创建，2支付，3备货，4发货，5收货，6取消，7超时关闭，8申请售后，9退出售后，10退款',
    DROP CHECK chk_order_log_type,
    ADD CONSTRAINT chk_order_log_type CHECK (operation_type IN (1, 2, 3, 4, 5, 6, 7, 8, 9, 10));

ALTER TABLE inventory_operation
    MODIFY COLUMN operation_type TINYINT NOT NULL COMMENT '操作类型：1增加可用，2锁定，3扣减，4释放，5售后回补';
