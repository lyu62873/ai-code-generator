package com.leyu.aicodegenerator.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/** ImageCategoryEnum implementation. */
@Getter
public enum ImageCategoryEnum {

    CONTENT("内容图片", "CONTENT"),
    LOGO("LOGO图片", "LOGO"),
    ILLUSTRATION("插画图片", "ILLUSTRATION"),
    ARCHITECTURE("架构图片", "ARCHITECTURE");


    private final String text;

    private final String value;

    ImageCategoryEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * Get enum by its value.
     *
     * @param value Enum's value.
     * @return Matched enum.
     */
    public static ImageCategoryEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (ImageCategoryEnum anEnum : ImageCategoryEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
