package com.leyu.aicodegenerator.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.leyu.aicodegenerator.exception.ErrorCode;
import lombok.Data;

import java.io.Serializable;

/** BaseResponse implementation. */
@Data

public class BaseResponse<T> implements Serializable {

    private int code;

    private T data;

    private String message;

    @JsonCreator
    public BaseResponse(@JsonProperty("code") int code,
                        @JsonProperty("data") T data,
                        @JsonProperty("message") String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    /** BaseResponse implementation. */
    public BaseResponse(int code, T data) {
        this(code, data, "");
    }

    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMessage());
    }
}
