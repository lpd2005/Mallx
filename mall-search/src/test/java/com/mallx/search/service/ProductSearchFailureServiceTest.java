package com.mallx.search.service;

import com.mallx.common.exception.BusinessException;
import com.mallx.search.service.impl.ProductSearchFailureService;
import com.mallx.search.service.impl.ProductSearchIndexService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductSearchFailureServiceTest {
    private HashOperations<String, Object, Object> hashOperations;
    private ProductSearchIndexService indexService;
    private ProductSearchFailureService failureService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        hashOperations = mock(HashOperations.class);
        indexService = mock(ProductSearchIndexService.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        failureService = new ProductSearchFailureService(redisTemplate, indexService);
    }

    @Test
    void replaySynchronizesLatestDocumentThenDeletesFailure() {
        when(hashOperations.get("mallx:search:sync:failures", "event-1"))
                .thenReturn("100|2026-09-10T00:00:00");

        failureService.replay("event-1");

        verify(indexService).synchronize(100L);
        verify(hashOperations).delete("mallx:search:sync:failures", "event-1");
    }

    @Test
    void replayRejectsUnknownEvent() {
        assertThrows(BusinessException.class, () -> failureService.replay("missing"));
    }
}
