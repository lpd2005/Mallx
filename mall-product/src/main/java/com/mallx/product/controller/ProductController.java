package com.mallx.product.controller;

import com.mallx.common.result.Result;
import com.mallx.product.service.ProductSkuService;
import com.mallx.product.service.ProductSpuService;
import com.mallx.product.vo.SkuVO;
import com.mallx.product.vo.SpuVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 前台商品只读接口，统一处理SPU和SKU查询。 */
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductSpuService spuService;
    private final ProductSkuService skuService;

    /** 查询已上架SPU详情。 */
    @GetMapping("/{spuId}")
    public Result<SpuVO> spu(@PathVariable Long spuId) {
        return Result.success(spuService.getSpu(spuId));
    }

    /** 查询指定SPU下的全部已上架SKU。 */
    @GetMapping("/{spuId}/skus")
    public Result<List<SkuVO>> skus(@PathVariable Long spuId) {
        return Result.success(skuService.listBySpu(spuId));
    }

    /** 查询已上架SKU详情。 */
    @GetMapping("/skus/{skuId}")
    public Result<SkuVO> sku(@PathVariable Long skuId) {
        return Result.success(skuService.getSku(skuId));
    }
}
