package com.leyu.aicodegenerator.model.enums;

import lombok.Getter;

/**
 *
 */
@Getter
public enum StreamMessageTypeEnum {

    AI_RESPONSE("ai_response", "AI response"),
    TOOL_REQUEST("tool_request", "Tool request"),
    TOOL_EXECUTED("tool_executed", "Tool execution result"),;

    private final String value;
    private final String text;

    StreamMessageTypeEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    /**
     *
     */
    public static StreamMessageTypeEnum getEnumByValue(String value) {
        for (StreamMessageTypeEnum typeEnum : values()) {
            if (typeEnum.getValue().equals(value)) {
                return typeEnum;
            }
        }
        return null;
    }
}
