package com.mallx.admin.controller;

import com.mallx.admin.service.AdminOperationLogService;
import com.mallx.search.service.impl.ProductSearchFailureService;
import com.mallx.search.service.impl.ProductSearchIndexService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@SuppressWarnings("unchecked")
class AdminSearchControllerTest {
    private AdminOperationLogService operationLogService;
    private ProductSearchIndexService indexService;
    private ProductSearchFailureService failureService;
    private AdminSearchController controller;

    @BeforeEach
    void setUp() {
        operationLogService = mock(AdminOperationLogService.class);
        indexService = mock(ProductSearchIndexService.class);
        failureService = mock(ProductSearchFailureService.class);
        controller = new AdminSearchController(operationLogService, indexService, failureService);
        doAnswer(invocation -> ((Supplier<?>) invocation.getArgument(2)).get())
                .when(operationLogService)
                .execute(anyString(), anyString(), any(Supplier.class));
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(2)).run();
            return null;
        }).when(operationLogService).execute(anyString(), anyString(), any(Runnable.class));
    }

    @Test
    void mutationEndpointsExecuteThroughOperationLogService() {
        controller.rebuild();
        controller.synchronize(100L);
        controller.replay("event-1");

        verify(operationLogService)
                .execute(eq("search:rebuild-product-index"), eq("product_index"), any(Supplier.class));
        verify(operationLogService)
                .execute(eq("search:sync-product"), eq("100"), any(Runnable.class));
        verify(operationLogService)
                .execute(eq("search:replay-failure"), eq("event-1"), any(Runnable.class));
        verify(indexService).rebuild();
        verify(indexService).synchronize(100L);
        verify(failureService).replay("event-1");
    }
}
