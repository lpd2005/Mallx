package com.mallx.common.enums;

import lombok.Getter;

/** 通用接口响应状态码。 */
@Getter
public enum ResultCode {
    SUCCESS(200, "success"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未认证或登录已过期"),
    FORBIDDEN(403, "无权访问"),
    NOT_FOUND(404, "资源不存在"),
    UNSUPPORTED_MEDIA_TYPE(415, "不支持的请求内容类型"),
    INTERNAL_ERROR(500, "系统繁忙，请稍后重试");

    /** 面向客户端的稳定业务状态码。 */
    private final int code;

    /** 状态码对应的默认提示语。 */
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
