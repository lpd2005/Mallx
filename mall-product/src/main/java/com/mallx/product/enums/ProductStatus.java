package com.mallx.product.enums;

import lombok.Getter;

/** SPU和SKU共用的商品状态。 */
@Getter
public enum ProductStatus {
    /** 尚未发布。 */
    DRAFT(0),
    /** 前台可售。 */
    ON_SHELF(1),
    /** 已停止前台销售。 */
    OFF_SHELF(2);

    /** 数据库存储值。 */
    private final int value;

    ProductStatus(int value) {
        this.value = value;
    }

    /** 判断整数值是否对应当前状态。 */
    public boolean matches(Integer value) {
        return value != null && this.value == value;
    }

    /** 校验请求中的状态值是否属于已定义范围。 */
    public static boolean isValid(Integer value) {
        return value != null && value >= DRAFT.value && value <= OFF_SHELF.value;
    }
}
