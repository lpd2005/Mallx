package com.mallx.common.utils;

import cn.hutool.core.util.StrUtil;

import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.OSS;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.ObjectMetadata;
import com.mallx.common.oss.AliyunOssConfiguration;
import com.mallx.common.oss.AliyunOssProperties;
import com.mallx.common.oss.ImageUploadVO;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.Date;

/** OSS存储工具；验证和Key生成由ImageUploadService处理，客户端由Spring统一释放。 */
@Component
@RequiredArgsConstructor
public class AliyunOssUtil {
    private final AliyunOssProperties properties;
    private final ObjectProvider<OSS> clientProvider;

    /** 只接收服务端生成的Key及已验证、重新编码的图片，不能透传客户端的任意Key。 */
    public ImageUploadVO upload(byte[] content, String objectKey, String contentType) {
        try {
            // 提前生成展示链接，配置/签名失败时不写对象。签名生成不查询OSS。
            ImageUploadVO result = accessUrl(objectKey);
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(contentType);
            metadata.setContentLength(content.length);
            clientProvider
                    .getObject()
                    .putObject(
                            properties.getBucketName(),
                            objectKey,
                            new ByteArrayInputStream(content),
                            metadata);
            return result;
        } catch (Exception exception) {
            // 全局异常处理返回HTTP500，不向前端暴露密钥、签名或SDK错误细节。
            throw new IllegalStateException("OSS图片上传失败", exception);
        }
    }

    /** 调用方必须先检查Key所属目录和用户权限，不能提供任意对象的公开签名入口。 */
    public ImageUploadVO accessUrl(String objectKey) {
        AliyunOssConfiguration.validate(properties);
        if (StrUtil.isNotBlank(properties.getPublicBaseUrl())) {
            String baseUrl = properties.getPublicBaseUrl().replaceAll("/+$", "");
            return ImageUploadVO.builder()
                    .objectKey(objectKey)
                    .url(baseUrl + "/" + objectKey)
                    .build();
        }
        Instant expiresAt = Instant.now().plusSeconds(properties.getSignedUrlSeconds());
        GeneratePresignedUrlRequest request =
                new GeneratePresignedUrlRequest(
                        properties.getBucketName(), objectKey, HttpMethod.GET);
        request.setExpiration(Date.from(expiresAt));
        return ImageUploadVO.builder()
                .objectKey(objectKey)
                .expiresAt(expiresAt)
                .url(clientProvider.getObject().generatePresignedUrl(request).toString())
                .build();
    }
}
