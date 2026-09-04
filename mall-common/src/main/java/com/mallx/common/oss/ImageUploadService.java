package com.mallx.common.oss;

import com.mallx.common.enums.ResultCode;
import com.mallx.common.exception.BusinessException;
import com.mallx.common.utils.AliyunOssUtil;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;

/** 商品图片与头像共用的上传服务；不自动修改业务表或删除旧对象。 */
@Service
@RequiredArgsConstructor
public class ImageUploadService {
    public static final int MAX_IMAGE_BYTES = 5 * 1024 * 1024;
    private static final long MAX_PIXELS = 16_000_000L;
    private static final DateTimeFormatter DIRECTORY = DateTimeFormatter.ofPattern("yyyy/MM");
    private static final String KEY_SUFFIX = "[0-9]{4}/(0[1-9]|1[0-2])/[a-f0-9]{32}\\.(jpg|png)";
    private final AliyunOssUtil oss;

    /** 商品、品牌、分类图片共用商品目录，操作人ID来自登录上下文。 */
    public ImageUploadVO uploadProductImage(MultipartFile file, Long operatorId) {
        return upload(file, "products/" + requireUserId(operatorId) + "/");
    }

    /** 头像按用户ID隔离，客户端不能指定其他人的目录。 */
    public ImageUploadVO uploadAvatar(MultipartFile file, Long userId) {
        return upload(file, "avatars/" + requireUserId(userId) + "/");
    }

    /** 调用此方法前必须经过后台商品管理权限校验。 */
    public ImageUploadVO productImageUrl(String objectKey) {
        requireKey(objectKey, "products/[1-9][0-9]*/" + KEY_SUFFIX);
        return oss.accessUrl(objectKey);
    }

    /** 只允许刷新当前用户自己的头像链接，不为任意OSS对象签名。 */
    public ImageUploadVO avatarUrl(String objectKey, Long userId) {
        requireKey(objectKey, "avatars/" + requireUserId(userId) + "/" + KEY_SUFFIX);
        return oss.accessUrl(objectKey);
    }

    private ImageUploadVO upload(MultipartFile file, String prefix) {
        if (file == null || file.isEmpty()) throw badRequest("请选择非空图片");
        if (file.getSize() > MAX_IMAGE_BYTES) throw badRequest("图片不能超过5MB");
        byte[] content;
        try (InputStream input = file.getInputStream()) {
            // 即使MultipartFile提供的大小不可信，读取也不会无上限分配内存。
            content = input.readNBytes(MAX_IMAGE_BYTES + 1);
        } catch (IOException exception) {
            throw badRequest("图片读取失败，请重新上传");
        }
        if (content.length == 0 || content.length > MAX_IMAGE_BYTES)
            throw badRequest("图片大小必须为1字节到5MB");
        try (MemoryCacheImageInputStream input =
                new MemoryCacheImageInputStream(new ByteArrayInputStream(content))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw badRequest("仅支持有效的JPEG、PNG图片");
            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!format.equals("jpeg") && !format.equals("png"))
                    throw badRequest("仅支持JPEG、PNG图片");
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                // 先读尺寸再解码，限制高压缩率图片解码后的内存占用。
                if (width <= 0
                        || height <= 0
                        || width > 8192
                        || height > 8192
                        || (long) width * height > MAX_PIXELS) {
                    throw badRequest("图片宽高不能超过8192，且总像素不能超过1600万");
                }
                BufferedImage image = reader.read(0);
                if (image == null) throw badRequest("图片内容无效");
                try (LimitedImageOutput output = new LimitedImageOutput()) {
                    // 重新编码移除附加数据及原始元数据，不信任文件名和客户端Content-Type。
                    if (!ImageIO.write(image, format, output)) throw badRequest("图片格式不受支持");
                    String extension = format.equals("jpeg") ? "jpg" : "png";
                    String key =
                            prefix
                                    + LocalDate.now().format(DIRECTORY)
                                    + "/"
                                    + UUID.randomUUID().toString().replace("-", "")
                                    + "."
                                    + extension;
                    return oss.upload(output.toByteArray(), key, "image/" + format);
                } finally {
                    image.flush();
                }
            } finally {
                reader.dispose();
            }
        } catch (IOException exception) {
            throw badRequest("图片损坏或编码后超过5MB，请压缩后重试");
        }
    }

    private Long requireUserId(Long userId) {
        if (userId == null || userId <= 0) throw badRequest("用户ID无效");
        return userId;
    }

    private void requireKey(String objectKey, String pattern) {
        if (objectKey == null || objectKey.length() > 255 || !objectKey.matches(pattern)) {
            throw badRequest("图片Key不属于允许访问的目录或格式不正确");
        }
    }

    private static BusinessException badRequest(String message) {
        return new BusinessException(ResultCode.BAD_REQUEST, message);
    }

    /** 输出也限制大小，不能只检查上传原图大小。 */
    private static class LimitedImageOutput extends ByteArrayOutputStream {
        @Override
        public synchronized void write(int value) {
            checkSize(1);
            super.write(value);
        }

        @Override
        public synchronized void write(byte[] bytes, int offset, int length) {
            checkSize(length);
            super.write(bytes, offset, length);
        }

        private void checkSize(int additional) {
            if ((long) count + additional > MAX_IMAGE_BYTES) throw badRequest("图片编码后超过5MB，请压缩后重试");
        }
    }
}
