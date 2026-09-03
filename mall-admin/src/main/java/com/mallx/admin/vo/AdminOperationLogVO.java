package com.mallx.admin.vo;

import lombok.*;

import java.time.*;

/** 后台成功变更记录，不提供修改或删除接口。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminOperationLogVO {
    /** 流水ID。 */
    private Long id;

    /** 操作人ID。 */
    private Long operatorId;

    /** 操作编码。 */
    private String action;

    /** 业务ID或单号。 */
    private String target;

    /** 时间。 */
    private LocalDateTime createdAt;
}
