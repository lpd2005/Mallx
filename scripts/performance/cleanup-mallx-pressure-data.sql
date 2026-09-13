-- MallX MVP 压测数据清理脚本（MySQL 8）。
-- 仅清理 prepare-mallx-pressure-data.sql 创建的固定压测数据。
-- 不会删除 admin(10001)、user1(10002)、user2(10003) 及普通业务商品。
-- 建议仅在本地或独立压测数据库执行；执行前请确认当前数据库为 mall。

START TRANSACTION;

-- 冻结本次压测用户范围。手机号规则同时作为保护条件，避免ID被其他账号复用时误删。
CREATE TEMPORARY TABLE cleanup_pressure_user (
    id BIGINT PRIMARY KEY
) ENGINE=InnoDB;

INSERT INTO cleanup_pressure_user (id)
SELECT id
FROM auth_account
WHERE id BETWEEN 30001 AND 30500
  AND phone = CONCAT('1881', LPAD(id - 30001, 7, '0'));

-- 即使某个压测账号已被手工删除，也要清理其遗留业务数据，因此业务表仍以预留用户ID段识别。
CREATE TEMPORARY TABLE cleanup_pressure_order (
    id BIGINT PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL,
    UNIQUE KEY uk_cleanup_pressure_order_no (order_no)
) ENGINE=InnoDB;

INSERT INTO cleanup_pressure_order (id, order_no)
SELECT id, order_no
FROM mall_order
WHERE user_id BETWEEN 30001 AND 30500;

CREATE TEMPORARY TABLE cleanup_pressure_after_sale (
    after_sale_no VARCHAR(64) PRIMARY KEY
) ENGINE=InnoDB;

INSERT INTO cleanup_pressure_after_sale (after_sale_no)
SELECT DISTINCT after_sale.after_sale_no
FROM after_sale
LEFT JOIN cleanup_pressure_order
       ON cleanup_pressure_order.order_no = after_sale.order_no
WHERE after_sale.user_id BETWEEN 30001 AND 30500
   OR cleanup_pressure_order.id IS NOT NULL;

CREATE TEMPORARY TABLE cleanup_pressure_coupon (
    id BIGINT PRIMARY KEY
) ENGINE=InnoDB;

INSERT INTO cleanup_pressure_coupon (id)
SELECT id
FROM coupon_user
WHERE user_id BETWEEN 30001 AND 30500
   OR template_id = 70001;

-- 通过固定ID和ASCII压测前缀双重确认商品范围。
-- 不使用中文名称做条件，避免Windows mysql source客户端字符集不同导致匹配失败。
CREATE TEMPORARY TABLE cleanup_pressure_spu (
    id BIGINT PRIMARY KEY
) ENGINE=InnoDB;

INSERT INTO cleanup_pressure_spu (id)
SELECT id
FROM product_spu
WHERE (id = 50001 AND product_name LIKE 'PRESSURE-MVP-%')
   OR (id BETWEEN 800001 AND 810000 AND product_name LIKE 'PRESSURE-CATALOG-%');

CREATE TEMPORARY TABLE cleanup_pressure_sku (
    id BIGINT PRIMARY KEY
) ENGINE=InnoDB;

INSERT INTO cleanup_pressure_sku (id)
SELECT product_sku.id
FROM product_sku
JOIN cleanup_pressure_spu ON cleanup_pressure_spu.id = product_sku.spu_id
WHERE (product_sku.id IN (60001, 60002)
       AND product_sku.sku_code IN ('PRESSURE-NORMAL', 'PRESSURE-HOT'))
   OR (product_sku.id BETWEEN 3000001 AND 3020000
       AND product_sku.sku_code LIKE 'PRESSURE-CATALOG-%');

-- 先删除售后日志、退款、售后单和支付记录。
DELETE after_sale_log
FROM after_sale_log
LEFT JOIN cleanup_pressure_after_sale
       ON cleanup_pressure_after_sale.after_sale_no = after_sale_log.after_sale_no
WHERE cleanup_pressure_after_sale.after_sale_no IS NOT NULL
   OR after_sale_log.operator_id BETWEEN 30001 AND 30500;

DELETE payment_refund
FROM payment_refund
LEFT JOIN cleanup_pressure_order
       ON cleanup_pressure_order.order_no = payment_refund.order_no
LEFT JOIN cleanup_pressure_after_sale
       ON cleanup_pressure_after_sale.after_sale_no = payment_refund.after_sale_no
WHERE payment_refund.user_id BETWEEN 30001 AND 30500
   OR cleanup_pressure_order.id IS NOT NULL
   OR cleanup_pressure_after_sale.after_sale_no IS NOT NULL;

DELETE after_sale
FROM after_sale
JOIN cleanup_pressure_after_sale
  ON cleanup_pressure_after_sale.after_sale_no = after_sale.after_sale_no;

DELETE payment_record
FROM payment_record
LEFT JOIN cleanup_pressure_order
       ON cleanup_pressure_order.order_no = payment_record.order_no
WHERE payment_record.user_id BETWEEN 30001 AND 30500
   OR cleanup_pressure_order.id IS NOT NULL;

-- 删除订单操作日志、订单明细和订单主表。
DELETE order_operation_log
FROM order_operation_log
LEFT JOIN cleanup_pressure_order
       ON cleanup_pressure_order.id = order_operation_log.order_id
WHERE cleanup_pressure_order.id IS NOT NULL
   OR order_operation_log.operator_id BETWEEN 30001 AND 30500;

DELETE order_item
FROM order_item
JOIN cleanup_pressure_order ON cleanup_pressure_order.id = order_item.order_id;

DELETE mall_order
FROM mall_order
JOIN cleanup_pressure_order ON cleanup_pressure_order.id = mall_order.id;

-- 删除压测用户的秒杀请求快照，并清理由专项压测创建的活动。
DELETE FROM seckill_order_request
WHERE user_id BETWEEN 30001 AND 30500;

DELETE seckill_item
FROM seckill_item
JOIN seckill_activity ON seckill_activity.id = seckill_item.activity_id
WHERE seckill_activity.activity_name LIKE 'PRESSURE-SECKILL-%';

DELETE FROM seckill_activity
WHERE activity_name LIKE 'PRESSURE-SECKILL-%';

-- 删除压测用户券、优惠券操作流水以及限量100张的压测模板。
DELETE coupon_operation_log
FROM coupon_operation_log
LEFT JOIN cleanup_pressure_coupon
       ON cleanup_pressure_coupon.id = coupon_operation_log.user_coupon_id
LEFT JOIN cleanup_pressure_order
       ON cleanup_pressure_order.order_no = coupon_operation_log.order_no
WHERE cleanup_pressure_coupon.id IS NOT NULL
   OR coupon_operation_log.user_id BETWEEN 30001 AND 30500
   OR cleanup_pressure_order.id IS NOT NULL;

DELETE coupon_user
FROM coupon_user
JOIN cleanup_pressure_coupon ON cleanup_pressure_coupon.id = coupon_user.id;

DELETE FROM coupon_template
WHERE id = 70001
  AND coupon_name LIKE 'PRESSURE-MVP-%';

-- 删除压测SKU对应的库存流水和库存，再按外键依赖删除商品数据。
DELETE inventory_operation
FROM inventory_operation
JOIN cleanup_pressure_sku ON cleanup_pressure_sku.id = inventory_operation.sku_id;

DELETE inventory_stock
FROM inventory_stock
JOIN cleanup_pressure_sku ON cleanup_pressure_sku.id = inventory_stock.sku_id;

-- Outbox 是商品搜索同步的历史投递记录，不受商品外键级联约束，需要随压测 SPU 一并清理。
DELETE product_search_outbox
FROM product_search_outbox
JOIN cleanup_pressure_spu ON cleanup_pressure_spu.id = product_search_outbox.spu_id;

DELETE product_sku
FROM product_sku
JOIN cleanup_pressure_sku ON cleanup_pressure_sku.id = product_sku.id;

DELETE product_spu
FROM product_spu
JOIN cleanup_pressure_spu ON cleanup_pressure_spu.id = product_spu.id
WHERE NOT EXISTS (
    SELECT 1 FROM product_sku WHERE product_sku.spu_id = product_spu.id
);

DELETE FROM product_brand
WHERE ((id = 50001 AND brand_name LIKE 'PRESSURE-MVP-%')
    OR (id BETWEEN 52001 AND 52050 AND brand_name LIKE 'PRESSURE-CATALOG-%'))
  AND NOT EXISTS (
      SELECT 1 FROM product_spu WHERE product_spu.brand_id = product_brand.id
  );

DELETE FROM product_category
WHERE ((id = 50001 AND category_name LIKE 'PRESSURE-MVP-%')
    OR (id BETWEEN 51001 AND 51050 AND category_name LIKE 'PRESSURE-CATALOG-%'))
  AND NOT EXISTS (
      SELECT 1 FROM product_spu WHERE product_spu.category_id = product_category.id
  );

-- 最后删除压测账号相关的后台日志、角色、会员、地址和账号资料。
-- 这里直接使用压测脚本预留的固定ID段，使清理脚本在账号已被部分删除后仍可重复执行。
-- 同时避免MySQL临时表在同一语句中被引用两次时出现 Can't reopen table。
DELETE FROM admin_operation_log
WHERE operator_id BETWEEN 30001 AND 30500;

DELETE FROM auth_account_role
WHERE account_id BETWEEN 30001 AND 30500;

DELETE FROM user_membership_log
WHERE user_id BETWEEN 30001 AND 30500
   OR operator_id BETWEEN 30001 AND 30500;

DELETE FROM user_membership
WHERE user_id BETWEEN 30001 AND 30500;

DELETE FROM user_address
WHERE user_id BETWEEN 30001 AND 30500;

DELETE FROM user_profile
WHERE user_id BETWEEN 30001 AND 30500;

DELETE auth_account
FROM auth_account
JOIN cleanup_pressure_user ON cleanup_pressure_user.id = auth_account.id;

COMMIT;

-- 验证结果：所有 remaining 都应为0。
-- 如果商品项不是0，说明双重保护条件或新的商品关联阻止了删除，请先人工核对，不要强删。
SELECT 'pressure_accounts' AS item, COUNT(*) AS remaining
FROM auth_account
WHERE id BETWEEN 30001 AND 30500
  AND phone LIKE '1881%'
UNION ALL
SELECT 'pressure_orders', COUNT(*)
FROM mall_order
WHERE user_id BETWEEN 30001 AND 30500
UNION ALL
SELECT 'pressure_payments', COUNT(*)
FROM payment_record
WHERE user_id BETWEEN 30001 AND 30500
UNION ALL
SELECT 'pressure_after_sales', COUNT(*)
FROM after_sale
WHERE user_id BETWEEN 30001 AND 30500
UNION ALL
SELECT 'pressure_user_coupons', COUNT(*)
FROM coupon_user
WHERE user_id BETWEEN 30001 AND 30500 OR template_id = 70001
UNION ALL
SELECT 'pressure_coupon_template', COUNT(*)
FROM coupon_template
WHERE id = 70001 AND coupon_name LIKE 'PRESSURE-MVP-%'
UNION ALL
SELECT 'pressure_inventory', COUNT(*)
FROM inventory_stock
WHERE sku_id IN (60001, 60002) OR sku_id BETWEEN 3000001 AND 3020000
UNION ALL
SELECT 'pressure_search_outbox', COUNT(*)
FROM product_search_outbox
WHERE spu_id = 50001 OR spu_id BETWEEN 800001 AND 810000
UNION ALL
SELECT 'pressure_skus', COUNT(*)
FROM product_sku
WHERE (id IN (60001, 60002) AND sku_code LIKE 'PRESSURE-%')
   OR (id BETWEEN 3000001 AND 3020000 AND sku_code LIKE 'PRESSURE-CATALOG-%')
UNION ALL
SELECT 'pressure_spus', COUNT(*)
FROM product_spu
WHERE (id = 50001 AND product_name LIKE 'PRESSURE-MVP-%')
   OR (id BETWEEN 800001 AND 810000 AND product_name LIKE 'PRESSURE-CATALOG-%')
UNION ALL
SELECT 'pressure_brands', COUNT(*)
FROM product_brand
WHERE (id = 50001 AND brand_name LIKE 'PRESSURE-MVP-%')
   OR (id BETWEEN 52001 AND 52050 AND brand_name LIKE 'PRESSURE-CATALOG-%')
UNION ALL
SELECT 'pressure_categories', COUNT(*)
FROM product_category
WHERE (id = 50001 AND category_name LIKE 'PRESSURE-MVP-%')
   OR (id BETWEEN 51001 AND 51050 AND category_name LIKE 'PRESSURE-CATALOG-%');

DROP TEMPORARY TABLE cleanup_pressure_sku;
DROP TEMPORARY TABLE cleanup_pressure_spu;
DROP TEMPORARY TABLE cleanup_pressure_coupon;
DROP TEMPORARY TABLE cleanup_pressure_after_sale;
DROP TEMPORARY TABLE cleanup_pressure_order;
DROP TEMPORARY TABLE cleanup_pressure_user;
