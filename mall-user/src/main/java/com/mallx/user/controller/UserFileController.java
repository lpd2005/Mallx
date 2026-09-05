package com.mallx.user.controller;

import com.mallx.common.oss.ImageUploadService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** 登录用户上传头像；公开图片没有签名到期和刷新接口。 */
@RestController
@RequestMapping("/users/me/files/avatar")
@RequiredArgsConstructor
public class UserFileController {
    private final ImageUploadService imageUploadService;

    /** 成功返回URL字符串；随后通过PUT /users/me将URL保存到avatarOssKey字段。 */
    @PostMapping(
            value = {"", "/upload"},
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String upload(@RequestPart("file") MultipartFile file) {
        return imageUploadService.upload(file);
    }
}
