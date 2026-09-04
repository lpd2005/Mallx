package com.mallx.product.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.mallx.common.result.PageResult;
import com.mallx.product.dto.ProductPageQuery;
import com.mallx.product.dto.SpuRequest;
import com.mallx.product.entity.ProductSpu;
import com.mallx.product.vo.SpuVO;

/** SPU业务服务，负责商品共享信息和上下架状态。 */
public interface ProductSpuService extends IService<ProductSpu> {
    /** 前台分页查询，仅返回已上架商品，按更新时间及ID倒序排列。 */
    PageResult<SpuVO> pageProducts(ProductPageQuery query);

    /** 创建草稿SPU。 */
    ProductSpu createSpu(SpuRequest request);

    /** 更新SPU基础资料。 */
    ProductSpu updateSpu(Long spuId, SpuRequest request);

    /** 物理删除没有SKU的草稿SPU。 */
    void deleteSpu(Long spuId);

    /** 修改SPU状态。 */
    void changeStatus(Long spuId, Integer status);

    /** 获取必须存在的SPU，供其他业务模块内部调用。 */
    ProductSpu requireSpu(Long spuId);

    /** 查询前台可见的已上架SPU。 */
    SpuVO getSpu(Long spuId);
}
