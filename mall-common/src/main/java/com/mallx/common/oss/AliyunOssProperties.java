package com.mallx.common.oss;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 阿里云 OSS 的非敏感连接配置。
 *
 * <p>访问凭证由 {@code OSS_ACCESS_KEY_ID} 和 {@code OSS_ACCESS_KEY_SECRET} 环境变量提供，不写入配置文件。
 */
@Data
@Component
@ConfigurationProperties(prefix = "mallx.aliyun.oss")
public class AliyunOssProperties {

    /** OSS 服务端点，例如 https://oss-cn-hangzhou.aliyuncs.com。 */
    private String endpoint;

    /** 用于存放 MallX 文件的 Bucket 名称。 */
    private String bucketName;

    /** Bucket 所在地域，例如 cn-hangzhou。 */
    private String region;

    /** 公开图片/CDN地址；留空使用私有Bucket限时签名链接，不改变Bucket ACL。 */
    private String publicBaseUrl;

    /** 签名链接有效秒数，限制60～3600秒。 */
    private int signedUrlSeconds = 900;
}
