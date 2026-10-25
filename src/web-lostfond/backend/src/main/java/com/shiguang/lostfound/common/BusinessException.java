package com.shiguang.lostfound.common;

/**
 * 业务异常：由 GlobalExceptionHandler 统一转成 Result.fail。
 */
public class BusinessException extends RuntimeException {
    private final int code;

    public BusinessException(String msg) { this(400, msg); }

    public BusinessException(int code, String msg) {
        super(msg);
        this.code = code;
    }

    public int getCode() { return code; }
}
