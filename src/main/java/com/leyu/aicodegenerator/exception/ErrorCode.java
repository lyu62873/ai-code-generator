package com.leyu.aicodegenerator.exception;

import lombok.Getter;

/** ErrorCode implementation. */
@Getter
public enum ErrorCode {

    SUCCESS(0, "OK"),
    PARAMS_ERROR(40000, "Request Param Error"),
    NOT_LOGIN_ERROR(40100, "Not Login"),
    NO_AUTH_ERROR(40101, "No Permission"),
    NOT_FOUND_ERROR(40400, "Data Not Exist"),
    FORBIDDEN_ERROR(40300, "Blocked"),
    SYSTEM_ERROR(50000, "Internal Error"),
    OPERATION_ERROR(50001, "Operation Fail"),
    TOO_MANY_REQUEST(42900, "Too Many Request");



    // state code
    private final int code;

    // info
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
