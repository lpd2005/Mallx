package com.mallx.common.exception;

import com.mallx.common.enums.ResultCode;

/**
 * 可预期的业务异常，不携带底层实现细节。
 */
public class BusinessException extends RuntimeException {

    /** 异常对应的对外响应状态码。 */
    private final ResultCode resultCode;

    /**
     * 使用状态码的默认文案构造业务异常。
     *
     * @param resultCode 对外响应状态码
     */
    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.resultCode = resultCode;
    }

    /**
     * 使用业务场景指定的安全提示语构造异常。
     *
     * @param resultCode 对外响应状态码
     * @param message 对客户端展示的业务提示，不应包含底层异常细节
     */
    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }

    /**
     * @return 当前异常的对外响应状态码
     */
    public ResultCode getResultCode() {
        return resultCode;
    }
}
