package com.mallx.common.oss;

import cn.hutool.core.util.StrUtil;

import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.CredentialsProviderFactory;
import com.aliyun.oss.common.comm.SignVersion;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.net.URI;

/** 延迟创建可复用OSS客户端，不配置OSS也不影响其他模块启动。 */
@Configuration
public class AliyunOssConfiguration {
    @Bean(destroyMethod = "shutdown")
    @Lazy
    public OSS aliyunOssClient(AliyunOssProperties properties) {
        validate(properties);
        ClientBuilderConfiguration configuration = new ClientBuilderConfiguration();
        configuration.setSignatureVersion(SignVersion.V4);
        configuration.setSupportCname(true);
        configuration.setConnectionTimeout(5000);
        configuration.setSocketTimeout(15000);
        configuration.setConnectionRequestTimeout(5000);
        configuration.setMaxConnections(50);
        return OSSClientBuilder.create()
                .endpoint(properties.getEndpoint())
                .region(properties.getRegion())
                .credentialsProvider(
                        CredentialsProviderFactory.newDefaultCredentialProvider(
                                properties.getAccessKeyId(), properties.getAccessKeySecret()))
                .clientConfiguration(configuration)
                .build();
    }

    /** 实际使用时检查配置，防止返回不安全或公网不可用的图片链接。 */
    public static void validate(AliyunOssProperties properties) {
        if (StrUtil.hasBlank(
                properties.getEndpoint(),
                properties.getBucketName(),
                properties.getRegion(),
                properties.getAccessKeyId(),
                properties.getAccessKeySecret())) {
            throw new IllegalStateException(
                    "OSS配置不完整，请配置mall.oss的endpoint、bucket-name、region、access-key-id和access-key-secret");
        }
        if (isPlaceholder(properties.getAccessKeyId())
                || isPlaceholder(properties.getAccessKeySecret())) {
            throw new IllegalStateException("OSS访问凭证仍是占位符，请在application-local.yml中填写真实值");
        }
        requireHttpsEndpoint(properties.getEndpoint());
    }

    /** 拒绝把%NAME%或${NAME}一类未解析占位符当作真实凭证发送给OSS。 */
    private static boolean isPlaceholder(String value) {
        String trimmed = value.trim();
        return (trimmed.startsWith("%") && trimmed.endsWith("%"))
                || (trimmed.startsWith("${") && trimmed.endsWith("}"));
    }

    /** 校验上传及图片访问共用的Endpoint，不修改云端访问权限。 */
    private static void requireHttpsEndpoint(String value) {
        URI uri = URI.create(value);
        if (!"https".equalsIgnoreCase(uri.getScheme())
                || uri.getHost() == null
                || uri.getUserInfo() != null
                || uri.getQuery() != null
                || uri.getFragment() != null
                || (uri.getPath() != null && !uri.getPath().isEmpty())) {
            throw new IllegalStateException("OSS地址必须为HTTPS且不能包含凭证、查询参数或片段；Endpoint不能带路径或末尾斜杠");
        }
    }
}
