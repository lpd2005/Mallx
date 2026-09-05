package com.mallx.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

/** 当前MVP模拟开通或续费VIP的请求参数。 */
@Data
public class ActivateVipMembershipRequest {

    /**
     * 本次购买的VIP月数。
     *
     * <p>客户端只提交时长，实际到期时间由服务端计算，避免客户端直接指定任意到期时间。
     */
    @NotNull(message = "VIP开通月数不能为空")
    @Min(value = 1, message = "VIP开通月数不能少于1个月")
    @Max(value = 36, message = "VIP开通月数不能超过36个月")
    private Integer durationMonths;
}
