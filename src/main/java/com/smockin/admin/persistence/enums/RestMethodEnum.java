package com.smockin.admin.persistence.enums;

import java.util.stream.Stream;

/**
 * Created by mgallina.
 */
public enum RestMethodEnum {
    GET,
    POST,
    PUT,
    DELETE,
    PATCH;

    public static RestMethodEnum findByName(final String name) {
        return Stream.of(RestMethodEnum.values())
                .filter(rm -> (rm.name().equalsIgnoreCase(name)))
                .findFirst().orElse(null);
    }

}
