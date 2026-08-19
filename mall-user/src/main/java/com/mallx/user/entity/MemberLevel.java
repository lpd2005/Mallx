package com.mallx.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 会员等级及折扣配置；当前数据库只配置普通会员和VIP会员。 */
@Data
@TableName("member_level")
public class MemberLevel {
    /** 会员业务编码，同时作为配置表主键。 */
    @TableId(value = "level_code", type = IdType.INPUT)
    private Integer levelCode;
    /** 会员展示名称。 */
    private String levelName;
    /** 商品折扣率，例如0.9500代表95折。 */
    private BigDecimal discountRate;
    /** 等级优先级。 */
    private Integer priority;
    /** 是否允许新开通：0 否、1 是。 */
    private Integer enabled;
    /** 权益说明。 */
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
