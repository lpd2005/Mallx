package com.mallx.aftersale.enums;

import lombok.Getter;

/** 售后操作流水的数据库数值。 */
@Getter
public enum AfterSaleOperationType {
//    '操作类型：1申请，2通过，3驳回，4寄回，5验收，6退款，7撤销',
    APPLY(1),
    APPROVE(2),
    REJECT(3),
    RETURN_SHIP(4),
    RECEIVE(5),
    REFUND(6),
    CANCEL(7);

    private final int value;

    AfterSaleOperationType(int value) {
        this.value = value;
    }
}
