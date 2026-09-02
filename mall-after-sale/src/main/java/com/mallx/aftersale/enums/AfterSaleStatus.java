package com.mallx.aftersale.enums;

import lombok.Getter;

/** 售后单状态；模拟退款同步完成，不设置没有真实异步任务的REFUNDING状态。 */
@Getter
public enum AfterSaleStatus {
//    '售后状态：1待审核，2待退款，3待寄回，4待验收，5已完成，6已驳回，7已撤销',
    WAIT_AUDIT(1),
    APPROVED(2),
    WAIT_RETURN(3),
    WAIT_RECEIVE(4),
    COMPLETED(5),
    REJECTED(6),
    CANCELED(7);

    private final int value;

    AfterSaleStatus(int value) {
        this.value = value;
    }

    public boolean matches(Integer value) {
        return value != null && this.value == value;
    }

    public static boolean supports(Integer value) {
        for (AfterSaleStatus status : values()) {
            if (status.matches(value)) {
                return true;
            }
        }
        return false;
    }
}
