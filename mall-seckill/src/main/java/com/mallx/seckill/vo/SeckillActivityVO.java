package com.mallx.seckill.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** 秒杀活动及其商品列表。 */
@Data
@Builder
public class SeckillActivityVO {
    /** 活动 ID。 */
    private Long activityId;
    /** 活动展示名称。 */
    private String activityName;
    /** 活动开始时间。 */
    private LocalDateTime startAt;
    /** 活动结束时间。 */
    private LocalDateTime endAt;
    /** 生命周期状态：0草稿、1已预热、2已关闭。 */
    private Integer status;
    /** 活动中的秒杀商品。 */
    private List<SeckillItemVO> items;
}
