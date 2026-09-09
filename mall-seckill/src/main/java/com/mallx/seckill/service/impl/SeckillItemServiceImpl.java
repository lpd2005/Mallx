package com.mallx.seckill.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mallx.seckill.entity.SeckillItem;
import com.mallx.seckill.mapper.SeckillItemMapper;
import com.mallx.seckill.service.SeckillItemService;
import org.springframework.stereotype.Service;

/** 秒杀商品的 MyBatis-Plus 基础服务实现。 */
@Service
public class SeckillItemServiceImpl extends ServiceImpl<SeckillItemMapper, SeckillItem>
        implements SeckillItemService {
}
