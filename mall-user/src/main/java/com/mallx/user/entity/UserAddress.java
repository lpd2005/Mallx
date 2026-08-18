package com.mallx.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 用户收货地址。订单创建时应复制地址快照，不能长期引用本表。 */
@Data
@TableName("user_address")
public class UserAddress {

    /** 地址主键，由 MyBatis-Plus 雪花算法生成。 */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    /** 地址所属用户。 */
    private Long userId;
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
    /** 邮政编码，可为空。 */
    private String postalCode;
    /** 用户自定义地址标签，例如“家”或“公司”。 */
    private String addressTag;
    /** 是否为默认地址：0 否、1 是。 */
    private Integer isDefault;
    /** 逻辑删除标记：0 有效、1 已删除。 */
    private Integer deleted;
    /** 地址创建时间。 */
    private LocalDateTime createdAt;
    /** 地址最后更新时间。 */
    private LocalDateTime updatedAt;
}
