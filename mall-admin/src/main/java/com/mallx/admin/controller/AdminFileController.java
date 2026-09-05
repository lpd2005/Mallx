package com.mallx.admin.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;

import com.mallx.common.oss.ImageUploadService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** 后台图片上传入口；保留后台准入和商品管理权限，不允许游客上传。 */
@RestController
@RequestMapping("/admin/files/images")
@RequiredArgsConstructor
@SaCheckPermission("product:manage")
public class AdminFileController {
    private final ImageUploadService imageUploadService;

    /** file为图片文件；成功直接返回URL字符串，保存商品时写入对应的OssKey字段。 */
    @PostMapping(
            value = {"", "/upload"},
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String upload(@RequestPart("file") MultipartFile file) {
        return imageUploadService.upload(file);
    }
}
