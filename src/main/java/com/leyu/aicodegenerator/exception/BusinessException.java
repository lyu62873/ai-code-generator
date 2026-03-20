package com.leyu.aicodegenerator.exception;

import lombok.Getter;

/** BusinessException implementation. */
@Getter
public class BusinessException extends RuntimeException{

    // Error Code
    private final int code;

    /** BusinessException implementation. */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    /** BusinessException implementation. */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }
}
