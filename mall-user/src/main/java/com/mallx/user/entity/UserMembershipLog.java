package com.mallx.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 用户会员开通、续期、取消和到期的审计日志。 */
@Data
@TableName("user_membership_log")
public class UserMembershipLog {
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    private Integer beforeLevelCode;
    private Integer afterLevelCode;
    /** 1 开通、2 升级、3 调整、4 续期、5 取消、6 到期。 */
    private Integer changeType;
    private LocalDateTime beforeExpireAt; // 变更前到期时间
    private LocalDateTime afterExpireAt;  // 变更后到期时间
    /** 系统自动到期时为空。 */
    private Long operatorId;
    private String remark;
    private LocalDateTime createdAt;
}
