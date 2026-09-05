-- MallX 2026-09-04后端验收数据清理脚本（MySQL 8）。
-- 已按当前数据库核对：相关订单均已退款或取消、售后已完成、锁定库存为0，可以直接清理。
-- 只匹配验收用户ID、业务记录及商品固定ID/名称，不影响新建的admin、user1、user2。
-- 执行前仍建议备份mall数据库；不要在生产环境执行测试数据清理脚本。

START TRANSACTION;

-- 使用临时表冻结本次清理范围，避免删除过程中子查询范围发生变化。
CREATE TEMPORARY TABLE cleanup_acceptance_user (
    id BIGINT PRIMARY KEY
) ENGINE=InnoDB;

INSERT INTO cleanup_acceptance_user (id)
VALUES (2095844983025197058),
       (2095845061920055298),
       (2095845063002185730),
       (2095845063488724993);

CREATE TEMPORARY TABLE cleanup_acceptance_order (
    id BIGINT PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL,
    UNIQUE KEY uk_cleanup_order_no (order_no)
) ENGINE=InnoDB;

INSERT INTO cleanup_acceptance_order (id, order_no)
SELECT mall_order.id, mall_order.order_no
FROM mall_order
JOIN cleanup_acceptance_user ON cleanup_acceptance_user.id = mall_order.user_id;

CREATE TEMPORARY TABLE cleanup_acceptance_after_sale (
    after_sale_no VARCHAR(64) PRIMARY KEY
) ENGINE=InnoDB;

INSERT INTO cleanup_acceptance_after_sale (after_sale_no)
SELECT DISTINCT after_sale.after_sale_no
FROM after_sale
LEFT JOIN cleanup_acceptance_user ON cleanup_acceptance_user.id = after_sale.user_id
LEFT JOIN cleanup_acceptance_order ON cleanup_acceptance_order.order_no = after_sale.order_no
WHERE cleanup_acceptance_user.id IS NOT NULL
   OR cleanup_acceptance_order.id IS NOT NULL;

CREATE TEMPORARY TABLE cleanup_acceptance_coupon (
    id BIGINT PRIMARY KEY
) ENGINE=InnoDB;

INSERT INTO cleanup_acceptance_coupon (id)
SELECT DISTINCT coupon_user.id
FROM coupon_user
LEFT JOIN cleanup_acceptance_user ON cleanup_acceptance_user.id = coupon_user.user_id
WHERE cleanup_acceptance_user.id IS NOT NULL
   OR coupon_user.template_id IN (2095845612602810369, 2095846195669786625);

CREATE TEMPORARY TABLE cleanup_acceptance_sku (
    id BIGINT PRIMARY KEY
) ENGINE=InnoDB;

-- 同时检查固定ID、所属SPU和测试名称，降低ID被复用时误删正常商品的风险。
INSERT INTO cleanup_acceptance_sku (id)
SELECT product_sku.id
FROM product_sku
WHERE product_sku.id IN (2095845067087437825, 2095845614226006018)
  AND product_sku.spu_id = 2095845066894499842
  AND product_sku.sku_name LIKE 'AT-20260904-200256%'
  AND NOT EXISTS (
      SELECT 1
      FROM order_item
      LEFT JOIN cleanup_acceptance_order
             ON cleanup_acceptance_order.id = order_item.order_id
      WHERE order_item.sku_id = product_sku.id
        AND cleanup_acceptance_order.id IS NULL
  );

-- 先删除售后、退款、支付的子流水。
DELETE after_sale_log
FROM after_sale_log
LEFT JOIN cleanup_acceptance_after_sale
       ON cleanup_acceptance_after_sale.after_sale_no = after_sale_log.after_sale_no
LEFT JOIN cleanup_acceptance_user
       ON cleanup_acceptance_user.id = after_sale_log.operator_id
WHERE cleanup_acceptance_after_sale.after_sale_no IS NOT NULL
   OR cleanup_acceptance_user.id IS NOT NULL;

DELETE payment_refund
FROM payment_refund
LEFT JOIN cleanup_acceptance_user
       ON cleanup_acceptance_user.id = payment_refund.user_id
LEFT JOIN cleanup_acceptance_order
       ON cleanup_acceptance_order.order_no = payment_refund.order_no
LEFT JOIN cleanup_acceptance_after_sale
       ON cleanup_acceptance_after_sale.after_sale_no = payment_refund.after_sale_no
WHERE cleanup_acceptance_user.id IS NOT NULL
   OR cleanup_acceptance_order.id IS NOT NULL
   OR cleanup_acceptance_after_sale.after_sale_no IS NOT NULL;

DELETE after_sale
FROM after_sale
JOIN cleanup_acceptance_after_sale
  ON cleanup_acceptance_after_sale.after_sale_no = after_sale.after_sale_no;

DELETE payment_record
FROM payment_record
LEFT JOIN cleanup_acceptance_user
       ON cleanup_acceptance_user.id = payment_record.user_id
LEFT JOIN cleanup_acceptance_order
       ON cleanup_acceptance_order.order_no = payment_record.order_no
WHERE cleanup_acceptance_user.id IS NOT NULL
   OR cleanup_acceptance_order.id IS NOT NULL;

-- 删除订单流水、明细和主表。
DELETE order_operation_log
FROM order_operation_log
LEFT JOIN cleanup_acceptance_order
       ON cleanup_acceptance_order.id = order_operation_log.order_id
LEFT JOIN cleanup_acceptance_user
       ON cleanup_acceptance_user.id = order_operation_log.operator_id
WHERE cleanup_acceptance_order.id IS NOT NULL
   OR cleanup_acceptance_user.id IS NOT NULL;

DELETE order_item
FROM order_item
JOIN cleanup_acceptance_order
  ON cleanup_acceptance_order.id = order_item.order_id;

DELETE mall_order
FROM mall_order
JOIN cleanup_acceptance_order
  ON cleanup_acceptance_order.id = mall_order.id;

-- 删除测试用户券、对应操作流水以及两个测试模板。
DELETE coupon_operation_log
FROM coupon_operation_log
LEFT JOIN cleanup_acceptance_coupon
       ON cleanup_acceptance_coupon.id = coupon_operation_log.user_coupon_id
LEFT JOIN cleanup_acceptance_user
       ON cleanup_acceptance_user.id = coupon_operation_log.user_id
LEFT JOIN cleanup_acceptance_order
       ON cleanup_acceptance_order.order_no = coupon_operation_log.order_no
WHERE cleanup_acceptance_coupon.id IS NOT NULL
   OR cleanup_acceptance_user.id IS NOT NULL
   OR cleanup_acceptance_order.id IS NOT NULL;

DELETE coupon_user
FROM coupon_user
JOIN cleanup_acceptance_coupon
  ON cleanup_acceptance_coupon.id = coupon_user.id;

DELETE FROM coupon_template
WHERE id IN (2095845612602810369, 2095846195669786625);

-- 删除测试SKU的库存流水及库存，再按外键依赖删除SKU、SPU、品牌和分类。
DELETE inventory_operation
FROM inventory_operation
JOIN cleanup_acceptance_sku ON cleanup_acceptance_sku.id = inventory_operation.sku_id;

DELETE inventory_stock
FROM inventory_stock
JOIN cleanup_acceptance_sku ON cleanup_acceptance_sku.id = inventory_stock.sku_id;

DELETE product_sku
FROM product_sku
JOIN cleanup_acceptance_sku ON cleanup_acceptance_sku.id = product_sku.id;

DELETE FROM product_spu
WHERE id = 2095845066894499842
  AND product_name = 'AT-20260904-200256'
  AND NOT EXISTS (
      SELECT 1 FROM product_sku WHERE product_sku.spu_id = product_spu.id
  );

DELETE FROM product_brand
WHERE id = 2095845066743504898
  AND brand_name = 'AT-20260904-200256'
  AND NOT EXISTS (
      SELECT 1 FROM product_spu WHERE product_spu.brand_id = product_brand.id
  );

DELETE FROM product_category
WHERE id = 2095845066613481474
  AND category_name = 'AT-20260904-200256'
  AND NOT EXISTS (
      SELECT 1 FROM product_spu WHERE product_spu.category_id = product_category.id
  );

-- 删除旧测试管理员的后台操作日志以及可能重复执行账号脚本前留下的身份数据。
DELETE admin_operation_log
FROM admin_operation_log
JOIN cleanup_acceptance_user ON cleanup_acceptance_user.id = admin_operation_log.operator_id;

DELETE auth_account_role
FROM auth_account_role
JOIN cleanup_acceptance_user ON cleanup_acceptance_user.id = auth_account_role.account_id;

DELETE user_address
FROM user_address
JOIN cleanup_acceptance_user ON cleanup_acceptance_user.id = user_address.user_id;

DELETE FROM user_membership_log
WHERE user_id IN (
          2095844983025197058,
          2095845061920055298,
          2095845063002185730,
          2095845063488724993
      )
   OR operator_id IN (
          2095844983025197058,
          2095845061920055298,
          2095845063002185730,
          2095845063488724993
      );

DELETE user_membership
FROM user_membership
JOIN cleanup_acceptance_user ON cleanup_acceptance_user.id = user_membership.user_id;

DELETE user_profile
FROM user_profile
JOIN cleanup_acceptance_user ON cleanup_acceptance_user.id = user_profile.user_id;

DELETE auth_account
FROM auth_account
JOIN cleanup_acceptance_user ON cleanup_acceptance_user.id = auth_account.id;

COMMIT;

-- 应全部为0；如果商品记录不是0，说明保护条件发现了新的关联，脚本不会强行删除。
SELECT 'old_user_accounts' AS item, COUNT(*) AS remaining
FROM auth_account
WHERE id IN (2095844983025197058, 2095845061920055298, 2095845063002185730, 2095845063488724993)
UNION ALL
SELECT 'old_user_orders', COUNT(*)
FROM mall_order
WHERE user_id IN (2095844983025197058, 2095845061920055298, 2095845063002185730, 2095845063488724993)
UNION ALL
SELECT 'test_coupon_templates', COUNT(*)
FROM coupon_template
WHERE id IN (2095845612602810369, 2095846195669786625)
UNION ALL
SELECT 'test_skus', COUNT(*)
FROM product_sku
WHERE id IN (2095845067087437825, 2095845614226006018)
UNION ALL
SELECT 'test_spu', COUNT(*)
FROM product_spu
WHERE id = 2095845066894499842
UNION ALL
SELECT 'test_brand', COUNT(*)
FROM product_brand
WHERE id = 2095845066743504898
UNION ALL
SELECT 'test_category', COUNT(*)
FROM product_category
WHERE id = 2095845066613481474;

DROP TEMPORARY TABLE cleanup_acceptance_sku;
DROP TEMPORARY TABLE cleanup_acceptance_coupon;
DROP TEMPORARY TABLE cleanup_acceptance_after_sale;
DROP TEMPORARY TABLE cleanup_acceptance_order;
DROP TEMPORARY TABLE cleanup_acceptance_user;
