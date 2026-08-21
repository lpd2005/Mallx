package com.mallx.product.controller;

import com.mallx.common.result.Result;
import com.mallx.product.service.ProductBrandService;
import com.mallx.product.vo.BrandVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 前台商品品牌只读接口；品牌管理写操作后续由mall-admin编排。 */
@RestController
@RequestMapping("/products/brands")
@RequiredArgsConstructor
public class ProductBrandController {
    private final ProductBrandService brandService;

    /** 查询全部启用品牌。 */
    @GetMapping
    public Result<List<BrandVO>> brands() {
        return Result.success(brandService.listEnabledBrands());
    }
}
