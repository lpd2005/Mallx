package com.mallx.admin.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;

import com.mallx.common.context.CurrentUserProvider;
import com.mallx.common.oss.ImageUploadService;
import com.mallx.common.oss.ImageUploadVO;
import com.mallx.common.result.Result;

import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** 商品、品牌、分类图片上传入口，保留后台准入及商品管理权限。 */
@RestController
@RequestMapping("/admin/files/images")
@RequiredArgsConstructor
@SaCheckPermission("product:manage")
public class AdminFileController {
    private final ImageUploadService imageUploadService;
    private final CurrentUserProvider currentUserProvider;

    /** multipart字段名为file，上传后再通过商品管理接口保存objectKey。 */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<ImageUploadVO> upload(@RequestPart("file") MultipartFile file) {
        return Result.success(
                imageUploadService.uploadProductImage(file, currentUserProvider.requireUserId()));
    }

    /** 私有图片签名过期后刷新，不重新上传或修改业务数据。 */
    @GetMapping("/url")
    public Result<ImageUploadVO> url(@RequestParam String objectKey) {
        return Result.success(imageUploadService.productImageUrl(objectKey));
    }
}
