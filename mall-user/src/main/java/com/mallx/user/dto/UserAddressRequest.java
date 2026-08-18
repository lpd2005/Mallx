package com.mallx.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 新增或完整更新收货地址的请求。 */
@Data
public class UserAddressRequest {

    /** 收货人姓名。 */
    @NotBlank(message = "收货人姓名不能为空")
    @Size(max = 50, message = "收货人姓名长度不能超过 50 位")
    private String recipientName;

    /** 收货人联系电话，允许国际区号、空格和连字符。 */
    @NotBlank(message = "收货人手机号不能为空")
    @Pattern(regexp = "^\\+?[0-9 -]{6,20}$", message = "收货人手机号格式不正确")
    private String recipientMobile;

    /** 省级行政区编码。 */
    @NotBlank(message = "省级行政区编码不能为空")
    @Size(max = 12, message = "省级行政区编码长度不能超过 12 位")
    private String provinceCode;

    /** 省级行政区名称，用于展示和订单地址快照。 */
    @NotBlank(message = "省级行政区名称不能为空")
    @Size(max = 32, message = "省级行政区名称长度不能超过 32 位")
    private String provinceName;

    /** 市级行政区编码。 */
    @NotBlank(message = "市级行政区编码不能为空")
    @Size(max = 12, message = "市级行政区编码长度不能超过 12 位")
    private String cityCode;

    /** 市级行政区名称。 */
    @NotBlank(message = "市级行政区名称不能为空")
    @Size(max = 32, message = "市级行政区名称长度不能超过 32 位")
    private String cityName;

    /** 区县行政区编码。 */
    @NotBlank(message = "区县行政区编码不能为空")
    @Size(max = 12, message = "区县行政区编码长度不能超过 12 位")
    private String districtCode;

    /** 区县行政区名称。 */
    @NotBlank(message = "区县行政区名称不能为空")
    @Size(max = 32, message = "区县行政区名称长度不能超过 32 位")
    private String districtName;

    /** 街道、门牌号等详细地址。 */
    @NotBlank(message = "详细地址不能为空")
    @Size(max = 255, message = "详细地址长度不能超过 255 位")
    private String detailAddress;

    /** 邮政编码，可为空。 */
    @Size(max = 12, message = "邮政编码长度不能超过 12 位")
    private String postalCode;

    /** 用户自定义标签，例如“家”或“公司”。 */
    @Size(max = 20, message = "地址标签长度不能超过 20 位")
    private String addressTag;

    /** 是否同时设为默认地址；为空或 false 时不主动切换默认地址。 */
    private Boolean defaultAddress;
}
