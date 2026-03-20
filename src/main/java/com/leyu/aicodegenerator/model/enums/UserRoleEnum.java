package com.leyu.aicodegenerator.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/** UserRoleEnum implementation. */
@Getter
public enum UserRoleEnum {

    USER("USER", "user"),
    ADMIN("ADMIN", "admin");

    private final String text;

    private final String value;

    UserRoleEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /** getEnumByValue implementation. */
    public static UserRoleEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) return null;

        for (UserRoleEnum anEnum : UserRoleEnum.values()) {
            if (anEnum.value.equals(value)) return anEnum;
        }

        return null;
    }
}
