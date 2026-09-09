package com.mallx.seckill.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.mallx.seckill.entity.SeckillItem;

/** 秒杀商品基础持久化服务；活动状态校验由 {@link SeckillActivityService} 统一编排。 */
public interface SeckillItemService extends IService<SeckillItem> {}
