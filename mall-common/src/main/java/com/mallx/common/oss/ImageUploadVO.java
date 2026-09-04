package com.mallx.common.oss;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/** 图片上传或刷新地址的结果；业务表只保存objectKey。 */
@Data
@Builder
public class ImageUploadVO {
    /** OSS对象Key，不含Bucket和域名。 */
    private String objectKey;

    /** 展示URL，不应保存到业务表。 */
    private String url;

    /** 签名到期时间；公开图片/CDN链接为null。 */
    private Instant expiresAt;
}
