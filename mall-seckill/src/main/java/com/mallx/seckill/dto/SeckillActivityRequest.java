package com.mallx.seckill.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;

import java.time.LocalDateTime;

/** 创建或修改秒杀活动。 */
@Data
public class SeckillActivityRequest {
    /** 活动名称，仅用于运营后台及前台展示。 */
    @NotBlank(message = "活动名称不能为空")
    @Size(max = 100, message = "活动名称不能超过100个字符")
    private String activityName;

    /** 抢购开始时间。 */
    @NotNull(message = "活动开始时间不能为空")
    private LocalDateTime startAt;

    /** 抢购结束时间，业务层额外校验其必须晚于开始时间。 */
    @NotNull(message = "活动结束时间不能为空")
    private LocalDateTime endAt;
}
