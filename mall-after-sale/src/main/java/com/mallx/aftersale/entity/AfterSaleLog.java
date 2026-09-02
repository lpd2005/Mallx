package com.mallx.aftersale.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

import java.time.LocalDateTime;

/** 售后每次状态变更的审计流水。 */
@Data
@TableName("after_sale_log")
public class AfterSaleLog {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String afterSaleNo;

    /** 创建申请时原状态为空。 */
    private Integer beforeStatus;

    private Integer afterStatus;
    private Integer operationType;
    private Long operatorId;
    private String remark;
    private LocalDateTime createdAt;
}
