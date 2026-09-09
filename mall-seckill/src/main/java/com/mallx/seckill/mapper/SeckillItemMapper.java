package com.mallx.seckill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mallx.seckill.entity.SeckillItem;

import org.apache.ibatis.annotations.Mapper;

/** 秒杀商品表的 MyBatis-Plus Mapper。 */
@Mapper
public interface SeckillItemMapper extends BaseMapper<SeckillItem> {}
