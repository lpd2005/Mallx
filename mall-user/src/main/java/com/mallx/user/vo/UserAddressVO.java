package com.mallx.user.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/** 当前用户收货地址响应。 */
@Data
@Builder
public class UserAddressVO {
    /** 地址ID。 */
    private Long id;
    /** 收货人姓名。 */
    private String recipientName;
    /** 收货人联系电话。 */
    private String recipientMobile;
    /** 省级行政区编码。 */
    private String provinceCode;
    /** 省级行政区名称。 */
    private String provinceName;
    /** 市级行政区编码。 */
    private String cityCode;
    /** 市级行政区名称。 */
    private String cityName;
    /** 区县行政区编码。 */
    private String districtCode;
    /** 区县行政区名称。 */
    private String districtName;
    /** 街道、门牌号等详细地址。 */
    private String detailAddress;
    /** 邮政编码。 */
    private String postalCode;
    /** 用户自定义地址标签。 */
    private String addressTag;
    /** 是否为当前默认地址。 */
    private boolean defaultAddress;
    /** 地址创建时间。 */
    private LocalDateTime createdAt;
    /** 地址最后更新时间。 */
    private LocalDateTime updatedAt;
}
