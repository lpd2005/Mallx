-- 可选统计索引升级，已有数据库执行一次；重复执行会提示索引已存在。
-- 本脚本仅添加索引，不更改业务数据。数据量大时请安排维护窗口。
ALTER TABLE auth_account ADD INDEX idx_admin_account_created (created_at);
ALTER TABLE mall_order ADD INDEX idx_admin_order_created (created_at);
ALTER TABLE mall_order ADD INDEX idx_admin_order_paid (paid_at);
ALTER TABLE payment_refund ADD INDEX idx_admin_refund_time (refunded_at);
