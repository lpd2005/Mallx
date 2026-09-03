package com.mallx.admin.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.bean.BeanUtil;

import com.mallx.admin.dto.*;
import com.mallx.admin.service.*;
import com.mallx.admin.vo.*;
import com.mallx.common.result.*;
import com.mallx.product.dto.*;
import com.mallx.product.service.*;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

/** SPU和SKU共用一个商品处理类，分类与品牌各自独立。 */
@RestController
@RequestMapping("/admin/products")
@RequiredArgsConstructor
@SaCheckPermission("product:manage")
public class AdminProductController {
    private final ProductSpuService spuService;
    private final ProductSkuService skuService;
    private final AdminProductService adminProductService;
    private final AdminQueryService queryService;
    private final AdminOperationLogService operationLogService;

    /** 查询Spu管理列表，不限制上架状态。 */
    @GetMapping("/spus")
    public Result<PageResult<AdminSpuVO>> spus(@Valid @ModelAttribute AdminPageQuery query) {
        return Result.success(queryService.spus(query));
    }

    /** 获取草稿、上架或下架Spu详情。 */
    @GetMapping("/spus/{id}")
    public Result<AdminSpuVO> getSpu(@PathVariable Long id) {
        return Result.success(BeanUtil.toBean(spuService.requireSpu(id), AdminSpuVO.class));
    }

    /** 创建草稿，禁止从客户端直接指定售卖状态。 */
    @PostMapping("/spus")
    public Result<AdminSpuVO> createSpu(@Valid @RequestBody SpuRequest request) {
        return Result.success(
                BeanUtil.toBean(
                        operationLogService.execute(
                                "product:create-spu", null, () -> spuService.createSpu(request)),
                        AdminSpuVO.class));
    }

    /** 更新Spu资料。 */
    @PutMapping("/spus/{id}")
    public Result<AdminSpuVO> updateSpu(
            @PathVariable Long id, @Valid @RequestBody SpuRequest request) {
        return Result.success(
                BeanUtil.toBean(
                        operationLogService.execute(
                                "product:update-spu",
                                id.toString(),
                                () -> spuService.updateSpu(id, request)),
                        AdminSpuVO.class));
    }

    /** 上下架必须走领域状态校验，不能直接updateById。 */
    @PutMapping("/spus/{id}/status")
    public Result<Void> statusSpu(
            @PathVariable Long id, @Valid @RequestBody ProductStatusRequest request) {
        operationLogService.execute(
                "product:status-spu",
                id.toString(),
                () -> spuService.changeStatus(id, request.getStatus()));
        return Result.success();
    }

    /** 物理删除仍受领域约束限制。 */
    @DeleteMapping("/spus/{id}")
    public Result<Void> deleteSpu(@PathVariable Long id) {
        operationLogService.execute(
                "product:delete-spu", id.toString(), () -> spuService.deleteSpu(id));
        return Result.success();
    }

    /** 查询Sku管理列表，不限制上架状态。 */
    @GetMapping("/skus")
    public Result<PageResult<AdminSkuVO>> skus(
            @RequestParam(required = false) Long spuId,
            @Valid @ModelAttribute AdminPageQuery query) {
        return Result.success(queryService.skus(spuId, query));
    }

    /** 获取草稿、上架或下架Sku详情。 */
    @GetMapping("/skus/{id}")
    public Result<AdminSkuVO> getSku(@PathVariable Long id) {
        return Result.success(BeanUtil.toBean(skuService.requireSku(id), AdminSkuVO.class));
    }

    /** 创建草稿，禁止从客户端直接指定售卖状态。 */
    @PostMapping("/skus")
    public Result<AdminSkuVO> createSku(@Valid @RequestBody SkuRequest request) {
        return Result.success(
                BeanUtil.toBean(
                        operationLogService.execute(
                                "product:create-sku", null, () -> skuService.createSku(request)),
                        AdminSkuVO.class));
    }

    /** 更新Sku资料。 */
    @PutMapping("/skus/{id}")
    public Result<AdminSkuVO> updateSku(
            @PathVariable Long id, @Valid @RequestBody SkuRequest request) {
        return Result.success(
                BeanUtil.toBean(
                        operationLogService.execute(
                                "product:update-sku",
                                id.toString(),
                                () -> skuService.updateSku(id, request)),
                        AdminSkuVO.class));
    }

    /** 上下架必须走领域状态校验，不能直接updateById。 */
    @PutMapping("/skus/{id}/status")
    public Result<Void> statusSku(
            @PathVariable Long id, @Valid @RequestBody ProductStatusRequest request) {
        operationLogService.execute(
                "product:status-sku",
                id.toString(),
                () -> skuService.changeStatus(id, request.getStatus()));
        return Result.success();
    }

    /** 物理删除仍受领域约束限制。 */
    @DeleteMapping("/skus/{id}")
    public Result<Void> deleteSku(@PathVariable Long id) {
        operationLogService.execute(
                "product:delete-sku", id.toString(), () -> adminProductService.deleteSku(id));
        return Result.success();
    }
}
