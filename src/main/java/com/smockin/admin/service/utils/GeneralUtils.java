package com.smockin.admin.service.utils;

import java.util.Date;
import java.util.UUID;

/**
 * Created by mgallina.
 */
public final class GeneralUtils {

    public final static String generateUUID() {
        return UUID.randomUUID().toString();
    }

    // Should be set to UTC from command line
    public final static Date getCurrentDate() {
        return new Date();
    }

}
