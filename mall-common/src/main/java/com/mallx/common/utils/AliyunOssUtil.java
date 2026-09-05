package com.mallx.common.utils;

import cn.hutool.core.util.StrUtil;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectMetadata;
import com.mallx.common.oss.AliyunOssConfiguration;
import com.mallx.common.oss.AliyunOssProperties;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

/** OSS图片上传工具：生成随机文件名、上传图片，返回可直接保存和展示的公开URL。 */
@Component
@RequiredArgsConstructor
public class AliyunOssUtil {
    private static final DateTimeFormatter DIRECTORY = DateTimeFormatter.ofPattern("yyyy/MM");
    private final AliyunOssProperties properties;
    /** 从Spring获取配置类创建的OSS客户端；首次获取时创建，之后复用同一个实例。 */
    private final ObjectProvider<OSS> clientProvider;

    /** 恢复简单的上传入口；图片内容已由上传服务校验，不能直接透传未经校验的文件。 */
    public String upload(byte[] content, String originalFilename) {
        String extension =
                StrUtil.subAfter(originalFilename == null ? "" : originalFilename, ".", true)
                        .toLowerCase(Locale.ROOT);
        if (!extension.equals("jpg") && !extension.equals("png")) {
            throw new IllegalArgumentException("仅支持JPEG、PNG图片");
        }
        String objectName =
                LocalDate.now().format(DIRECTORY)
                        + "/"
                        + UUID.randomUUID().toString().replace("-", "")
                        + "."
                        + extension;
        // 先检查访问域名，避免上传成功后才发现无法构造URL。
        String url = buildAccessUrl(objectName);
        if (url.length() > 1024) {
            throw new IllegalStateException("图片访问地址超过数据库1024字符限制，请检查Endpoint配置");
        }
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(extension.equals("jpg") ? "image/jpeg" : "image/png");
            metadata.setContentLength(content.length);
            clientProvider
                    .getObject()
                    .putObject(
                            properties.getBucketName(),
                            objectName,
                            new ByteArrayInputStream(content),
                            metadata);
            return url;
        } catch (Exception exception) {
            throw new IllegalStateException("OSS图片上传失败", exception);
        }
    }

    /** 标准Endpoint加Bucket前缀；自定义域名直接使用，避免重复拼接Bucket。 */
    private String buildAccessUrl(String objectName) {
        AliyunOssConfiguration.validate(properties);
        URI endpoint = URI.create(properties.getEndpoint());
        String host = endpoint.getHost();
        if (host.contains("-internal.")) {
            throw new IllegalStateException("图片需要浏览器直接访问，请使用公网或已绑定的自定义Endpoint，不能使用内网Endpoint");
        }
        if (host.matches("oss-[a-z0-9-]+\\.aliyuncs\\.com")) {
            return "https://" + properties.getBucketName() + "." + host + "/" + objectName;
        }
        return properties.getEndpoint() + "/" + objectName;
    }
}
