package com.mallx.seckill.service;

import com.mallx.common.exception.BusinessException;
import com.mallx.seckill.dto.SeckillPurchaseRequest;
import com.mallx.seckill.message.SeckillOrderMessage;
import com.mallx.seckill.publisher.SeckillMessagePublisher;
import com.mallx.seckill.service.impl.SeckillPurchaseServiceImpl;
import com.mallx.seckill.vo.SeckillSubmitVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SeckillPurchaseServiceImplTest {
    private SeckillRedisService redisService;
    private SeckillMessagePublisher publisher;
    private SeckillPurchaseService service;

    @BeforeEach
    void setUp() {
        redisService = mock(SeckillRedisService.class);
        publisher = mock(SeckillMessagePublisher.class);
        service = new SeckillPurchaseServiceImpl(
                redisService, publisher, mock(SeckillOrderRequestService.class));
    }

    @Test
    void reserveSuccessMarksPendingBeforePublishing() {
        SeckillOrderMessage stored = new SeckillOrderMessage(
                "100", 1L, 2L, 3L, 4L, 5L, 1, new BigDecimal("9.90"));
        when(redisService.reserve(any())).thenReturn(0L);
        when(redisService.getMessage(any())).thenReturn(stored);
        SeckillPurchaseRequest request = request();

        SeckillSubmitVO response = service.submit(4L, 2L, request);

        assertEquals(0, response.getStatus());
        InOrder order = inOrder(redisService, publisher);
        order.verify(redisService).markDispatched(response.getRequestNo());
        order.verify(publisher).send(stored);
    }

    @Test
    void soldOutDoesNotPublishMessage() {
        when(redisService.reserve(any())).thenReturn(4L);

        BusinessException exception = assertThrows(
                BusinessException.class, () -> service.submit(4L, 2L, request()));

        assertEquals("秒杀商品已售罄", exception.getMessage());
        verify(publisher, never()).send(any());
    }

    private SeckillPurchaseRequest request() {
        SeckillPurchaseRequest request = new SeckillPurchaseRequest();
        request.setAddressId(5L);
        request.setQuantity(1);
        return request;
    }
}
