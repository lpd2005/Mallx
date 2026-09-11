package com.mallx.search.service;

import com.mallx.common.result.PageResult;
import com.mallx.search.dto.ProductSearchQuery;
import com.mallx.search.vo.ProductSearchVO;

import java.util.List;

/** 前台商品检索能力。 */
public interface ProductSearchService {
    /** 按关键词、筛选条件和排序规则查询商品。 */
    PageResult<ProductSearchVO> search(ProductSearchQuery query);

    /** 返回商品名称联想词。 */
    List<String> suggestions(String keyword);

    /** 返回近期累计搜索次数最高的关键词。 */
    List<String> hotWords();
}
