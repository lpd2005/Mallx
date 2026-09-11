package com.mallx.product.service;

import java.util.Collection;

/** 在商品或库存事务内记录需要重建搜索索引的 SPU。 */
public interface ProductSearchEventRecorder {
    void record(Long spuId);

    void recordAll(Collection<Long> spuIds);
}
