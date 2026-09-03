package com.mallx.admin.entity;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

import java.time.LocalDateTime;

/** 成功后台变更的审计流水，与业务修改在同一事务提交；不记录密码或请求正文。 */
@Data
@TableName("admin_operation_log")
public class AdminOperationLog {
    /** 流水主键。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 真实登录操作人。 */
    private Long operatorId;

    /** 稳定操作编码，例如product:create-spu。 */
    private String action;

    /** 业务对象ID/单号，不存手机号等个人资料。 */
    private String target;

    /** 服务端生成的时间。 */
    private LocalDateTime createdAt;
}
