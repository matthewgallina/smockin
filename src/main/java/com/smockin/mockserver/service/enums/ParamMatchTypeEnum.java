package com.smockin.mockserver.service.enums;

/**
 * Created by mgallina
 */
public enum ParamMatchTypeEnum {

    lookUpKvp(true), // lookUpKvp must come 1st!
    requestHeader(true),
    requestParameter(true),
    pathVar(true),
    randomNumber(true),
    requestBody(false),
    isoDatetime(false), // isoDatetime must come before isoDate!
    isoDate(false),
    uuid(false);

    private boolean takesArg;

    ParamMatchTypeEnum(final boolean takesArg) {
        this.takesArg = takesArg;
    }

    public final boolean takesArg() {
        return takesArg;
    }

    public static final String PARAM_PREFIX = "$";

    public static ParamMatchTypeEnum toEnum(final String paramMatchTypeStr) {

        for (ParamMatchTypeEnum p : ParamMatchTypeEnum.values()) {
            if (p.name().equalsIgnoreCase(paramMatchTypeStr)) {
                return p;
            }
        }

        return null;
    }

}
