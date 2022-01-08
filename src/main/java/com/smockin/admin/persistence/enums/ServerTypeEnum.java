package com.smockin.admin.persistence.enums;

import org.apache.commons.lang3.StringUtils;

/**
 * Created by mgallina.
 */
public enum ServerTypeEnum {
    RESTFUL,
    S3,
    MAIL,
    JMS,
    FTP;

    public static ServerTypeEnum toServerType(final String value) {

        if (value == null) {
            return null;
        }

        for (ServerTypeEnum st : values()) {
            if (StringUtils.equals(st.name(), value)) {
                return st;
            }
        }

        return null;
    }

}
