package com.mallx.aftersale.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.mallx.aftersale.dto.AuditAfterSaleRequest;
import com.mallx.aftersale.dto.CreateAfterSaleRequest;
import com.mallx.aftersale.dto.ReceiveReturnRequest;
import com.mallx.aftersale.dto.ReturnShipmentRequest;
import com.mallx.aftersale.entity.AfterSale;
import com.mallx.aftersale.vo.AfterSaleVO;
import com.mallx.common.result.PageResult;

/** 整单售后流程编排，管理操作只能由已鉴权入口调用。 */
public interface AfterSaleService extends IService<AfterSale> {
    AfterSaleVO apply(Long userId, CreateAfterSaleRequest request);

    PageResult<AfterSaleVO> pageMine(Long userId, Integer status, int current, int size);

    AfterSaleVO getMine(Long userId, String afterSaleNo);

    AfterSaleVO cancel(Long userId, String afterSaleNo);

    AfterSaleVO submitReturn(Long userId, String afterSaleNo, ReturnShipmentRequest request);

    PageResult<AfterSaleVO> pageManagement(Integer status, int current, int size);

    AfterSaleVO getManagement(String afterSaleNo);

    AfterSaleVO audit(Long operatorId, String afterSaleNo, AuditAfterSaleRequest request);

    AfterSaleVO receiveReturn(Long operatorId, String afterSaleNo, ReceiveReturnRequest request);

    AfterSaleVO refund(Long operatorId, String afterSaleNo);
}
