package com.mallx.aftersale.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mallx.aftersale.entity.AfterSaleLog;
import com.mallx.aftersale.mapper.AfterSaleLogMapper;
import com.mallx.aftersale.service.AfterSaleLogService;

import org.springframework.stereotype.Service;

@Service
public class AfterSaleLogServiceImpl extends ServiceImpl<AfterSaleLogMapper, AfterSaleLog>
        implements AfterSaleLogService {}
