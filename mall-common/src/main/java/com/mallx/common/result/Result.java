package com.mallx.common.result;

import com.mallx.common.enums.ResultCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 平台统一接口响应体。
 *
 * @param <T> 响应数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    /** 业务状态码。 */
    private int code;

    /** 面向调用方的提示语。 */
    private String message;

    /** 响应数据；失败时通常为 {@code null}。 */
    private T data;

    /**
     * 构建标准成功响应。
     *
     * @param data 响应数据
     * @param <T> 响应数据类型
     * @return 成功响应
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    /**
     * 构建不包含业务数据的标准成功响应。
     *
     * @return 空数据成功响应
     */
    public static Result<Void> success() {
        return success(null);
    }


    /**
     * 构建失败响应；未指定提示语时使用状态码默认文案。
     *
     * @param resultCode 失败状态码
     * @param message 可选的安全业务提示
     * @param <T> 响应数据类型
     * @return 失败响应
     */
    public static <T> Result<T> failure(ResultCode resultCode, String message) {
        String responseMessage = message == null || message.isBlank() ? resultCode.getMessage() : message;
        return new Result<>(resultCode.getCode(), responseMessage, null);
    }
}
