package com.mallx.order.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mallx.order.entity.OrderOperationLog;
import com.mallx.order.mapper.OrderOperationLogMapper;
import com.mallx.order.service.OrderOperationLogService;

import org.springframework.stereotype.Service;

/** 订单操作流水服务实现。 */
@Service
public class OrderOperationLogServiceImpl
        extends ServiceImpl<OrderOperationLogMapper, OrderOperationLog>
        implements OrderOperationLogService {}
