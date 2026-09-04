package com.mallx.common.exception;

import com.mallx.common.enums.ResultCode;
import com.mallx.common.result.Result;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

/** 将异常转换为统一响应，避免向客户端暴露服务端异常信息。 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /** 路径/查询参数转换失败及无法读取的JSON均是客户端错误，不返回解析器内部详情。 */
    @ExceptionHandler({
        MethodArgumentTypeMismatchException.class,
        HttpMessageNotReadableException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleUnreadableRequest(Exception exception) {
        String message =
                exception instanceof HttpMessageNotReadableException
                        ? "请求体不能为空且必须符合接口要求的JSON格式"
                        : "请求参数类型或格式不正确";
        return Result.failure(ResultCode.BAD_REQUEST, message);
    }

    /** 例如图片上传接口收到JSON或普通表单，返回415而不是服务器异常。 */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public Result<Void> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException exception) {
        return Result.failure(ResultCode.UNSUPPORTED_MEDIA_TYPE, "不支持的Content-Type，请按接口要求设置请求内容类型");
    }

    /** 文件超限、缺少file或multipart损坏均属于请求错误。 */
    @ExceptionHandler({
        MaxUploadSizeExceededException.class,
        MultipartException.class,
        MissingServletRequestPartException.class,
        MissingServletRequestParameterException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleUploadRequestException(Exception exception) {
        String message =
                exception instanceof MaxUploadSizeExceededException
                        ? "上传文件或请求体超过允许大小"
                        : "请求不完整或格式不正确，请检查文件和请求参数";
        return Result.failure(ResultCode.BAD_REQUEST, message);
    }

    /** 转换可预期的业务异常。 */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBusinessException(BusinessException exception) {
        return Result.failure(exception.getResultCode(), exception.getMessage());
    }

    /**
     * 提取第一个参数校验失败原因，避免返回 Spring 内部对象结构。
     *
     * @param exception 参数绑定与校验异常
     * @return 统一格式的参数错误响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidationException(MethodArgumentNotValidException exception) {
        String message =
                exception.getBindingResult().getAllErrors().stream()
                        .findFirst()
                        // @ModelAttribute类型转换也会进入此分支，不能透传Java类型及原始输入。
                        .map(
                                error ->
                                        error instanceof FieldError field
                                                        && field.isBindingFailure()
                                                ? "请求参数类型或格式不正确"
                                                : error.getDefaultMessage())
                        .orElse(ResultCode.BAD_REQUEST.getMessage());
        return Result.failure(ResultCode.BAD_REQUEST, message);
    }

    /**
     * 兜底处理未分类异常。
     *
     * @param exception 未预期异常
     * @return 不包含内部错误细节的统一响应
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleUnexpectedException(Exception exception) {
        // 完整堆栈仅写入服务端日志，响应中不暴露内部异常细节。
        log.error("Unexpected error", exception);
        return Result.failure(ResultCode.INTERNAL_ERROR, null);
    }
}
