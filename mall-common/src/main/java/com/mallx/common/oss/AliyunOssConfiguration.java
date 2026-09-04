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
    public OSS aliyunOssClient(AliyunOssProperties properties) throws Exception {
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
                        CredentialsProviderFactory.newEnvironmentVariableCredentialsProvider())
                .clientConfiguration(configuration)
                .build();
    }

    /** 实际使用时检查配置，防止返回不安全或公网不可用的图片链接。 */
    public static void validate(AliyunOssProperties properties) {
        if (StrUtil.hasBlank(
                properties.getEndpoint(), properties.getBucketName(), properties.getRegion())) {
            throw new IllegalStateException("OSS配置不完整，请配置mall.oss的endpoint、bucket-name和region");
        }
        URI endpoint = requireHttpsUrl(properties.getEndpoint(), false);
        if (properties.getSignedUrlSeconds() < 60 || properties.getSignedUrlSeconds() > 3600) {
            throw new IllegalStateException("OSS签名链接有效期必须为60到3600秒");
        }
        if (StrUtil.isNotBlank(properties.getPublicBaseUrl())) {
            requireHttpsUrl(properties.getPublicBaseUrl(), true);
        } else if (endpoint.getHost().contains("-internal.")) {
            throw new IllegalStateException("私有图片签名链接需使用公网或自定义域名，不能使用内网Endpoint");
        }
    }

    private static URI requireHttpsUrl(String value, boolean allowPath) {
        URI uri = URI.create(value);
        if (!"https".equalsIgnoreCase(uri.getScheme())
                || uri.getHost() == null
                || uri.getUserInfo() != null
                || uri.getQuery() != null
                || uri.getFragment() != null
                || (!allowPath && uri.getPath() != null && !uri.getPath().isEmpty())) {
            throw new IllegalStateException("OSS地址必须为HTTPS且不能包含凭证、查询参数或片段；Endpoint不能带路径或末尾斜杠");
        }
        return uri;
    }
}
