package com.leyu.aicodegenerator.common;

import com.leyu.aicodegenerator.exception.ErrorCode;

/** ResultUtils implementation. */
public class ResultUtils {

    // SUCCESS
    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(0, data, "OK");
    }

    /** success implementation. */
    public static <T> BaseResponse<T> success() {
        return new BaseResponse<>(0, null, "OK");
    }

    // FAILURE
    public static BaseResponse<?> error(ErrorCode errorCode) {
        return new BaseResponse<>(errorCode);
    }

    /** error implementation. */
    public static BaseResponse<?> error(int code, String message) {
        return new BaseResponse<>(code, null, message);
    }

    public static BaseResponse<?> error(ErrorCode errorCode, String message) {
        return new BaseResponse<>(errorCode.getCode(), null, message);
    }
}
