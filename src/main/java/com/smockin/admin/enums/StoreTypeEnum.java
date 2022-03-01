package com.smockin.admin.enums;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

public enum StoreTypeEnum {
    DB, CACHE;

    public static StoreTypeEnum toEnum(final String value) {

        return Arrays.stream(values())
                .filter(e ->
                        StringUtils.equalsIgnoreCase(e.name(), value))
                .findFirst()
                .orElse(null);
    }
}
