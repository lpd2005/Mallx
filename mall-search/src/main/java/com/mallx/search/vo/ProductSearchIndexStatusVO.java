package com.mallx.search.vo;

import lombok.Builder;
import lombok.Data;

/** 商品搜索索引运行状态。 */
@Data
@Builder
public class ProductSearchIndexStatusVO {
    /** 对外使用的逻辑索引别名。 */
    private String alias;
    /** 当前别名是否已经指向可用物理索引。 */
    private Boolean available;
    /** 当前别名下的商品文档数量。 */
    private Long documentCount;
}
