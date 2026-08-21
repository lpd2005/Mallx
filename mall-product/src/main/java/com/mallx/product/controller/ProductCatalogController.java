package com.mallx.product.controller;

import com.mallx.common.result.Result;
import com.mallx.product.service.ProductCategoryService;
import com.mallx.product.vo.CategoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 前台商品分类只读接口；分类管理写操作后续由mall-admin编排。 */
@RestController
@RequestMapping("/products/categories")
@RequiredArgsConstructor
public class ProductCatalogController {
    private final ProductCategoryService categoryService;

    /** 查询启用的商品分类树。 */
    @GetMapping
    public Result<List<CategoryVO>> categories() { return Result.success(categoryService.categoryTree()); }
}
