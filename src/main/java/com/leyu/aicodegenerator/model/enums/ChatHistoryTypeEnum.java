package com.leyu.aicodegenerator.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * Message type enum used by chat history records.
 */
@Getter
public enum ChatHistoryTypeEnum {

    USER("User Message", "user"),
    AI("AI Message", "ai");

    private final String text;

    private final String value;

    ChatHistoryTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static ChatHistoryTypeEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (ChatHistoryTypeEnum anEnum : ChatHistoryTypeEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
