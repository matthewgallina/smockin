package com.smockin.utils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import spark.Request;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class RuleEngineUtils {

    public static String matchOnPathVariable(final String fieldName, final Request req) {

        final int argPosition = NumberUtils.toInt(fieldName, -1);

        // TODO MG will need to test this with call analytics!

        if (argPosition == -1
                || req.splat().length < argPosition) {
            throw new IllegalArgumentException("Unable to perform wildcard matching on the mocked endpoint '" + req.pathInfo() + "'. Path variable arg count does not align.");
        }

        return req.splat()[(argPosition - 1)];

    }

    public static String matchOnJsonField(final String fieldName, final String reqBody, final String path) {

        if (StringUtils.isBlank(reqBody)) {
            return null;
        }

        final Object jsonRequestBody = (StringUtils.startsWith(reqBody, "["))
                ? GeneralUtils.deserialiseJSONToList(reqBody)
                : GeneralUtils.deserialiseJSONToMap(reqBody);

        if (jsonRequestBody == null) {
            return null;
        }

        // e.g.
        // person.name
        // person.pets[2].type
        if (StringUtils.indexOf(fieldName, ".") > -1) {

            final String[] fields = StringUtils.split(fieldName,".");

            Object currentJsonObject = jsonRequestBody;

            for (String f : fields) {

                if (isJSONFieldAList(f)) {

                    final Optional<String> listFieldNameOpt = extractJSONFieldListFieldName(f);

                    if (listFieldNameOpt.isPresent()) {

                        currentJsonObject = ((Map) currentJsonObject).get(listFieldNameOpt.get());

                        if (!(currentJsonObject instanceof List)) {
                            return null;
                        }
                    }

                    final Integer fieldListPos = extractJSONFieldListPosition(f);

                    if (fieldListPos == null) {
                        return null;
                    }

                    currentJsonObject = ((List)currentJsonObject).get(fieldListPos);

                } else if (currentJsonObject instanceof Map) {
                    currentJsonObject = ((Map)currentJsonObject).get(f);
                } else {
                    return null;
                }

            }

            if (currentJsonObject == null
                    || currentJsonObject instanceof List
                    || currentJsonObject instanceof Map) {
                return null;
            }

            return (String)currentJsonObject;

        } else {

            if (isJSONFieldAList(fieldName)
                    && jsonRequestBody instanceof List) {
                return (String)((List)jsonRequestBody).get(0);
            } else {
                return (String)((Map<String, ?>)jsonRequestBody).get(fieldName);
            }

        }

    }

    static boolean isJSONFieldAList(final String field) {

        return field.indexOf("[") > -1 && field.endsWith("]");
    }

    static Optional<String> extractJSONFieldListFieldName(final String field) {

        if (field.startsWith("[")) {
            return Optional.empty();
        }

        return Optional.of(field.substring(0, field.indexOf("[")));
    }

    static Integer extractJSONFieldListPosition(final String field) {

        int bracketStart = field.indexOf("[");
        final String positionStr = field.substring( (bracketStart + 1), (field.length() - 1) );

        if (!NumberUtils.isDigits(positionStr)) {
            return null;
        }

        return Integer.valueOf(positionStr);
    }

}
