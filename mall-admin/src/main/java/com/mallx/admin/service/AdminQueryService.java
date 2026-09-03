package com.mallx.admin.service;

import com.mallx.admin.dto.AdminPageQuery;
import com.mallx.admin.vo.*;
import com.mallx.common.result.PageResult;
import com.mallx.inventory.vo.InventoryStockVO;
import com.mallx.order.vo.OrderVO;

/** 后台只读组合查询，不绕过各领域的写入规则。 */
public interface AdminQueryService {
    PageResult<AdminCategoryVO> categories(AdminPageQuery query);

    PageResult<AdminBrandVO> brands(AdminPageQuery query);

    PageResult<AdminSpuVO> spus(AdminPageQuery query);

    PageResult<AdminSkuVO> skus(Long spuId, AdminPageQuery query);

    PageResult<AdminOrderSummaryVO> orders(Long userId, AdminPageQuery query);

    OrderVO order(String orderNo);

    PageResult<InventoryStockVO> stocks(Long skuId, AdminPageQuery query);

    PageResult<AdminOperationLogVO> logs(Long operatorId, AdminPageQuery query);

    PageResult<AdminRoleVO> roles(AdminPageQuery query);
}
