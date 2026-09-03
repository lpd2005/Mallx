package com.mallx.admin.service.impl;

import cn.hutool.core.util.StrUtil;

import com.mallx.admin.dto.AdminPageQuery;
import com.mallx.admin.entity.AdminOperationLog;
import com.mallx.admin.service.*;
import com.mallx.admin.support.AdminPages;
import com.mallx.admin.vo.*;
import com.mallx.auth.entity.AuthRole;
import com.mallx.common.enums.ResultCode;
import com.mallx.common.exception.BusinessException;
import com.mallx.common.result.PageResult;
import com.mallx.inventory.entity.InventoryStock;
import com.mallx.inventory.service.InventoryStockService;
import com.mallx.inventory.vo.InventoryStockVO;
import com.mallx.order.entity.MallOrder;
import com.mallx.order.service.OrderService;
import com.mallx.order.vo.OrderVO;
import com.mallx.product.entity.*;
import com.mallx.product.service.*;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.Objects;

/** 单表筛选使用Service的lambdaQuery，后台查询不套用前台的上架过滤。 */
@Service
@RequiredArgsConstructor
public class AdminQueryServiceImpl implements AdminQueryService {
    private final ProductCategoryService categoryService;
    private final ProductBrandService brandService;
    private final ProductSpuService spuService;
    private final ProductSkuService skuService;
    private final InventoryStockService stockService;
    private final OrderService orderService;
    private final AdminOperationLogService operationLogService;
    private final AdminRoleService roleService;

    /** 分页查询商品分类，支持分类名模糊与启用状态过滤，按排序值和ID升序。 */
    @Override
    public PageResult<AdminCategoryVO> categories(AdminPageQuery query) {
        return AdminPages.result(
                categoryService
                        .lambdaQuery()
                        .like(
                                StrUtil.isNotBlank(query.getKeyword()),
                                ProductCategory::getCategoryName,
                                query.getKeyword())
                        .eq(
                                query.getStatus() != null,
                                ProductCategory::getEnabled,
                                query.getStatus())
                        .orderByAsc(ProductCategory::getSort, ProductCategory::getId)
                        .page(AdminPages.page(query)),
                AdminCategoryVO.class);
    }

    /** 分页查询商品品牌，支持品牌名模糊与启用状态过滤，按排序值和ID升序。 */
    @Override
    public PageResult<AdminBrandVO> brands(AdminPageQuery query) {
        return AdminPages.result(
                brandService
                        .lambdaQuery()
                        .like(
                                StrUtil.isNotBlank(query.getKeyword()),
                                ProductBrand::getBrandName,
                                query.getKeyword())
                        .eq(query.getStatus() != null, ProductBrand::getEnabled, query.getStatus())
                        .orderByAsc(ProductBrand::getSort, ProductBrand::getId)
                        .page(AdminPages.page(query)),
                AdminBrandVO.class);
    }

    /** 分页查询SPU商品，支持商品名模糊与状态过滤，按ID倒序。 */
    @Override
    public PageResult<AdminSpuVO> spus(AdminPageQuery query) {
        return AdminPages.result(
                spuService
                        .lambdaQuery()
                        .like(
                                StrUtil.isNotBlank(query.getKeyword()),
                                ProductSpu::getProductName,
                                query.getKeyword())
                        .eq(query.getStatus() != null, ProductSpu::getStatus, query.getStatus())
                        .orderByDesc(ProductSpu::getId)
                        .page(AdminPages.page(query)),
                AdminSpuVO.class);
    }

    /** 分页查询SKU，支持按所属SPU过滤、SKU名模糊与状态筛选，按ID倒序。 */
    @Override
    public PageResult<AdminSkuVO> skus(Long spuId, AdminPageQuery query) {
        return AdminPages.result(
                skuService
                        .lambdaQuery()
                        .eq(spuId != null, ProductSku::getSpuId, spuId)
                        .like(
                                StrUtil.isNotBlank(query.getKeyword()),
                                ProductSku::getSkuName,
                                query.getKeyword())
                        .eq(query.getStatus() != null, ProductSku::getStatus, query.getStatus())
                        .orderByDesc(ProductSku::getId)
                        .page(AdminPages.page(query)),
                AdminSkuVO.class);
    }

    /** 分页查询订单摘要列表，支持按用户ID、订单号与订单状态筛选，按ID倒序。 */
    @Override
    public PageResult<AdminOrderSummaryVO> orders(Long userId, AdminPageQuery query) {
        return AdminPages.result(
                orderService
                        .lambdaQuery()
                        .eq(userId != null, MallOrder::getUserId, userId)
                        .eq(
                                StrUtil.isNotBlank(query.getKeyword()),
                                MallOrder::getOrderNo,
                                query.getKeyword())
                        .eq(query.getStatus() != null, MallOrder::getStatus, query.getStatus())
                        .orderByDesc(MallOrder::getId)
                        .page(AdminPages.page(query)),
                AdminOrderSummaryVO.class);
    }

    /** 按订单号查询订单完整详情（含明细），校验存在后以真实买家身份复用前台详情装配。 */
    @Override
    public OrderVO order(String orderNo) {
        MallOrder order = orderService.lambdaQuery().eq(MallOrder::getOrderNo, orderNo).one();
        if (order == null) throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        // 仅管理端鉴权后使用真实买家ID复用详情装配，前台不能传任意userId越权查询。
        return orderService.getOrder(order.getUserId(), orderNo);
    }

    /** 分页查询SKU库存，支持按SKU过滤，按SKU升序返回并显式计算缺货标记。 */
    @Override
    public PageResult<InventoryStockVO> stocks(Long skuId, AdminPageQuery query) {
        PageResult<InventoryStockVO> result =
                AdminPages.result(
                        stockService
                                .lambdaQuery()
                                .eq(skuId != null, InventoryStock::getSkuId, skuId)
                                .orderByAsc(InventoryStock::getSkuId)
                                .page(AdminPages.page(query)),
                        InventoryStockVO.class);
        // 缺货标记是派生字段，BeanUtil只能复制同名属性，仍需显式计算。
        result.getRecords()
                .forEach(
                        stock -> stock.setOutOfStock(Objects.equals(stock.getAvailableStock(), 0)));
        return result;
    }

    /** 分页查询后台操作审计日志，支持按操作人与动作关键字筛选，按ID倒序。 */
    @Override
    public PageResult<AdminOperationLogVO> logs(Long operatorId, AdminPageQuery query) {
        return AdminPages.result(
                operationLogService
                        .lambdaQuery()
                        .eq(operatorId != null, AdminOperationLog::getOperatorId, operatorId)
                        .eq(
                                StrUtil.isNotBlank(query.getKeyword()),
                                AdminOperationLog::getAction,
                                query.getKeyword())
                        .orderByDesc(AdminOperationLog::getId)
                        .page(AdminPages.page(query)),
                AdminOperationLogVO.class);
    }

    /** 分页查询后台角色，支持角色名模糊与状态过滤，按ID倒序。 */
    @Override
    public PageResult<AdminRoleVO> roles(AdminPageQuery query) {
        return AdminPages.result(
                roleService
                        .lambdaQuery()
                        .like(
                                StrUtil.isNotBlank(query.getKeyword()),
                                AuthRole::getRoleName,
                                query.getKeyword())
                        .eq(query.getStatus() != null, AuthRole::getStatus, query.getStatus())
                        .orderByDesc(AuthRole::getId)
                        .page(AdminPages.page(query)),
                AdminRoleVO.class);
    }
}
