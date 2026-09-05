-- 图片字段改存完整公开URL。已有数据库执行此脚本；新库使用各模块schema即可。
-- 执行前备份相关表并选择MallX数据库。仅扩容及修改注释，不删除或自动改写历史值。
-- MySQL DDL会隐式提交，不能依赖事务回滚；历史Key转换请参阅docs中的OSS配置说明。

ALTER TABLE product_category MODIFY COLUMN icon_oss_key VARCHAR(1024) DEFAULT NULL COMMENT '分类图标完整访问URL（沿用OSS Key字段名）';
ALTER TABLE product_brand MODIFY COLUMN logo_oss_key VARCHAR(1024) DEFAULT NULL COMMENT '品牌Logo完整访问URL（沿用OSS Key字段名）';
ALTER TABLE product_spu MODIFY COLUMN main_image_oss_key VARCHAR(1024) DEFAULT NULL COMMENT '主图完整访问URL（沿用OSS Key字段名）';
ALTER TABLE product_sku MODIFY COLUMN image_oss_key VARCHAR(1024) DEFAULT NULL COMMENT 'SKU图片完整访问URL（沿用OSS Key字段名）';
ALTER TABLE user_profile MODIFY COLUMN avatar_oss_key VARCHAR(1024) DEFAULT NULL COMMENT '头像完整访问URL（沿用OSS Key字段名）';
ALTER TABLE order_item MODIFY COLUMN image_oss_key VARCHAR(1024) DEFAULT NULL COMMENT '下单时商品图片完整访问URL快照（沿用OSS Key字段名）';
