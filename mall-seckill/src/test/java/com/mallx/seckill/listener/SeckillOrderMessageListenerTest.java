package com.mallx.seckill.listener;

import com.mallx.common.enums.ResultCode;
import com.mallx.common.exception.BusinessException;
import com.mallx.seckill.message.SeckillOrderMessage;
import com.mallx.seckill.service.SeckillOrderRequestService;
import com.mallx.seckill.service.SeckillRedisService;
import com.mallx.seckill.vo.SeckillResultVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SeckillOrderMessageListenerTest {
    private SeckillOrderRequestService requestService;
    private SeckillRedisService redisService;
    private SeckillOrderMessageListener listener;
    private SeckillOrderMessage message;

    @BeforeEach
    void setUp() {
        requestService = mock(SeckillOrderRequestService.class);
        redisService = mock(SeckillRedisService.class);
        RedissonClient redissonClient = mock(RedissonClient.class);
        RLock lock = mock(RLock.class);
        when(redissonClient.getLock(org.mockito.ArgumentMatchers.anyString())).thenReturn(lock);
        listener = new SeckillOrderMessageListener(requestService, redisService, redissonClient);
        message = new SeckillOrderMessage(
                "100", 1L, 2L, 3L, 4L, 5L, 1, new BigDecimal("9.90"));
        when(redisService.getResult("100")).thenReturn(SeckillResultVO.builder()
                .requestNo("100").status(0).build());
    }

    @Test
    void deterministicBusinessFailureCompensatesAndRecordsFailure() {
        when(requestService.createOrder(message))
                .thenThrow(new BusinessException(ResultCode.BAD_REQUEST, "地址失效"));

        listener.createOrder(message);

        verify(redisService).compensate(message, "地址失效");
        verify(requestService).markFailed(message, "地址失效");
    }

    @Test
    void internalFailureIsRetriedByRabbitListener() {
        BusinessException failure = new BusinessException(ResultCode.INTERNAL_ERROR, "数据库繁忙");
        when(requestService.createOrder(message)).thenThrow(failure);

        assertThrows(BusinessException.class, () -> listener.createOrder(message));

        verify(redisService, never()).compensate(message, "数据库繁忙");
        verify(requestService, never()).markFailed(message, "数据库繁忙");
    }

}
