package com.mallx.admin.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.bean.BeanUtil;

import com.mallx.admin.dto.*;
import com.mallx.admin.service.*;
import com.mallx.admin.vo.*;
import com.mallx.common.result.*;
import com.mallx.product.dto.BrandRequest;
import com.mallx.product.service.ProductBrandService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

/** 品牌后台入口：查询包括禁用记录，写操作复用商品领域服务。 */
@RestController
@RequestMapping("/admin/brands")
@RequiredArgsConstructor
@SaCheckPermission("product:manage")
public class AdminBrandController {
    private final ProductBrandService brandService;
    private final AdminQueryService queryService;
    private final AdminOperationLogService operationLogService;

    /** 后台分页查询，status筛选enabled状态。 */
    @GetMapping
    public Result<PageResult<AdminBrandVO>> page(@Valid @ModelAttribute AdminPageQuery query) {
        return Result.success(queryService.brands(query));
    }

    /** 创建记录并写入后台操作日志。 */
    @PostMapping
    public Result<AdminBrandVO> create(@Valid @RequestBody BrandRequest request) {
        return Result.success(
                BeanUtil.toBean(
                        operationLogService.execute(
                                "product:create-brand",
                                null,
                                () -> brandService.createBrand(request)),
                        AdminBrandVO.class));
    }

    /** 修改字段，业务约束由领域服务校验。 */
    @PutMapping("/{id}")
    public Result<AdminBrandVO> update(
            @PathVariable Long id, @Valid @RequestBody BrandRequest request) {
        return Result.success(
                BeanUtil.toBean(
                        operationLogService.execute(
                                "product:update-brand",
                                id.toString(),
                                () -> brandService.updateBrand(id, request)),
                        AdminBrandVO.class));
    }

    /** 物理删除，仍然检查子分类或SPU引用关系。 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        operationLogService.execute(
                "product:delete-brand", id.toString(), () -> brandService.deleteBrand(id));
        return Result.success();
    }
}
