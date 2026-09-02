package com.mallx.order.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mallx.order.entity.OrderItem;
import com.mallx.order.mapper.OrderItemMapper;
import com.mallx.order.service.OrderItemService;

import org.springframework.stereotype.Service;

/** 订单商品快照服务实现。 */
@Service
public class OrderItemServiceImpl extends ServiceImpl<OrderItemMapper, OrderItem>
        implements OrderItemService {}
