package com.mallx.aftersale.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AfterSaleLogVO {
    private Integer beforeStatus;
    private Integer afterStatus;
    private Integer operationType;
    private Long operatorId;
    private String remark;
    private LocalDateTime createdAt;
}
