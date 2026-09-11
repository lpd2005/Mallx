package com.mallx.search.service.impl;

import cn.hutool.core.util.StrUtil;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.mallx.common.enums.ResultCode;
import com.mallx.common.exception.BusinessException;
import com.mallx.common.result.PageResult;
import com.mallx.search.config.ProductSearchProperties;
import com.mallx.search.document.ProductSearchDocument;
import com.mallx.search.dto.ProductSearchQuery;
import com.mallx.search.dto.ProductSearchSort;
import com.mallx.search.service.ProductSearchService;
import com.mallx.search.vo.ProductSearchVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** 使用 ElasticsearchOperations 构造固定查询，避免接受客户端原生 DSL。 */
@Service
@RequiredArgsConstructor
public class ProductSearchServiceImpl implements ProductSearchService {
    /** ZSet 的 member 是规范化后的关键词，score 是成功命中次数。 */
    private static final String HOT_WORD_KEY = "mallx:search:hot-words";

    /** 与 ES 默认 index.max_result_window 保持一致，深分页应另行使用 search_after。 */
    private static final int MAX_RESULT_WINDOW = 10_000;

    private final ElasticsearchOperations operations;
    private final ProductSearchIndexService indexService;
    private final ProductSearchProperties properties;
    private final StringRedisTemplate redisTemplate;

    /** 执行参数校验、索引检查、ES 查询，并只对有结果的关键词累计热度。 */
    @Override
    public PageResult<ProductSearchVO> search(ProductSearchQuery request) {
        validatePrice(request);
        validatePageWindow(request);
        indexService.ensureIndex();
        String keyword = StrUtil.trimToNull(request.getKeyword());
        NativeQuery query = buildQuery(request, keyword);
        SearchHits<ProductSearchDocument> hits = operations.search(query, ProductSearchDocument.class);
        // 无结果关键词不计入热词，避免大量随机输入污染排行榜。
        if (keyword != null && hits.hasSearchHits()) {
            redisTemplate.opsForZSet().incrementScore(HOT_WORD_KEY, keyword, 1D);
        }
        List<ProductSearchVO> records = hits.getSearchHits().stream().map(this::toVO).toList();
        return new PageResult<>(records, hits.getTotalHits(), request.getCurrent(), request.getSize());
    }

    /**
     * 使用 match_phrase_prefix 做轻量联想；LinkedHashSet 同时保持 ES 顺序并去重商品名。
     */
    @Override
    public List<String> suggestions(String input) {
        String keyword = StrUtil.trimToNull(input);
        if (keyword == null) {
            return List.of();
        }
        indexService.ensureIndex();
        NativeQuery query = NativeQuery.builder()
                .withQuery(root -> root.bool(bool -> bool
                        .should(should -> should.matchPhrasePrefix(prefix -> prefix
                                .field("productName").query(keyword).maxExpansions(20)))
                        .should(should -> should.matchPhrasePrefix(prefix -> prefix
                                .field("brandName").query(keyword).maxExpansions(20)))
                        .minimumShouldMatch("1")))
                .withPageable(PageRequest.of(0, 10))
                .build();
        SearchHits<ProductSearchDocument> hits = operations.search(query, ProductSearchDocument.class);
        Set<String> values = new LinkedHashSet<>();
        for (SearchHit<ProductSearchDocument> hit : hits) {
            values.add(hit.getContent().getProductName());
            if (values.size() == 10) {
                break;
            }
        }
        return new ArrayList<>(values);
    }

    /** ZSet 按 score 从高到低读取，Redis 中没有数据时返回空集合。 */
    @Override
    public List<String> hotWords() {
        Set<String> values = redisTemplate.opsForZSet().reverseRange(
                HOT_WORD_KEY, 0, properties.getHotWordLimit() - 1L);
        return values == null ? List.of() : new ArrayList<>(values);
    }

    /**
     * 组装搜索主查询：must 负责全文相关度，filter 负责不参与算分的精确条件，
     * 从而避免过滤条件干扰商品相关度排序。
     */
    private NativeQuery buildQuery(ProductSearchQuery request, String keyword) {
        List<Query> must = new ArrayList<>();
        List<Query> filters = new ArrayList<>();
        if (keyword == null) {
            // 没有关键词时仍允许用户按分类、品牌、价格和库存浏览商品。
            must.add(Query.of(query -> query.matchAll(match -> match)));
        } else {
            // SPU 字段和 nested SKU 字段属于不同文档作用域，需要分别构造查询后取并集。
            Query productKeywordQuery = Query.of(query -> query.multiMatch(multiMatch -> multiMatch
                    .query(keyword)
                    .fields("productName^5", "subtitle^3", "brandName^2", "categoryPathNames")
                    .operator(Operator.And))); // 默认是 or 运算符，这里改为 and 运算符
            Query skuKeywordQuery = Query.of(query -> query.nested(nested -> nested
                    .path("skus")
                    .query(nestedQuery -> nestedQuery.multiMatch(multiMatch -> multiMatch
                            .query(keyword)
                            .fields("skus.skuName^2", "skus.specText")
                            .operator(Operator.And)))));
            must.add(Query.of(query -> query.bool(bool -> bool
                    .should(productKeywordQuery, skuKeywordQuery) // 至少匹配一个
                    .minimumShouldMatch("1")))); // 至少匹配一个
        }
        if (request.getCategoryId() != null) {
            // categoryPathIds 包含完整祖先路径，因此传父分类也能命中其后代商品。
            filters.add(Query.of(query -> query.term(term -> term
                    .field("categoryPathIds").value(request.getCategoryId()))));
        }
        if (request.getBrandId() != null) {
            filters.add(Query.of(query -> query.term(term -> term
                    .field("brandId").value(request.getBrandId()))));
        }
        addSkuFilters(filters, request);
        if (Boolean.FALSE.equals(request.getInStock())) {
            // 查无货商品可直接使用 SPU 级预计算字段；有货与价格组合则必须进入 nested SKU。
            filters.add(Query.of(query -> query.term(term -> term
                    .field("hasStock").value(false))));
        }
        NativeQueryBuilder builder = NativeQuery.builder()
                .withQuery(query -> query.bool(bool -> bool.must(must).filter(filters)))
                .withPageable(PageRequest.of(request.getCurrent() - 1, request.getSize()));
        applySort(builder, request.getSort(), keyword != null);
        if (keyword != null) {
            // 当前列表只展示名称和副标题，所以仅请求这两个字段的高亮片段。
            builder.withHighlightQuery(new HighlightQuery(
                    new Highlight(List.of(
                            new HighlightField("productName"),
                            new HighlightField("subtitle"))),
                    ProductSearchDocument.class));
        }
        return builder.build();
    }

    /**
     * 应用客户端允许的固定排序规则，并始终追加 SPU ID 作为最终排序键，
     * 避免相同价格、销量或时间的数据在翻页时顺序漂移。
     */
    private void applySort(NativeQueryBuilder builder, ProductSearchSort sort, boolean hasKeyword) {
        ProductSearchSort actualSort = sort == null ? ProductSearchSort.RELEVANCE : sort;
        switch (actualSort) {
            case PRICE_ASC -> builder
                    .withSort(value -> value.field(field -> field.field("minPrice").order(SortOrder.Asc)))
                    .withSort(value -> value.field(field -> field.field("spuId").order(SortOrder.Desc)));
            case PRICE_DESC -> builder
                    .withSort(value -> value.field(field -> field.field("minPrice").order(SortOrder.Desc)))
                    .withSort(value -> value.field(field -> field.field("spuId").order(SortOrder.Desc)));
            case SALES_DESC -> builder
                    .withSort(value -> value.field(field -> field.field("soldCount").order(SortOrder.Desc)))
                    .withSort(value -> value.field(field -> field.field("spuId").order(SortOrder.Desc)));
            case NEWEST -> builder
                    .withSort(value -> value.field(field -> field.field("updatedAt").order(SortOrder.Desc)))
                    .withSort(value -> value.field(field -> field.field("spuId").order(SortOrder.Desc)));
            case RELEVANCE -> {
                if (hasKeyword) {
                    builder.withSort(value -> value.score(score -> score.order(SortOrder.Desc)));
                }
                builder.withSort(value -> value.field(field -> field.field("soldCount").order(SortOrder.Desc)))
                        .withSort(value -> value.field(field -> field.field("spuId").order(SortOrder.Desc)));
            }
        }
    }

    /** 将内部 ES 文档和高亮结果裁剪为前台需要的字段。 */
    private ProductSearchVO toVO(SearchHit<ProductSearchDocument> hit) {
        ProductSearchDocument document = hit.getContent();
        List<String> categories = document.getCategoryPathNames();
        return ProductSearchVO.builder()
                .spuId(document.getSpuId())
                .productName(document.getProductName())
                .subtitle(document.getSubtitle())
                .highlightedProductName(highlight(hit, "productName", document.getProductName()))
                .highlightedSubtitle(highlight(hit, "subtitle", document.getSubtitle()))
                .mainImageUrl(document.getMainImageUrl())
                .brandId(document.getBrandId())
                .brandName(document.getBrandName())
                .categoryId(document.getCategoryId())
                .categoryName(categories == null || categories.isEmpty()
                        ? null : categories.get(categories.size() - 1))
                .minPrice(document.getMinPrice())
                .maxPrice(document.getMaxPrice())
                .hasStock(document.getHasStock())
                .soldCount(document.getSoldCount())
                .build();
    }

    /** ES 未返回高亮片段时回退到原始字段，前端无需处理 null 高亮。 */
    private String highlight(SearchHit<ProductSearchDocument> hit, String field, String fallback) {
        List<String> values = hit.getHighlightField(field);
        return values == null || values.isEmpty() ? fallback : String.join("", values);
    }

    /** Bean Validation 只能校验单字段范围，这里补充两个价格之间的关系校验。 */
    private void validatePrice(ProductSearchQuery request) {
        if (request.getMinPrice() != null && request.getMaxPrice() != null
                && request.getMinPrice().compareTo(request.getMaxPrice()) > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "最低价格不能高于最高价格");
        }
    }

    /** 价格和有货条件必须由同一个 SKU 满足，不能分别命中不同的嵌套对象。 */
    private void addSkuFilters(List<Query> filters, ProductSearchQuery request) {
        if (request.getMinPrice() == null && request.getMaxPrice() == null
                && !Boolean.TRUE.equals(request.getInStock())) {
            return;
        }
        List<Query> skuFilters = new ArrayList<>();
        if (request.getMinPrice() != null) {
            skuFilters.add(Query.of(query -> query.range(range -> range.number(number -> number
                    .field("skus.salePrice").gte(request.getMinPrice().doubleValue())))));
        }
        if (request.getMaxPrice() != null) {
            skuFilters.add(Query.of(query -> query.range(range -> range.number(number -> number
                    .field("skus.salePrice").lte(request.getMaxPrice().doubleValue())))));
        }
        if (Boolean.TRUE.equals(request.getInStock())) {
            skuFilters.add(Query.of(query -> query.range(range -> range.number(number -> number
                    .field("skus.availableStock").gt(0D)))));
        }
        filters.add(Query.of(query -> query.nested(nested -> nested
                .path("skus")
                .query(nestedQuery -> nestedQuery.bool(bool -> bool.filter(skuFilters))))));
    }

    /** 阻止 from + size 超过 ES 默认结果窗口，避免请求在运行时被 ES 拒绝。 */
    private void validatePageWindow(ProductSearchQuery request) {
        long start = (long) (request.getCurrent() - 1) * request.getSize();
        if (start + request.getSize() > MAX_RESULT_WINDOW) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "普通分页最多查看前" + MAX_RESULT_WINDOW + "条数据");
        }
    }
}
