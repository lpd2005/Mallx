package com.mallx.search.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/** 商品搜索模块参数。 */
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "mallx.search")
public class ProductSearchProperties {
    /** 前台查询和增量写入始终使用的逻辑别名，而不是具体物理索引名。 */
    @NotBlank
    private String indexAlias = "product_index";

    /** 全量重建时每批从 MySQL 读取的 SPU 数量。 */
    @Min(1)
    private int rebuildBatchSize = 200;

    /** 热门搜索词接口最多返回的词条数量。 */
    @Min(1)
    private int hotWordLimit = 10;
}
