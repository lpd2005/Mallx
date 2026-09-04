package com.mallx.user.controller;

import com.mallx.common.context.CurrentUserProvider;
import com.mallx.common.oss.ImageUploadService;
import com.mallx.common.oss.ImageUploadVO;
import com.mallx.common.result.Result;

import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** 登录用户的头像上传与预览，不允许匿名上传或为其他用户签发链接。 */
@RestController
@RequestMapping("/users/me/files/avatar")
@RequiredArgsConstructor
public class UserFileController {
    private final ImageUploadService imageUploadService;
    private final CurrentUserProvider currentUserProvider;

    /** 仅上传，保存资料时将objectKey传入现有PUT /users/me的avatarOssKey字段。 */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<ImageUploadVO> upload(@RequestPart("file") MultipartFile file) {
        return Result.success(
                imageUploadService.uploadAvatar(file, currentUserProvider.requireUserId()));
    }

    /** 刷新当前用户自己上传头像的展示链接。 */
    @GetMapping("/url")
    public Result<ImageUploadVO> url(@RequestParam String objectKey) {
        return Result.success(
                imageUploadService.avatarUrl(objectKey, currentUserProvider.requireUserId()));
    }
}
