package com.mallx.cart.service;

import com.mallx.cart.vo.CartCheckoutVO;
import com.mallx.cart.vo.CartVO;

import java.util.Collection;

/** Redis购物车业务服务。 */
public interface CartService {
    /** 查询并使用当前商品、库存信息组装购物车。 */
    CartVO getCart(Long userId);
    /** 将指定数量加入购物车；已有SKU时原子累加。 */
    void addItem(Long userId, Long skuId, int quantity);
    /** 将购物车SKU修改为指定绝对数量。 */
    void updateQuantity(Long userId, Long skuId, int quantity);
    /** 物理删除单个购物车项。 */
    void removeItem(Long userId, Long skuId);
    /** 修改单个购物车项的勾选状态。 */
    void changeSelection(Long userId, Long skuId, boolean selected);
    /** 修改当前购物车全部商品的勾选状态。 */
    void changeAllSelection(Long userId, boolean selected);
    /** 删除当前用户全部已勾选商品。 */
    void removeSelectedItems(Long userId);
    /** 生成已勾选商品的结算预览。 */
    CartCheckoutVO checkout(Long userId);
    /** 订单创建成功后按SKU清理已购买购物车项。 */
    void removePurchasedItems(Long userId, Collection<Long> skuIds);
}
