package com.mallx.cart.service.impl;

import cn.hutool.json.JSONUtil;
import com.mallx.cart.constant.CartConstants;
import com.mallx.cart.model.RedisCartItem;
import com.mallx.cart.service.CartService;
import com.mallx.cart.support.CartRedisKey;
import com.mallx.cart.vo.CartCheckoutVO;
import com.mallx.cart.vo.CartItemVO;
import com.mallx.cart.vo.CartVO;
import com.mallx.common.enums.ResultCode;
import com.mallx.common.exception.BusinessException;
import com.mallx.inventory.service.InventoryStockService;
import com.mallx.inventory.vo.InventoryStockVO;
import com.mallx.product.enums.ProductStatus;
import com.mallx.product.service.ProductSkuService;
import com.mallx.product.vo.CartProductVO;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Redis购物车实现；Redis只保存数量、勾选状态和加入时间。 */
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {
    private static final long SCRIPT_QUANTITY_EXCEEDED = -1L; //数量超出限制
    private static final long SCRIPT_KINDS_EXCEEDED = -2L;//种类超出限制
    private static final long SCRIPT_ITEM_NOT_FOUND = -3L;//商品不存在
    private static final long TTL_SECONDS = CartConstants.EXPIRE_DURATION.toSeconds();//过期时间

    private static final DefaultRedisScript<Long> ADD_SCRIPT = script("lua/cart_add.lua");
    private static final DefaultRedisScript<Long> UPDATE_QUANTITY_SCRIPT = script("lua/cart_update_quantity.lua");
    private static final DefaultRedisScript<Long> CHANGE_SELECTION_SCRIPT = script("lua/cart_change_selection.lua");
    private static final DefaultRedisScript<Long> CHANGE_ALL_SELECTION_SCRIPT = script("lua/cart_change_all_selection.lua");
    private static final DefaultRedisScript<Long> REMOVE_SELECTED_SCRIPT = script("lua/cart_remove_selected.lua");

    private final StringRedisTemplate redisTemplate;
    private final ProductSkuService productSkuService;
    private final InventoryStockService inventoryStockService;

    /** 查询Redis购物车，并批量补充商品当前价格与库存。 */
    @Override
    public CartVO getCart(Long userId) {
        validateUserId(userId);
        String key = CartRedisKey.cartKey(userId);
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        if (entries.isEmpty()) {
            return emptyCart();
        }
        redisTemplate.expire(key, CartConstants.EXPIRE_DURATION);

        Map<Long, RedisCartItem> redisItems = parseEntries(entries);
        List<Long> skuIds = new ArrayList<>(redisItems.keySet());
        Map<Long, CartProductVO> products = productSkuService.listCartProducts(skuIds).stream()
                .collect(Collectors.toMap(CartProductVO::getSkuId, Function.identity()));
        Map<Long, InventoryStockVO> stocks = inventoryStockService.listStocks(skuIds).stream()
                .collect(Collectors.toMap(InventoryStockVO::getSkuId, Function.identity()));

        List<CartItemVO> items = redisItems.entrySet().stream()
                .map(entry -> buildItem(entry.getKey(), entry.getValue(), products.get(entry.getKey()),
                        stocks.get(entry.getKey())))
                .sorted(Comparator.comparing(CartItemVO::getAddedAt).reversed())
                .toList();
        return summarize(items);
    }

    /** 校验商品和库存后，通过Lua原子累加购物车数量。 */
    @Override
    public void addItem(Long userId, Long skuId, int quantity) {
        validateUserAndSku(userId, skuId);
        validateQuantity(quantity);
        int maxQuantity = currentPurchasableLimit(skuId);
        Long result = redisTemplate.execute(ADD_SCRIPT, List.of(CartRedisKey.cartKey(userId)),
                skuId.toString(), Integer.toString(quantity), Long.toString(System.currentTimeMillis()),
                Integer.toString(maxQuantity), Integer.toString(CartConstants.MAX_ITEM_KINDS),
                Long.toString(TTL_SECONDS));
        checkScriptResult(result);
    }

    /** 校验当前库存后，通过Lua原子覆盖购物车数量。 */
    @Override
    public void updateQuantity(Long userId, Long skuId, int quantity) {
        validateUserAndSku(userId, skuId);
        validateQuantity(quantity);
        int maxQuantity = currentPurchasableLimit(skuId);
        Long result = redisTemplate.execute(UPDATE_QUANTITY_SCRIPT, List.of(CartRedisKey.cartKey(userId)),
                skuId.toString(), Integer.toString(quantity), Integer.toString(maxQuantity),
                Long.toString(TTL_SECONDS));
        checkScriptResult(result);
    }

    /** 删除当前用户指定SKU，不存在时保持幂等成功。 */
    @Override
    public void removeItem(Long userId, Long skuId) {
        validateUserAndSku(userId, skuId);
        String key = CartRedisKey.cartKey(userId);
        redisTemplate.opsForHash().delete(key,skuId.toString());
        refreshOrDeleteEmptyKey(key);
    }

    /** 通过Lua原子更新指定SKU的勾选状态。 */
    @Override
    public void changeSelection(Long userId, Long skuId, boolean selected) {
        validateUserAndSku(userId, skuId);
        Long result = redisTemplate.execute(CHANGE_SELECTION_SCRIPT, List.of(CartRedisKey.cartKey(userId)),
                skuId.toString(), selected ? "1" : "0", Long.toString(TTL_SECONDS));
        checkScriptResult(result);
    }

    /** 通过Lua一次修改当前用户全部购物车项的勾选状态。 */
    @Override
    public void changeAllSelection(Long userId, boolean selected) {
        validateUserId(userId);
        redisTemplate.execute(CHANGE_ALL_SELECTION_SCRIPT, List.of(CartRedisKey.cartKey(userId)),
                selected ? "1" : "0", Long.toString(TTL_SECONDS));
    }

    /** 通过Lua删除当前用户全部已勾选商品。 */
    @Override
    public void removeSelectedItems(Long userId) {
        validateUserId(userId);
        redisTemplate.execute(REMOVE_SELECTED_SCRIPT, List.of(CartRedisKey.cartKey(userId)),
                Long.toString(TTL_SECONDS));
    }

    /** 对已勾选商品重新执行当前价格、上下架和库存校验。 */
    @Override
    public CartCheckoutVO checkout(Long userId) {
        List<CartItemVO> selectedItems = getCart(userId).getItems().stream()
                .filter(CartItemVO::getSelected)
                .toList();
        if (selectedItems.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请先勾选需要结算的商品");
        }
        CartItemVO invalidItem = selectedItems.stream().filter(item -> !item.getPurchasable()).findFirst().orElse(null);
        if (invalidItem != null) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    invalidItem.getSkuId() + "号SKU不可结算：" + invalidItem.getInvalidReason());
        }
        int totalQuantity = selectedItems.stream().mapToInt(CartItemVO::getQuantity).sum();
        BigDecimal productAmount = selectedItems.stream().map(CartItemVO::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return CartCheckoutVO.builder().items(selectedItems).totalQuantity(totalQuantity)
                .productAmount(productAmount).build();
    }

    /** 订单创建成功后批量删除对应SKU；Hash批量删除命令本身具有原子性。 */
    @Override
    public void removePurchasedItems(Long userId, Collection<Long> skuIds) {
        validateUserId(userId);
        if (skuIds == null || skuIds.isEmpty()) {
            return;
        }
        String key = CartRedisKey.cartKey(userId);
        Object[] fields = skuIds.stream().filter(skuId -> skuId != null && skuId > 0)
                .distinct().map(String::valueOf).toArray();
        if (fields.length == 0) {
            return;
        }
        redisTemplate.opsForHash().delete(key, fields);
        refreshOrDeleteEmptyKey(key);
    }


    private int currentPurchasableLimit(Long skuId) {
        List<CartProductVO> products = productSkuService.listCartProducts(List.of(skuId));
        if (products.isEmpty()) {
            throw new BusinessException(ResultCode.NOT_FOUND, "SKU不存在");
        }
        CartProductVO product = products.get(0);
        if (!ProductStatus.ON_SHELF.matches(product.getSpuStatus())
                || !ProductStatus.ON_SHELF.matches(product.getSkuStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "商品未上架，不能加入或修改购物车");
        }
        InventoryStockVO stock = inventoryStockService.listStocks(List.of(skuId)).stream().findFirst().orElse(null);
        if (stock == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "SKU库存尚未初始化");
        }
        if (stock.getAvailableStock() <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "商品已无库存");
        }
        return Math.min(CartConstants.MAX_ITEM_QUANTITY, stock.getAvailableStock());
    }

    private Map<Long, RedisCartItem> parseEntries(Map<Object, Object> entries) {
        Map<Long, RedisCartItem> items = new LinkedHashMap<>();
        try {
            entries.forEach((field, value) -> items.put(Long.valueOf(field.toString()),
                    JSONUtil.toBean(value.toString(), RedisCartItem.class)));
        } catch (RuntimeException exception) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "购物车数据解析失败");
        }
        return items;
    }

    private CartItemVO buildItem(Long skuId, RedisCartItem redisItem, CartProductVO product,
                                 InventoryStockVO stock) {
        String invalidReason = invalidReason(redisItem, product, stock);
        BigDecimal subtotal = product == null || product.getSalePrice() == null
                ? BigDecimal.ZERO : product.getSalePrice().multiply(BigDecimal.valueOf(redisItem.getQuantity()));
        return CartItemVO.builder()
                .skuId(skuId)
                .spuId(product == null ? null : product.getSpuId())
                .productName(product == null ? null : product.getProductName())
                .skuName(product == null ? null : product.getSkuName())
                .specJson(product == null ? null : product.getSpecJson())
                .imageOssKey(product == null ? null : product.getImageOssKey())
                .salePrice(product == null ? null : product.getSalePrice())
                .marketPrice(product == null ? null : product.getMarketPrice())
                .quantity(redisItem.getQuantity())
                .selected(Boolean.TRUE.equals(redisItem.getSelected()))
                .availableStock(stock == null ? null : stock.getAvailableStock())
                .purchasable(invalidReason == null)
                .invalidReason(invalidReason)
                .subtotal(subtotal)
                .addedAt(redisItem.getAddedAt())
                .build();
    }

    private String invalidReason(RedisCartItem item, CartProductVO product, InventoryStockVO stock) {
        if (product == null) return "商品不存在";
        if (!ProductStatus.ON_SHELF.matches(product.getSpuStatus())) return "商品已下架";
        if (!ProductStatus.ON_SHELF.matches(product.getSkuStatus())) return "SKU已下架";
        if (stock == null) return "库存尚未初始化";
        if (stock.getAvailableStock() == null || stock.getAvailableStock() <= 0) return "商品已无库存";
        if (item.getQuantity() == null || item.getQuantity() <= 0) return "购物车数量不正确";
        if (item.getQuantity() > stock.getAvailableStock()) return "可用库存不足";
        return null;
    }

    private CartVO summarize(List<CartItemVO> items) {
        int totalQuantity = items.stream().mapToInt(CartItemVO::getQuantity).sum();
        List<CartItemVO> selectedPurchasable = items.stream()
                .filter(item -> item.getSelected() && item.getPurchasable()).toList();
        int selectedQuantity = selectedPurchasable.stream().mapToInt(CartItemVO::getQuantity).sum();
        BigDecimal selectedAmount = selectedPurchasable.stream().map(CartItemVO::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return CartVO.builder()
                .items(items)
                .itemKinds(items.size())
                .totalQuantity(totalQuantity)
                .selectedQuantity(selectedQuantity)
                .selectedAmount(selectedAmount)
                .build();
    }

    private CartVO emptyCart() {
        return CartVO.builder().items(List.of()).itemKinds(0).totalQuantity(0)
                .selectedQuantity(0).selectedAmount(BigDecimal.ZERO).build();
    }

    private void refreshOrDeleteEmptyKey(String key) {
        Long size = redisTemplate.opsForHash().size(key);
        if (size == null || size == 0) {
            redisTemplate.delete(key);
        } else {
            redisTemplate.expire(key, CartConstants.EXPIRE_DURATION);
        }
    }

    private void checkScriptResult(Long result) {
        if (result == null) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "购物车Redis操作失败");
        }
        if (result == SCRIPT_QUANTITY_EXCEEDED) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "购物车数量超过当前库存或购买上限");
        }
        if (result == SCRIPT_KINDS_EXCEEDED) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "购物车最多保存100种商品");
        }
        if (result == SCRIPT_ITEM_NOT_FOUND) {
            throw new BusinessException(ResultCode.NOT_FOUND, "购物车中不存在该SKU");
        }
    }

    private void validateQuantity(int quantity) {
        if (quantity <= 0 || quantity > CartConstants.MAX_ITEM_QUANTITY) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "购物车数量必须在1至99之间");
        }
    }

    private void validateUserAndSku(Long userId, Long skuId) {
        validateUserId(userId);
        if (skuId == null || skuId <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "SKU ID不正确");
        }
    }

    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "当前用户身份无效");
        }
    }

    private static DefaultRedisScript<Long> script(String path) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource(path));
        script.setResultType(Long.class);
        return script;
    }
}
