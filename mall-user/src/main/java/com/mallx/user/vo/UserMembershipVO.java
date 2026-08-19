package com.mallx.user.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 当前用户实际生效的会员权益。 */
@Data
@Builder
public class UserMembershipVO {
    private Long userId;
    /** 是否存在有效会员。 */
    private Boolean member;
    /** 0 非会员、1 普通会员、2 VIP会员。 */
    private Integer levelCode;
    private String levelName;
    /** 1.00为原价、0.95为95折、0.80为8折。 */
    private BigDecimal discountRate;
    private LocalDateTime startAt;
    private LocalDateTime expireAt;
    /** 0 非会员、1 生效中。 */
    private Integer status;
}
