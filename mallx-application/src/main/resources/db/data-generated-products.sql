-- MallX 商城展示商品初始化数据（MySQL 8）。
-- 素材来源：_UI/storefront/public/generated-products，共10个虚构品牌、10个SPU、30个SKU。
-- 图片已通过后台上传接口保存至阿里云OSS，本脚本保存公开HTTPS访问地址。
-- 固定ID：分类20001~20005、品牌21001~21010、SPU 22001~22010、SKU 23001~23030。

SET NAMES utf8mb4;
START TRANSACTION;

INSERT INTO product_category (
    id, parent_id, category_name, category_level, sort, icon_oss_key, enabled
) VALUES
    (20001, 0, '厨房与饮水', 1, 1, 'https://lpd-web.oss-cn-beijing.aliyuncs.com/2026/09/2d48047c82fe42648cee0a27bfd5e496.png', 1),
    (20002, 0, '数码影音', 1, 2, 'https://lpd-web.oss-cn-beijing.aliyuncs.com/2026/09/244024529b0c4f6b80baf0af56c7b761.png', 1),
    (20003, 0, '健康家居', 1, 3, 'https://lpd-web.oss-cn-beijing.aliyuncs.com/2026/09/eb36303daf1f41ba89bd0cd9dac2e43f.png', 1),
    (20004, 0, '户外出行', 1, 4, 'https://lpd-web.oss-cn-beijing.aliyuncs.com/2026/09/6fe235aba80146958b70e08e8eec02aa.png', 1),
    (20005, 0, '美妆护肤', 1, 5, 'https://lpd-web.oss-cn-beijing.aliyuncs.com/2026/09/2b77a0d3426c4378ba9bf54f5430417c.png', 1)
    AS new
ON DUPLICATE KEY UPDATE
    parent_id = new.parent_id,
    category_name = new.category_name,
    category_level = new.category_level,
    sort = new.sort,
    icon_oss_key = new.icon_oss_key,
    enabled = new.enabled;

INSERT INTO product_brand (
    id, brand_name, logo_oss_key, description, sort, enabled
) VALUES
    (21001, 'AURORA BREW', 'https://lpd-web.oss-cn-beijing.aliyuncs.com/2026/09/8c04252693704861bfd0f65ff8d8c87d.png', '专注家庭精品咖啡体验的虚构生活电器品牌。', 1, 1),
    (21002, 'LUMASOUND', 'https://lpd-web.oss-cn-beijing.aliyuncs.com/2026/09/f3c82a249b4f41f9a124b0ec1c23dc34.png', '专注沉浸式声音与舒适佩戴体验的虚构音频品牌。', 2, 1),
    (21003, 'VERDANT AIR', 'https://lpd-web.oss-cn-beijing.aliyuncs.com/2026/09/7c68f178105f4c7cab0f925bfedf2912.png', '以自然香氛和舒缓空间体验为灵感的虚构家居品牌。', 3, 1),
    (21004, 'NORTHPEAK', 'https://lpd-web.oss-cn-beijing.aliyuncs.com/2026/09/66e64ecaa38043d38c363702a0d0e979.png', '面向城市通勤与轻户外场景的虚构装备品牌。', 4, 1),
    (21005, 'SOLARA', 'https://lpd-web.oss-cn-beijing.aliyuncs.com/2026/09/df3bd26deb5c44d686f7627d1b1b441e.png', '强调舒适光线和简洁设计的虚构照明品牌。', 5, 1),
    (21006, 'TERRASIP', 'https://lpd-web.oss-cn-beijing.aliyuncs.com/2026/09/f31258452345488d8f0501875ca7dfe6.png', '提供耐用随行饮水器具的虚构生活方式品牌。', 6, 1),
    (21007, 'CLOUDREST', 'https://lpd-web.oss-cn-beijing.aliyuncs.com/2026/09/f0b15189f2b74608ac3e4045026bbcd5.png', '关注睡眠承托与居家舒适度的虚构寝具品牌。', 7, 1),
    (21008, 'PUREHAVEN', 'https://lpd-web.oss-cn-beijing.aliyuncs.com/2026/09/e07dc18d4ae44fef916faea9f78dd809.png', '专注小空间空气管理的虚构健康家电品牌。', 8, 1),
    (21009, 'NOVACHARGE', 'https://lpd-web.oss-cn-beijing.aliyuncs.com/2026/09/d7e661566d7b4dcb9626e61ec4f8bd84.png', '面向移动设备提供便携能源产品的虚构数码品牌。', 9, 1),
    (21010, 'KINDRED DEW', 'https://lpd-web.oss-cn-beijing.aliyuncs.com/2026/09/ceeddcf93c7e47b79425cc7d361c4315.png', '以植物水润护理为理念的虚构护肤品牌。', 10, 1)
    AS new
ON DUPLICATE KEY UPDATE
    brand_name = new.brand_name,
    logo_oss_key = new.logo_oss_key,
    description = new.description,
    sort = new.sort,
    enabled = new.enabled;

INSERT INTO product_spu (
    id, category_id, brand_id, product_name, subtitle, main_image_oss_key,
    description, min_price, max_price, status
) VALUES
    (22001, 20001, 21001, 'AB-01 自动意式咖啡机', '在家轻松制作醇厚意式与绵密奶咖', 'https://lpd-web.oss-cn-beijing.aliyuncs.com/2026/09/2d48047c82fe42648cee0a27bfd5e496.png', '紧凑机身集成咖啡萃取和蒸汽奶泡功能，适合家庭日常咖啡制作。', 2599.00, 2999.00, 1),
    (22002, 20002, 21002, 'LS-X1 头戴式降噪耳机', '轻量包耳设计，通勤与专注场景皆适用', 'https://lpd-web.oss-cn-beijing.aliyuncs.com/2026/09/244024529b0c4f6b80baf0af56c7b761.png', '支持主动降噪和环境声模式，柔软耳罩兼顾长时间佩戴舒适度。', 1299.00, 1399.00, 1),
    (22003, 20003, 21003, 'VA-Mist 超声波香薰机', '细腻水雾，为空间增添自然气息', 'https://lpd-web.oss-cn-beijing.aliyuncs.com/2026/09/113c2840d85b4dd98f501ab527388255.png', '采用超声波雾化方式，提供定时与柔光模式，适合卧室和工作空间。', 299.00, 399.00, 1),
    (22004, 20004, 21004, 'NP-22 城市户外背包', '通勤收纳与周末轻户外一包兼顾', 'https://lpd-web.oss-cn-beijing.aliyuncs.com/2026/09/6fe235aba80146958b70e08e8eec02aa.png', '分区收纳设计搭配舒适背负系统，兼顾日常通勤与短途出行。', 699.00, 829.00, 1),
    (22005, 20003, 21005, 'SL-Arc LED 护眼台灯', '均匀柔光，陪伴阅读与桌面工作', 'https://lpd-web.oss-cn-beijing.aliyuncs.com/2026/09/ee771d88ee554dba94014db38c67132c.png', '支持亮度与色温调节，弧形灯臂为桌面提供宽广均匀照明。', 599.00, 759.00, 1),
    (22006, 20001, 21006, 'TS-500 真空保温杯', '简洁耐用的全天候随行饮水搭档', 'https://lpd-web.oss-cn-beijing.aliyuncs.com/2026/09/a873d9e4e4874962a04f2093303a8a86.png', '双层真空杯体兼顾保温和保冷，杯盖密封，适合通勤及户外携带。', 199.00, 259.00, 1),
    (22007, 20003, 21007, 'CR-Wave 记忆棉颈椎枕', '分区承托，适配不同睡眠偏好', 'https://lpd-web.oss-cn-beijing.aliyuncs.com/2026/09/11189635d2b94f06b5ae7e380cfe33e2.png', '慢回弹记忆棉搭配波浪曲线设计，为颈部提供不同高度和支撑选择。', 359.00, 459.00, 1),
    (22008, 20003, 21008, 'PH-360 桌面空气净化器', '小巧机身，守护桌面与卧室空气', 'https://lpd-web.oss-cn-beijing.aliyuncs.com/2026/09/eb36303daf1f41ba89bd0cd9dac2e43f.png', '环形进风结构适合小空间使用，可按不同生活场景选择对应滤芯。', 899.00, 1199.00, 1),
    (22009, 20002, 21009, 'NC-10 磁吸充电宝', '随贴随充，轻装补充移动电量', 'https://lpd-web.oss-cn-beijing.aliyuncs.com/2026/09/f8839b445d9248ad9a3a2ccaa5a84b6a.png', '支持磁吸无线充电和有线输出，提供不同容量以覆盖通勤及旅行需求。', 199.00, 299.00, 1),
    (22010, 20005, 21010, '植物水润护肤系列', '从基础补水到丰润护理的组合方案', 'https://lpd-web.oss-cn-beijing.aliyuncs.com/2026/09/2b77a0d3426c4378ba9bf54f5430417c.png', '围绕清洁、补水和修护搭配不同套装，满足日常及进阶护理需求。', 399.00, 699.00, 1)
    AS new
ON DUPLICATE KEY UPDATE
    category_id = new.category_id,
    brand_id = new.brand_id,
    product_name = new.product_name,
    subtitle = new.subtitle,
    main_image_oss_key = new.main_image_oss_key,
    description = new.description,
    min_price = new.min_price,
    max_price = new.max_price,
    status = new.status;

INSERT INTO product_sku (
    id, spu_id, sku_code, sku_name, spec_json, image_oss_key,
    sale_price, market_price, status
) VALUES
    (23001, 22001, 'AB01-FOREST-STANDARD', 'AB-01 深林绿标准款', JSON_OBJECT('颜色', '深林绿', '版本', '标准款'), 'https://lpd-web.oss-cn-beijing.aliyuncs.com/2026/09/e54670b8672e4ccaba8f6dd690239c3a.png', 2599.00, 2899.00, 1),
    (23002, 22001, 'AB01-CREAM-STANDARD', 'AB-01 奶油白标准款', JSON_OBJECT('颜色', '奶油白', '版本', '标准款'), 'https://lpd-web.oss-cn-beijing.aliyuncs.com/2026/09/00452690bda8458284015bb45e8e87b4.png', 2699.00, 2999.00, 1),
    (23003, 22001, 'AB01-OBSIDIAN-MILK', 'AB-01 曜石黑奶泡套装', JSON_OBJECT('颜色', '曜石黑', '版本', '奶泡套装'), 'https://lpd-web.oss-cn-beijing.aliyuncs.com/2026/09/fe5e72cf954d42e78daf95a4d4af4100.png', 2999.00, 3399.00, 1),
    (23004, 22002, 'LSX1-MIDNIGHT-NAVY', 'LS-X1 午夜蓝', JSON_OBJECT('颜色', '午夜蓝'), 'https://lpd-web.oss-cn-beijing.aliyuncs.com/2026/09/3a071d95be8548abba669a9cb4520073.png', 1299.00, 1499.00, 1),
    (23005, 22002, 'LSX1-MIST-SILVER', 'LS-X1 雾银灰', JSON_OBJECT('颜色', '雾银灰'), 'https://lpd-web.oss-cn-beijing.aliyuncs.com/2026/09/df547d74dced4ad08e7cdfef4b9fd19d.png', 1399.00, 1599.00, 1),
    (23006, 22002, 'LSX1-MORNING-WHITE', 'LS-X1 晨雾白', JSON_OBJECT('颜色', '晨雾白'), 'https://lpd-web.oss-cn-beijing.aliyuncs.com/2026/09/2081cd0596f8497bb604c56348b59985.png', 1299.00, 1499.00, 1),
    (23007, 22003, 'VAMIST-SAGE-200', 'VA-Mist 鼠尾草绿 200ml', JSON_OBJECT('颜色', '鼠尾草绿', '容量', '200ml'), 'https://lpd-web.oss-cn-beijing.aliyuncs.com/2026/09/7155bb5bd7754d9bbb497a647acf3f75.png', 299.00, 359.00, 1),
    (23008, 22003, 'VAMIST-TERRACOTTA-200', 'VA-Mist 陶土粉 200ml', JSON_OBJECT('颜色', '陶土粉', '容量', '200ml'), 'https://lpd-web.oss-cn-beijing.aliyuncs.com/2026/09/5872c1b35f0544f8a45592835daad6a2.png', 319.00, 379.00, 1),
    (23009, 22003, 'VAMIST-BLUE-300', 'VA-Mist 雾灰蓝 300ml', JSON_OBJECT('颜色', '雾灰蓝', '容量', '300ml'), 'https://lpd-web.oss-cn-beijing.aliyuncs.com/2026/09/54a782b3a9d942fc9ee23b991d8ebdcc.png', 399.00, 469.00, 1),
    (23010, 22004, 'NP22-PINE-22L', 'NP-22 松林绿 22L', JSON_OBJECT('颜色', '松林绿', '容量', '22L'), 'https://lpd-web.oss-cn-beijing.aliyuncs.com/2026/09/5cebafb09d594facbff0d72daba7600c.png', 699.00, 799.00, 1),
    (23011, 22004, 'NP22-ROCK-22L', 'NP-22 岩石灰 22L', JSON_OBJECT('颜色', '岩石灰', '容量', '22L'), 'https://lpd-web.oss-cn-beijing.aliyuncs.com/2026/09/dc2086f25ea0494a9dab7904c3f01497.png', 729.00, 829.00, 1),
    (23012, 22004, 'NP22-NIGHT-28L', 'NP-22 夜行黑 28L', JSON_OBJECT('颜色', '夜行黑', '容量', '28L'), 'https://lpd-web.oss-cn-beijing.aliyuncs.com/2026/09/abe2fb7720be4cf7a1e397d8debe18a8.png', 829.00, 949.00, 1),
    (23013, 22005, 'SLARC-OBSIDIAN-40', 'SL-Arc 曜石黑 40cm', JSON_OBJECT('颜色', '曜石黑', '高度', '40cm'), 'https://lpd-web.oss-cn-beijing.aliyuncs.com/2026/09/426d302268cd42538c2712972c6e5f9c.png', 599.00, 699.00, 1),
    (23014, 22005, 'SLARC-SILVER-40', 'SL-Arc 月岩银 40cm', JSON_OBJECT('颜色', '月岩银', '高度', '40cm'), 'https://lpd-web.oss-cn-beijing.aliyuncs.com/2026/09/63e78644846048a09570cd63efe6000c.png', 629.00, 729.00, 1),
    (23015, 22005, 'SLARC-FOREST-50', 'SL-Arc 深林绿 50cm', JSON_OBJECT('颜色', '深林绿', '高度', '50cm'), 'https://lpd-web.oss-cn-beijing.aliyuncs.com/2026/09/32b79bd5c26d487bb2b9199cc0d338a6.png', 759.00, 899.00, 1),
    (23016, 22006, 'TS500-TERRACOTTA-500', 'TS-500 陶土橙 500ml', JSON_OBJECT('颜色', '陶土橙', '容量', '500ml'), 'https://lpd-web.oss-cn-beijing.aliyuncs.com/2026/09/e2470203337b4f4ebe2770d4f9f6cab6.png', 199.00, 239.00, 1),
    (23017, 22006, 'TS500-MOSS-500', 'TS-500 苔藓绿 500ml', JSON_OBJECT('颜色', '苔藓绿', '容量', '500ml'), 'https://lpd-web.oss-cn-beijing.aliyuncs.com/2026/09/ef8eefef3a97427cb772b48e493be707.png', 209.00, 249.00, 1),
    (23018, 22006, 'TS500-SAND-750', 'TS-500 沙岩白 750ml', JSON_OBJECT('颜色', '沙岩白', '容量', '750ml'), 'https://lpd-web.oss-cn-beijing.aliyuncs.com/2026/09/5ff820775c8243819f5d0a242d48865e.png', 259.00, 309.00, 1),
    (23019, 22007, 'CRWAVE-SOFT-LOW', 'CR-Wave 柔软低枕', JSON_OBJECT('软硬度', '柔软', '高度', '低枕'), 'https://lpd-web.oss-cn-beijing.aliyuncs.com/2026/09/48a0fb5fb65d4e299693011e8347f80a.png', 359.00, 429.00, 1),
    (23020, 22007, 'CRWAVE-SUPPORT-MID', 'CR-Wave 支撑中枕', JSON_OBJECT('软硬度', '适中', '高度', '中枕'), 'https://lpd-web.oss-cn-beijing.aliyuncs.com/2026/09/e6f4cf1a1e784298a4b08c5627466004.png', 399.00, 469.00, 1),
    (23021, 22007, 'CRWAVE-FIRM-HIGH', 'CR-Wave 强支撑高枕', JSON_OBJECT('软硬度', '强支撑', '高度', '高枕'), 'https://lpd-web.oss-cn-beijing.aliyuncs.com/2026/09/abb66f2ff014475fbcd9ceb27e54ddee.png', 459.00, 539.00, 1),
    (23022, 22008, 'PH360-WHITE-BASIC', 'PH-360 清新白基础滤芯', JSON_OBJECT('颜色', '清新白', '滤芯', '基础滤芯'), 'https://lpd-web.oss-cn-beijing.aliyuncs.com/2026/09/327a0b95827d40319d8ca03f1ec189c1.png', 899.00, 999.00, 1),
    (23023, 22008, 'PH360-GREEN-FORMALDEHYDE', 'PH-360 雾绿除醛滤芯', JSON_OBJECT('颜色', '雾绿', '滤芯', '除醛滤芯'), 'https://lpd-web.oss-cn-beijing.aliyuncs.com/2026/09/ef0845ef51724affb00373bfbbc5f740.png', 1099.00, 1249.00, 1),
    (23024, 22008, 'PH360-GRAPHITE-PET', 'PH-360 曜石灰宠物滤芯', JSON_OBJECT('颜色', '曜石灰', '滤芯', '宠物滤芯'), 'https://lpd-web.oss-cn-beijing.aliyuncs.com/2026/09/e2e4daf7a6894a59bbb8bfe80fbcd2db.png', 1199.00, 1399.00, 1),
    (23025, 22009, 'NC10-GRAPHITE-10000', 'NC-10 石墨黑 10000mAh', JSON_OBJECT('颜色', '石墨黑', '容量', '10000mAh'), 'https://lpd-web.oss-cn-beijing.aliyuncs.com/2026/09/7115700bfcd4445ea2d14e12580ed965.png', 199.00, 239.00, 1),
    (23026, 22009, 'NC10-SILVER-10000', 'NC-10 雾银 10000mAh', JSON_OBJECT('颜色', '雾银', '容量', '10000mAh'), 'https://lpd-web.oss-cn-beijing.aliyuncs.com/2026/09/b25ac03e5f7f41e18c556781bd6e73ad.png', 219.00, 259.00, 1),
    (23027, 22009, 'NC10-FOREST-20000', 'NC-10 森林绿 20000mAh', JSON_OBJECT('颜色', '森林绿', '容量', '20000mAh'), 'https://lpd-web.oss-cn-beijing.aliyuncs.com/2026/09/7b1bf303f58942deb484b55c8596348f.png', 299.00, 359.00, 1),
    (23028, 22010, 'KD-HYDRATION-TRIO', 'KINDRED DEW 水润基础三件套', JSON_OBJECT('套装', '水润基础三件套', '数量', '3件'), 'https://lpd-web.oss-cn-beijing.aliyuncs.com/2026/09/fb83d66406614f8aacd5c127dd6219ff.png', 399.00, 469.00, 1),
    (23029, 22010, 'KD-SERUM-DUO', 'KINDRED DEW 修护精华双瓶套', JSON_OBJECT('套装', '修护精华双瓶套', '数量', '2瓶'), 'https://lpd-web.oss-cn-beijing.aliyuncs.com/2026/09/fffd0db8476e43f59dfb82a2ec1c2d4e.png', 459.00, 539.00, 1),
    (23030, 22010, 'KD-RICH-FIVE', 'KINDRED DEW 丰润五件礼盒装', JSON_OBJECT('套装', '丰润五件礼盒装', '数量', '5件'), 'https://lpd-web.oss-cn-beijing.aliyuncs.com/2026/09/c66065eaf0b3491c9e78f0c1330aecbf.png', 699.00, 799.00, 1)
    AS new
ON DUPLICATE KEY UPDATE
    spu_id = new.spu_id,
    sku_code = new.sku_code,
    sku_name = new.sku_name,
    spec_json = new.spec_json,
    image_oss_key = new.image_oss_key,
    sale_price = new.sale_price,
    market_price = new.market_price,
    status = new.status;

-- 库存只在首次初始化时插入；重复执行不会覆盖锁定、已售或剩余库存。
INSERT IGNORE INTO inventory_stock (
    sku_id, available_stock, locked_stock, sold_stock
) VALUES
    (23001, 30, 0, 0), (23002, 35, 0, 0), (23003, 20, 0, 0),
    (23004, 80, 0, 0), (23005, 65, 0, 0), (23006, 90, 0, 0),
    (23007, 120, 0, 0), (23008, 100, 0, 0), (23009, 75, 0, 0),
    (23010, 60, 0, 0), (23011, 55, 0, 0), (23012, 40, 0, 0),
    (23013, 70, 0, 0), (23014, 65, 0, 0), (23015, 45, 0, 0),
    (23016, 150, 0, 0), (23017, 140, 0, 0), (23018, 100, 0, 0),
    (23019, 100, 0, 0), (23020, 90, 0, 0), (23021, 80, 0, 0),
    (23022, 60, 0, 0), (23023, 50, 0, 0), (23024, 45, 0, 0),
    (23025, 200, 0, 0), (23026, 180, 0, 0), (23027, 120, 0, 0),
    (23028, 100, 0, 0), (23029, 85, 0, 0), (23030, 50, 0, 0);

COMMIT;

-- 验证：应得到5个分类、10个品牌、10个SPU、30个SKU和30条库存。
SELECT 'categories' AS item, COUNT(*) AS count
FROM product_category WHERE id BETWEEN 20001 AND 20005
UNION ALL
SELECT 'brands', COUNT(*) FROM product_brand WHERE id BETWEEN 21001 AND 21010
UNION ALL
SELECT 'spus', COUNT(*) FROM product_spu WHERE id BETWEEN 22001 AND 22010
UNION ALL
SELECT 'skus', COUNT(*) FROM product_sku WHERE id BETWEEN 23001 AND 23030
UNION ALL
SELECT 'stocks', COUNT(*) FROM inventory_stock WHERE sku_id BETWEEN 23001 AND 23030;
