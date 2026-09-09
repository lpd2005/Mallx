package com.mallx.seckill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mallx.seckill.entity.SeckillOrderRequest;

import org.apache.ibatis.annotations.Mapper;

/** 秒杀异步下单请求表的 MyBatis-Plus Mapper。 */
@Mapper
public interface SeckillOrderRequestMapper extends BaseMapper<SeckillOrderRequest> {}
