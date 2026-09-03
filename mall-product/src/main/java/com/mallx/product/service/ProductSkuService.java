package com.mallx.product.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.mallx.product.dto.SkuRequest;
import com.mallx.product.entity.ProductSku;
import com.mallx.product.vo.SkuVO;
import com.mallx.product.vo.CartProductVO;

import java.util.Collection;
import java.util.List;

/** SKU业务服务，SKU是商品定价和库存管理的最小单元。 */
public interface ProductSkuService extends IService<ProductSku> {
    /** 为SPU创建草稿SKU。 */
    ProductSku createSku(SkuRequest request);
    /** 更新SKU规格和价格。 */
    ProductSku updateSku(Long skuId, SkuRequest request);
    /** 物理删除尚未上架的草稿SKU。 */
    void deleteSku(Long skuId);
    /** 修改SKU状态并刷新SPU价格区间。 */
    void changeStatus(Long skuId, Integer status);
    /** 获取必须存在的SKU，供库存等模块调用。 */
    ProductSku requireSku(Long skuId);
    /** 在已有事务中锁定SKU行，协调后台删除与首次入库。 */
    ProductSku lockSku(Long skuId);
    /** 查询指定已上架SPU下的已上架SKU。 */
    List<SkuVO> listBySpu(Long spuId);
    /** 查询前台可见的已上架SKU。 */
    SkuVO getSku(Long skuId);
    /** 批量查询购物车展示所需的SPU和SKU当前信息。 */
    List<CartProductVO> listCartProducts(Collection<Long> skuIds);
}
