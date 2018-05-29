package com.smockin.admin.persistence.enums;

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

        for (RestMethodEnum restMethod : RestMethodEnum.values()) {
            if (restMethod.name().equalsIgnoreCase(name))
                return restMethod;
        }

        return null;
    }

}
