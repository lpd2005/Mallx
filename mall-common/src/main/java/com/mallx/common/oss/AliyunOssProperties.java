package com.mallx.common.oss;

import lombok.Data;
import lombok.ToString;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 阿里云 OSS 配置，由 {@code mallx.aliyun.oss} 前缀统一绑定。
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

    /** 阿里云访问密钥ID，从YAML配置注入。 */
    @ToString.Exclude
    private String accessKeyId;

    /** 阿里云访问密钥Secret，从YAML配置注入，禁止记录到日志。 */
    @ToString.Exclude
    private String accessKeySecret;

}
