package com.mallx.cart.controller;

import com.mallx.cart.dto.AddCartItemRequest;
import com.mallx.cart.dto.ChangeCartSelectionRequest;
import com.mallx.cart.dto.UpdateCartQuantityRequest;
import com.mallx.cart.service.CartService;
import com.mallx.cart.vo.CartCheckoutVO;
import com.mallx.cart.vo.CartVO;
import com.mallx.common.context.CurrentUserProvider;
import com.mallx.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 当前登录用户购物车接口。 */
@RestController
@RequestMapping("/users/me/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;
    private final CurrentUserProvider currentUserProvider;

    /** 查询当前购物车及商品、库存实时状态。 */
    @GetMapping
    public Result<CartVO> getCart() {
        return Result.success(cartService.getCart(currentUserProvider.requireUserId()));
    }

    /** 加入购物车；重复加入同一SKU时累加数量。 */
    @PostMapping("/items")
    public Result<Void> addItem(@Valid @RequestBody AddCartItemRequest request) {
        cartService.addItem(currentUserProvider.requireUserId(), request.getSkuId(), request.getQuantity());
        return Result.success();
    }

    /** 修改指定SKU的购物车数量。 */
    @PatchMapping("/items/{skuId}/quantity")
    public Result<Void> updateQuantity(@PathVariable Long skuId,
                                       @Valid @RequestBody UpdateCartQuantityRequest request) {
        cartService.updateQuantity(currentUserProvider.requireUserId(), skuId, request.getQuantity());
        return Result.success();
    }

    /** 修改指定SKU的勾选状态。 */
    @PatchMapping("/items/{skuId}/selected")
    public Result<Void> changeSelection(@PathVariable Long skuId,
                                        @Valid @RequestBody ChangeCartSelectionRequest request) {
        cartService.changeSelection(currentUserProvider.requireUserId(), skuId, request.getSelected());
        return Result.success();
    }

    /** 全选或取消全选。 */
    @PatchMapping("/selection")
    public Result<Void> changeAllSelection(@Valid @RequestBody ChangeCartSelectionRequest request) {
        cartService.changeAllSelection(currentUserProvider.requireUserId(), request.getSelected());
        return Result.success();
    }

    /** 删除指定购物车项。 */
    @DeleteMapping("/items/{skuId}")
    public Result<Void> removeItem(@PathVariable Long skuId) {
        cartService.removeItem(currentUserProvider.requireUserId(), skuId);
        return Result.success();
    }

    /** 删除当前全部已勾选商品。 */
    @DeleteMapping("/selected-items")
    public Result<Void> removeSelectedItems() {
        cartService.removeSelectedItems(currentUserProvider.requireUserId());
        return Result.success();
    }

    /** 查询已勾选商品的结算预览。 */
    @GetMapping("/checkout")
    public Result<CartCheckoutVO> checkout() {
        return Result.success(cartService.checkout(currentUserProvider.requireUserId()));
    }
}
