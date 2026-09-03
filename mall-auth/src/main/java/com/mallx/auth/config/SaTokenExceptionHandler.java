package com.mallx.auth.config;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;

import com.mallx.common.enums.ResultCode;
import com.mallx.common.result.Result;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** 将认证授权异常返回为真正的401/403，避免落入通用500兜底。 */
@Order(-10)
@RestControllerAdvice
public class SaTokenExceptionHandler {
    @ExceptionHandler(NotLoginException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> notLogin(NotLoginException exception) {
        return Result.failure(ResultCode.UNAUTHORIZED, "请先登录");
    }

    @ExceptionHandler(NotPermissionException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> forbidden(NotPermissionException exception) {
        return Result.failure(ResultCode.FORBIDDEN, "没有该后台操作的权限");
    }
}
