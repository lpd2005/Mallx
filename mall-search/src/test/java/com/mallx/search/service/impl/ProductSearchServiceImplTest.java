package com.mallx.search.service.impl;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.mallx.common.exception.BusinessException;
import com.mallx.search.config.ProductSearchProperties;
import com.mallx.search.document.ProductSearchDocument;
import com.mallx.search.dto.ProductSearchQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class ProductSearchServiceImplTest {
    private ElasticsearchOperations operations;
    private ProductSearchIndexService indexService;
    private ProductSearchServiceImpl searchService;

    @BeforeEach
    void setUp() {
        operations = mock(ElasticsearchOperations.class);
        indexService = mock(ProductSearchIndexService.class);
        ProductSearchProperties properties = new ProductSearchProperties();
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        searchService = new ProductSearchServiceImpl(
                operations, indexService, properties, redisTemplate);
    }

    @Test
    void priceAndStockFiltersUseTheSameNestedSku() {
        ProductSearchQuery request = new ProductSearchQuery();
        request.setMinPrice(new BigDecimal("10.00"));
        request.setMaxPrice(new BigDecimal("20.00"));
        request.setInStock(true);
        SearchHits<ProductSearchDocument> hits = mock(SearchHits.class);
        when(hits.getSearchHits()).thenReturn(List.of());
        when(hits.getTotalHits()).thenReturn(0L);
        when(operations.search(any(NativeQuery.class), eq(ProductSearchDocument.class)))
                .thenReturn(hits);

        searchService.search(request);

        ArgumentCaptor<NativeQuery> captor = ArgumentCaptor.forClass(NativeQuery.class);
        verify(operations).search(captor.capture(), eq(ProductSearchDocument.class));
        Query nestedFilter = captor.getValue().getQuery().bool().filter().get(0);
        assertEquals("skus", nestedFilter.nested().path());
        assertEquals(3, nestedFilter.nested().query().bool().filter().size());
    }

    @Test
    void rejectsInvalidPriceRangeBeforeCallingElasticsearch() {
        ProductSearchQuery request = new ProductSearchQuery();
        request.setMinPrice(new BigDecimal("20.00"));
        request.setMaxPrice(new BigDecimal("10.00"));

        assertThrows(BusinessException.class, () -> searchService.search(request));

        verify(indexService, never()).ensureIndex();
        verify(operations, never()).search(any(NativeQuery.class), eq(ProductSearchDocument.class));
    }

    @Test
    void rejectsPageBeyondElasticsearchResultWindow() {
        ProductSearchQuery request = new ProductSearchQuery();
        request.setCurrent(201);
        request.setSize(50);

        assertThrows(BusinessException.class, () -> searchService.search(request));

        verify(indexService, never()).ensureIndex();
    }
}
