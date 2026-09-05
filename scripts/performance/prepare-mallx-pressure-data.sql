-- MallX MVP 压测数据（MySQL 8）。仅用于独立压测数据库，禁止在生产环境执行。
-- 创建500个账号与地址、1万个商品SPU、2万个商品SKU和一张库存100的优惠券；密码统一为123456。
-- 固定账号ID：30001~30500；手机号：18810000000~18810000499；地址ID：40001~40500。
-- 商品目录：分类51001~51050、品牌52001~52050、SPU 800001~810000、SKU 3000001~3020000。

START TRANSACTION;

CREATE TEMPORARY TABLE pressure_seq (n INT PRIMARY KEY) ENGINE=InnoDB;
INSERT INTO pressure_seq (n)
SELECT ones.n + tens.n * 10 + hundreds.n * 100 + thousands.n * 1000
FROM (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
      UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) ones
CROSS JOIN (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
            UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) tens
CROSS JOIN (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
            UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) hundreds
CROSS JOIN (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
            UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) thousands;

-- 清除上一轮压测交易数据，使本脚本可以在两轮对比测试之间重新执行。
CREATE TEMPORARY TABLE pressure_order (
    id BIGINT PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL,
    UNIQUE KEY uk_pressure_order_no (order_no)
) ENGINE=InnoDB;
INSERT INTO pressure_order (id, order_no)
SELECT id, order_no FROM mall_order WHERE user_id BETWEEN 30001 AND 30500;

CREATE TEMPORARY TABLE pressure_after_sale (after_sale_no VARCHAR(64) PRIMARY KEY) ENGINE=InnoDB;
INSERT INTO pressure_after_sale (after_sale_no)
SELECT after_sale_no FROM after_sale WHERE user_id BETWEEN 30001 AND 30500;

DELETE after_sale_log FROM after_sale_log
JOIN pressure_after_sale ON pressure_after_sale.after_sale_no = after_sale_log.after_sale_no;
DELETE payment_refund FROM payment_refund
LEFT JOIN pressure_order ON pressure_order.order_no = payment_refund.order_no
LEFT JOIN pressure_after_sale ON pressure_after_sale.after_sale_no = payment_refund.after_sale_no
WHERE payment_refund.user_id BETWEEN 30001 AND 30500
   OR pressure_order.id IS NOT NULL OR pressure_after_sale.after_sale_no IS NOT NULL;
DELETE after_sale FROM after_sale
JOIN pressure_after_sale ON pressure_after_sale.after_sale_no = after_sale.after_sale_no;
DELETE payment_record FROM payment_record
LEFT JOIN pressure_order ON pressure_order.order_no = payment_record.order_no
WHERE payment_record.user_id BETWEEN 30001 AND 30500 OR pressure_order.id IS NOT NULL;
DELETE order_operation_log FROM order_operation_log
JOIN pressure_order ON pressure_order.id = order_operation_log.order_id;
DELETE order_item FROM order_item JOIN pressure_order ON pressure_order.id = order_item.order_id;
DELETE mall_order FROM mall_order JOIN pressure_order ON pressure_order.id = mall_order.id;

DELETE FROM coupon_operation_log
WHERE user_id BETWEEN 30001 AND 30500 OR user_coupon_id IN (
    SELECT id FROM coupon_user WHERE template_id = 70001
);
DELETE FROM coupon_user WHERE user_id BETWEEN 30001 AND 30500 OR template_id = 70001;
DELETE FROM coupon_template WHERE id = 70001;

DELETE FROM inventory_operation
WHERE sku_id IN (60001, 60002) OR sku_id BETWEEN 3000001 AND 3020000;
DELETE FROM inventory_stock
WHERE sku_id IN (60001, 60002) OR sku_id BETWEEN 3000001 AND 3020000;

-- 清理上一轮批量商品目录；先删SKU，再删SPU，满足商品表外键约束。
DELETE FROM product_sku WHERE id BETWEEN 3000001 AND 3020000;
DELETE FROM product_spu WHERE id BETWEEN 800001 AND 810000;
DELETE FROM product_brand WHERE id BETWEEN 52001 AND 52050;
DELETE FROM product_category WHERE id BETWEEN 51001 AND 51050;

DELETE FROM auth_account_role WHERE account_id BETWEEN 30001 AND 30500;
DELETE FROM user_membership_log
WHERE user_id BETWEEN 30001 AND 30500 OR operator_id BETWEEN 30001 AND 30500;
DELETE FROM user_membership WHERE user_id BETWEEN 30001 AND 30500;
DELETE FROM user_address WHERE user_id BETWEEN 30001 AND 30500;
DELETE FROM user_profile WHERE user_id BETWEEN 30001 AND 30500;
DELETE FROM auth_account WHERE id BETWEEN 30001 AND 30500;

-- 密码摘要由项目同款BCryptPasswordEncoder生成，对应明文123456。
INSERT INTO auth_account (id, phone, password_hash, status)
SELECT 30001 + n,
       CONCAT('1881', LPAD(n, 7, '0')),
       '$2a$10$AthLfsQA8S8Ghc3wEX4rVeWslClhcDErb5jmYQSn6fUfQSWNRIbei',
       1
FROM pressure_seq
WHERE n < 500;

INSERT INTO user_profile (user_id, nickname, gender)
SELECT 30001 + n, CONCAT('pressure_', LPAD(n + 1, 3, '0')), 0
FROM pressure_seq
WHERE n < 500;

INSERT INTO user_address (
    id, user_id, recipient_name, recipient_mobile,
    province_code, province_name, city_code, city_name,
    district_code, district_name, detail_address,
    postal_code, address_tag, is_default, deleted
)
SELECT 40001 + n,
       30001 + n,
       CONCAT('压测用户', n + 1),
       CONCAT('1881', LPAD(n, 7, '0')),
       '110000', '北京市', '110100', '北京市',
       '110101', '东城区', CONCAT('压测地址', n + 1, '号'),
       '100000', '压测', 1, 0
FROM pressure_seq
WHERE n < 500;

INSERT INTO product_category (
    id, parent_id, category_name, category_level, sort, icon_oss_key, enabled
) VALUES (50001, 0, 'PRESSURE-MVP-分类', 1, 999, NULL, 1) AS new
ON DUPLICATE KEY UPDATE category_name = new.category_name, enabled = 1;

INSERT INTO product_brand (id, brand_name, logo_oss_key, description, sort, enabled)
VALUES (50001, 'PRESSURE-MVP-品牌', NULL, '仅用于MallX压力测试', 999, 1) AS new
ON DUPLICATE KEY UPDATE brand_name = new.brand_name, enabled = 1;

INSERT INTO product_spu (
    id, category_id, brand_id, product_name, subtitle, main_image_oss_key,
    description, min_price, max_price, status
) VALUES (
    50001, 50001, 50001, 'PRESSURE-MVP-商品', '压测专用，请勿用于生产', NULL,
    'MallX普通交易并发测试商品', 99.00, 199.00, 1
) AS new
ON DUPLICATE KEY UPDATE
    category_id = new.category_id, brand_id = new.brand_id,
    product_name = new.product_name, min_price = new.min_price,
    max_price = new.max_price, status = 1;

INSERT INTO product_sku (
    id, spu_id, sku_code, sku_name, spec_json, image_oss_key,
    sale_price, market_price, status
) VALUES
    (60001, 50001, 'PRESSURE-NORMAL', 'PRESSURE-MVP-普通库存SKU', '{"type":"normal"}', NULL, 99.00, 109.00, 1),
    (60002, 50001, 'PRESSURE-HOT', 'PRESSURE-MVP-热点库存100件SKU', '{"type":"hot"}', NULL, 199.00, 219.00, 1) AS new
ON DUPLICATE KEY UPDATE
    spu_id = new.spu_id, sku_name = new.sku_name,
    sale_price = new.sale_price, market_price = new.market_price, status = 1;

INSERT INTO inventory_stock (sku_id, available_stock, locked_stock, sold_stock)
VALUES (60001, 1000000, 0, 0),
       (60002, 100, 0, 0);

-- 商品列表容量数据：50个分类、50个品牌、1万个SPU，每个SPU包含两个SKU。
INSERT INTO product_category (
    id, parent_id, category_name, category_level, sort, icon_oss_key, enabled
)
SELECT 51001 + n,
       0,
       CONCAT('PRESSURE-CATALOG-分类-', LPAD(n + 1, 2, '0')),
       1,
       n,
       NULL,
       1
FROM pressure_seq
WHERE n < 50;

INSERT INTO product_brand (
    id, brand_name, logo_oss_key, description, sort, enabled
)
SELECT 52001 + n,
       CONCAT('PRESSURE-CATALOG-品牌-', LPAD(n + 1, 2, '0')),
       NULL,
       'MallX商品列表容量压测品牌',
       n,
       1
FROM pressure_seq
WHERE n < 50;

-- 状态分布：90%上架、5%草稿、5%下架；价格、分类、品牌和更新时间均有分布。
INSERT INTO product_spu (
    id, category_id, brand_id, product_name, subtitle, main_image_oss_key,
    description, min_price, max_price, status, created_at, updated_at
)
SELECT 800001 + n,
       51001 + MOD(n, 50),
       52001 + MOD(FLOOR(n / 50), 50),
       CONCAT('PRESSURE-CATALOG-商品-', LPAD(n + 1, 5, '0')),
       CONCAT('商品列表容量压测数据-', n + 1),
       NULL,
       'MallX商品列表容量压测专用数据，请勿用于生产环境',
       10.00 + MOD(n, 990),
       20.00 + MOD(n, 990),
       CASE WHEN MOD(n, 20) = 0 THEN 0
            WHEN MOD(n, 10) = 0 THEN 2
            ELSE 1 END,
       DATE_SUB(NOW(), INTERVAL MOD(n, 365) DAY),
       DATE_SUB(NOW(), INTERVAL n SECOND)
FROM pressure_seq;

INSERT INTO product_sku (
    id, spu_id, sku_code, sku_name, spec_json, image_oss_key,
    sale_price, market_price, status, created_at, updated_at
)
SELECT 3000001 + n * 2 + sku_variant.variant,
       800001 + n,
       CONCAT('PRESSURE-CATALOG-', LPAD(n + 1, 5, '0'), '-', sku_variant.variant + 1),
       CONCAT('PRESSURE-CATALOG-SKU-', LPAD(n + 1, 5, '0'), '-', sku_variant.variant + 1),
       JSON_OBJECT('version', IF(sku_variant.variant = 0, '标准版', '升级版')),
       NULL,
       10.00 + MOD(n, 990) + sku_variant.variant * 10.00,
       30.00 + MOD(n, 990) + sku_variant.variant * 10.00,
       CASE WHEN MOD(n, 20) = 0 THEN 0
            WHEN MOD(n, 10) = 0 THEN 2
            ELSE 1 END,
       DATE_SUB(NOW(), INTERVAL MOD(n, 365) DAY),
       DATE_SUB(NOW(), INTERVAL n SECOND)
FROM pressure_seq
CROSS JOIN (SELECT 0 AS variant UNION ALL SELECT 1) sku_variant;

INSERT INTO inventory_stock (sku_id, available_stock, locked_stock, sold_stock)
SELECT id, 1000 + MOD(id, 9000), 0, 0
FROM product_sku
WHERE id BETWEEN 3000001 AND 3020000;

INSERT INTO coupon_template (
    id, coupon_name, coupon_type, threshold_amount, discount_amount,
    discount_rate, max_discount_amount, total_stock, received_stock,
    claim_start_at, claim_end_at, valid_start_at, valid_end_at, status
) VALUES (
    70001, 'PRESSURE-MVP-限量100张', 1, 0.00, 10.00,
    NULL, NULL, 100, 0,
    DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 1 DAY),
    DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 7 DAY), 1
);

COMMIT;

SELECT COUNT(*) AS pressure_accounts FROM auth_account WHERE id BETWEEN 30001 AND 30500;
SELECT COUNT(*) AS pressure_addresses FROM user_address WHERE user_id BETWEEN 30001 AND 30500;
SELECT COUNT(*) AS pressure_categories FROM product_category WHERE id BETWEEN 51001 AND 51050;
SELECT COUNT(*) AS pressure_brands FROM product_brand WHERE id BETWEEN 52001 AND 52050;
SELECT status, COUNT(*) AS pressure_spu_count
FROM product_spu WHERE id BETWEEN 800001 AND 810000 GROUP BY status ORDER BY status;
SELECT COUNT(*) AS pressure_skus FROM product_sku WHERE id BETWEEN 3000001 AND 3020000;
SELECT COUNT(*) AS pressure_catalog_stocks FROM inventory_stock WHERE sku_id BETWEEN 3000001 AND 3020000;
SELECT sku_id, available_stock, locked_stock, sold_stock
FROM inventory_stock WHERE sku_id IN (60001, 60002) ORDER BY sku_id;
SELECT id, total_stock, received_stock, status FROM coupon_template WHERE id = 70001;

DROP TEMPORARY TABLE pressure_after_sale;
DROP TEMPORARY TABLE pressure_order;
DROP TEMPORARY TABLE pressure_seq;
