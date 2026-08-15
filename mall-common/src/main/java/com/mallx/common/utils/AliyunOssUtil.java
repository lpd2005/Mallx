package com.mallx.common.utils;

import cn.hutool.core.util.StrUtil;
import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.CredentialsProviderFactory;
import com.aliyun.oss.common.auth.EnvironmentVariableCredentialsProvider;
import com.aliyun.oss.common.comm.SignVersion;
import com.mallx.common.enums.ResultCode;
import com.mallx.common.exception.BusinessException;
import com.mallx.common.oss.AliyunOssProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 阿里云 OSS 文件上传工具。
 *
 * <p>对象按 {@code yyyy/MM} 归档，文件名使用 UUID，避免用户上传的同名文件相互覆盖。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AliyunOssUtil {

    private static final DateTimeFormatter DIRECTORY_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM");

    /** 从配置文件读取的 OSS 非敏感连接参数。 */
    private final AliyunOssProperties properties;

    /**
     * 上传文件并返回其访问地址。
     *
     * @param content 文件二进制内容
     * @param originalFilename 用户上传的原始文件名
     * @return 已上传文件的访问地址
     */
    public String upload(byte[] content, String originalFilename) throws Exception {
        validateConfiguration();
        String objectName = buildObjectName(originalFilename);
        EnvironmentVariableCredentialsProvider credentialsProvider =
                CredentialsProviderFactory.newEnvironmentVariableCredentialsProvider();

        ClientBuilderConfiguration clientConfiguration = new ClientBuilderConfiguration();
        clientConfiguration.setSignatureVersion(SignVersion.V4);
        OSS ossClient = OSSClientBuilder.create()
                .endpoint(properties.getEndpoint())
                .credentialsProvider(credentialsProvider)
                .clientConfiguration(clientConfiguration)
                .region(properties.getRegion())
                .build();

        try {
            ossClient.putObject(properties.getBucketName(), objectName, new ByteArrayInputStream(content));
            return buildAccessUrl(objectName);
        } catch (Exception exception) {
            log.error("Failed to upload file to OSS, objectName={}", objectName, exception);
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "文件上传失败");
        } finally {
            ossClient.shutdown();
        }
    }

    /**
     * 以日期目录与随机文件名生成对象键，仅保留原始文件的扩展名。
     */
    private String buildObjectName(String originalFilename) {
        String extension = StrUtil.emptyToDefault(
                StrUtil.subAfter(originalFilename == null ? "" : originalFilename, ".", true), "");
        String filename = UUID.randomUUID().toString().replace("-", "");
        String objectFilename = extension.isBlank() ? filename : filename + "." + extension;
        return LocalDate.now().format(DIRECTORY_FORMATTER) + "/" + objectFilename;
    }

    /**
     * 根据 endpoint、Bucket 与对象键组合标准访问地址。
     */
    private String buildAccessUrl(String objectName) {
        String endpoint = properties.getEndpoint();
        String scheme = endpoint.startsWith("https://") ? "https://" : "http://";
        String host = endpoint.replaceFirst("^https?://", "");
        return scheme + properties.getBucketName() + "." + host + "/" + objectName;
    }

    /**
     * 在发起网络调用前校验本机私有配置是否填写完整。
     */
    private void validateConfiguration() {
        if (StrUtil.hasBlank(properties.getEndpoint(), properties.getBucketName(), properties.getRegion())) {
            throw new IllegalStateException("阿里云 OSS 配置不完整，请检查 application-local.yml");
        }
    }
}
