package com.mallx.admin.controller;

import com.mallx.admin.service.AdminOperationLogService;
import com.mallx.seckill.dto.SeckillActivityRequest;
import com.mallx.seckill.dto.SeckillItemRequest;
import com.mallx.seckill.service.SeckillActivityService;
import com.mallx.seckill.vo.SeckillActivityVO;
import com.mallx.seckill.vo.SeckillItemVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class AdminSeckillControllerTest {
    private AdminOperationLogService operationLogService;
    private SeckillActivityService activityService;
    private AdminSeckillController controller;

    @BeforeEach
    void setUp() {
        operationLogService = mock(AdminOperationLogService.class);
        activityService = mock(SeckillActivityService.class);
        controller = new AdminSeckillController(operationLogService, activityService);

        doAnswer(invocation -> ((Supplier<?>) invocation.getArgument(2)).get())
                .when(operationLogService)
                .execute(anyString(), nullable(String.class), any(Supplier.class));
        doAnswer(
                        invocation -> {
                            ((Runnable) invocation.getArgument(2)).run();
                            return null;
                        })
                .when(operationLogService)
                .execute(anyString(), anyString(), any(Runnable.class));
    }

    @Test
    void mutationEndpointsExecuteThroughOperationLogService() {
        Long activityId = 10L;
        Long itemId = 20L;
        SeckillActivityRequest activityRequest = new SeckillActivityRequest();
        SeckillItemRequest itemRequest = new SeckillItemRequest();
        SeckillActivityVO activity = mock(SeckillActivityVO.class);
        SeckillItemVO item = mock(SeckillItemVO.class);
        when(activityService.createActivity(activityRequest)).thenReturn(activity);
        when(activityService.updateActivity(activityId, activityRequest)).thenReturn(activity);
        when(activityService.addItem(activityId, itemRequest)).thenReturn(item);
        when(activityService.updateItem(activityId, itemId, itemRequest)).thenReturn(item);
        when(activityService.prepareActivity(activityId)).thenReturn(activity);

        controller.create(activityRequest);
        controller.update(activityId, activityRequest);
        controller.addItem(activityId, itemRequest);
        controller.updateItem(activityId, itemId, itemRequest);
        controller.removeItem(activityId, itemId);
        controller.prepare(activityId);
        controller.close(activityId);

        verify(operationLogService)
                .execute(eq("seckill:create-activity"), isNull(), any(Supplier.class));
        verify(operationLogService)
                .execute(eq("seckill:update-activity"), eq("10"), any(Supplier.class));
        verify(operationLogService)
                .execute(eq("seckill:add-item"), eq("10"), any(Supplier.class));
        verify(operationLogService)
                .execute(eq("seckill:update-item"), eq("10/20"), any(Supplier.class));
        verify(operationLogService)
                .execute(eq("seckill:remove-item"), eq("10/20"), any(Runnable.class));
        verify(operationLogService)
                .execute(eq("seckill:prepare-activity"), eq("10"), any(Supplier.class));
        verify(operationLogService)
                .execute(eq("seckill:close-activity"), eq("10"), any(Runnable.class));

        verify(activityService).createActivity(activityRequest);
        verify(activityService).updateActivity(activityId, activityRequest);
        verify(activityService).addItem(activityId, itemRequest);
        verify(activityService).updateItem(activityId, itemId, itemRequest);
        verify(activityService).removeItem(activityId, itemId);
        verify(activityService).prepareActivity(activityId);
        verify(activityService).closeActivity(activityId);
    }

    @Test
    void queryEndpointsDoNotWriteOperationLogs() {
        Long activityId = 10L;
        SeckillActivityVO activity = mock(SeckillActivityVO.class);
        when(activityService.listActivities(null)).thenReturn(List.of(activity));
        when(activityService.getActivity(activityId)).thenReturn(activity);

        controller.list(null);
        controller.detail(activityId);

        verify(activityService).listActivities(null);
        verify(activityService).getActivity(activityId);
        verifyNoInteractions(operationLogService);
    }
}
