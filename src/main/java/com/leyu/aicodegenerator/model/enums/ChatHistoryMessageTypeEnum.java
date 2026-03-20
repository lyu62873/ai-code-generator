package com.leyu.aicodegenerator.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/** ChatHistoryMessageTypeEnum implementation. */
@Getter
public enum ChatHistoryMessageTypeEnum {

    USER("User", "user"),
    AI("AI", "ai"),
    TOOL_EXECUTION_REQUEST("Tool Execution Request", "toolExecutionRequest"),
    TOOL_EXECUTION_RESULT("Tool Execution Result", "toolExecutionResult");

    private final String text;

    private final String value;

    ChatHistoryMessageTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * Get enum by its value.
     *
     * @param value Enum value.
     * @return Matched enum value.
     */
    public static ChatHistoryMessageTypeEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (ChatHistoryMessageTypeEnum anEnum : ChatHistoryMessageTypeEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}

