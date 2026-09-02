package com.mallx.aftersale.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 售后视图；列表不加载流水，详情返回完整流水。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AfterSaleVO {
    private String afterSaleNo;
    private String orderNo;
    private Long userId;
    private Integer type;
    private Integer status;
    private Integer originalOrderStatus;
    private BigDecimal refundAmount;
    private String reason;
    private String auditRemark;
    private Long auditorId;
    private Integer restock;
    private String returnCarrier;
    private String returnTrackingNo;
    private String refundNo;
    private LocalDateTime auditedAt;
    private LocalDateTime returnShippedAt;
    private LocalDateTime receivedAt;
    private LocalDateTime completedAt;
    private LocalDateTime canceledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<AfterSaleLogVO> logs;
}
