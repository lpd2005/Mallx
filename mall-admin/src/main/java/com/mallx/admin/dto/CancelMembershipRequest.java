package com.mallx.admin.dto;

import jakarta.validation.constraints.Size;

import lombok.Data;

/** 管理员取消用户全部会员权益的请求参数。 */
@Data
public class CancelMembershipRequest {

    /** 取消原因会写入会员变更日志；空值表示未填写原因。 */
    @Size(max = 255, message = "取消原因长度不能超过255位")
    private String remark;
}
