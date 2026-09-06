package com.mallx.product.cache;

/** 商品域缓存名称、缓存 Key 表达式和布隆过滤器 Key 的统一定义。 */
public final class ProductCacheDefinition {

    private static final String PRODUCT_PREFIX = "mallx:cache:v2:product:";

    /** JetCache 缓存名称；名称同时是 Redis Key 的前缀。 */
    public static final class Name {
        public static final String SPU_BY_ID = PRODUCT_PREFIX + "spu-by-id:";
        public static final String SKU_BY_ID = PRODUCT_PREFIX + "sku-by-id:";
        public static final String SKUS_BY_SPU_ID = PRODUCT_PREFIX + "skus-by-spu-id:";
        public static final String CATEGORY_TREE = PRODUCT_PREFIX + "category-tree:";
        public static final String ENABLED_BRANDS = PRODUCT_PREFIX + "enabled-brands:";
        public static final String HOME_PAGE = PRODUCT_PREFIX + "home-page:";

        private Name() {}
    }

    /** JetCache 注解使用的 SpEL Key；单引号包裹的是固定字符串 Key。 */
    public static final class Key {
        public static final String SPU_ID = "#spuId";
        public static final String SKU_ID = "#skuId";
        public static final String SPU_IDS = "#spuIds";
        public static final String SKU_IDS = "#skuIds";
        public static final String REQUEST_SPU_ID = "#request.spuId";
        public static final String ALL = "'all'";
        public static final String DEFAULT_HOME_PAGE = "'default'";

        private Key() {}
    }

    /** 仅缓存默认首页查询，其他筛选和分页查询直接访问数据库。 */
    public static final class Condition {
        public static final String DEFAULT_HOME_PAGE =
                "#query.current == 1 && #query.size == 20"
                        + " && #query.categoryId == null && #query.brandId == null"
                        + " && (#query.keyword == null || #query.keyword.trim().isEmpty())";

        private Condition() {}
    }

    /** Redisson 布隆过滤器物理名称；Redisson 会额外维护同名的 config Key。 */
    public static final class Bloom {
        public static final String EXISTING_SPU_IDS = PRODUCT_PREFIX + "bloom:existing-spu-ids";
        public static final String EXISTING_SKU_IDS = PRODUCT_PREFIX + "bloom:existing-sku-ids";

        private Bloom() {}
    }

    private ProductCacheDefinition() {}
}
