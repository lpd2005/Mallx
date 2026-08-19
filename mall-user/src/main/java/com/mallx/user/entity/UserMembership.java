package com.mallx.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 用户当前会员记录；不存在有效记录时用户就是非会员。 */
@Data
@TableName("user_membership")
public class UserMembership {
    /** 一个用户最多保留一条当前会员记录。 */
    @TableId(value = "user_id", type = IdType.INPUT)
    private Long userId;
    /** 会员等级：1 普通会员、2 VIP会员；非会员不会写入数据库。 */
    private Integer levelCode;
    /** 本次会员生效时间。 */
    private LocalDateTime startAt;
    /** 普通会员可为空；VIP必须有到期时间。 */
    private LocalDateTime expireAt;
    /** 1 生效中、2 已过期、3 已取消。 */
    private Integer status;
    /** VIP覆盖前的基础会员等级；只有普通会员升级VIP时保存值1。 */
    private Integer fallbackLevelCode;
    /** VIP到期恢复普通会员时使用的原普通会员生效时间。 */
    private LocalDateTime fallbackStartAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
