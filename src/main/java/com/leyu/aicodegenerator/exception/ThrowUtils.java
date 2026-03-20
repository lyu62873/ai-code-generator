package com.leyu.aicodegenerator.exception;

/** ThrowUtils implementation. */
public class ThrowUtils {

    /*
    Throw Error in Certain Condition
     */

    /** throwIf implementation. */
    public static void throwIf(boolean condition, RuntimeException runtimeException) {
        if (condition) {
            throw runtimeException;
        }
    }

    /** throwIf implementation. */
    public static void throwIf(boolean condition, ErrorCode errorCode) {
        throwIf(condition, new BusinessException(errorCode));
    }

    public static void throwIf(boolean condition, ErrorCode errorCode, String message) {
        throwIf(condition, new BusinessException(errorCode, message));
    }
}
