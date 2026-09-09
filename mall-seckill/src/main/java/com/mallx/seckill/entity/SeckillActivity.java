package com.mallx.seckill.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

import java.time.LocalDateTime;

/** 秒杀活动主记录。 */
@Data
@TableName("seckill_activity")
public class SeckillActivity {
    /** 活动主键，使用 MyBatis-Plus 雪花 ID。 */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    /** 后台展示的活动名称。 */
    private String activityName;
    /** 允许资格预扣的开始时间。 */
    private LocalDateTime startAt;
    /** 停止资格预扣的结束时间。 */
    private LocalDateTime endAt;
    /** 0草稿、1已预热、2已关闭；是否进行中由时间窗口判断。 */
    private Integer status;
    /** 创建时间，由数据库默认值维护。 */
    private LocalDateTime createdAt;
    /** 最后更新时间，由数据库自动更新。 */
    private LocalDateTime updatedAt;
}
