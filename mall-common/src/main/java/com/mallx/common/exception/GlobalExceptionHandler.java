package com.mallx.common.exception;

import com.mallx.common.enums.ResultCode;
import com.mallx.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 将异常转换为统一响应，避免向客户端暴露服务端异常信息。
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

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
        String message = exception.getBindingResult().getAllErrors().stream()
                .findFirst()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
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
