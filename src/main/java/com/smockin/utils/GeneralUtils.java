package com.smockin.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;

import java.io.*;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Created by mgallina.
 */
public final class GeneralUtils {

    private static final Logger logger = LoggerFactory.getLogger(GeneralUtils.class);

    public static final String ISO_DATE_FORMAT = "yyyy-MM-dd";
    public static final String ISO_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";
    public static final String UNIQUE_TIMESTAMP_FORMAT = "yyMMdd_HHmmss";

    public static final String OAUTH_HEADER_VALUE_PREFIX = "Bearer";
    public static final String OAUTH_HEADER_NAME = "Authorization";
    public static final String KEEP_EXISTING_HEADER_NAME = "KeepExisting";

    public static final String ENABLE_CORS_PARAM = "ENABLE_CORS";

    public static final String LOG_REQ_ID = "X-Smockin-Trace-ID";
    public static final String PROXY_MOCK_INTERCEPT_HEADER = "X-Proxy-Mock-Intercept";

    // Looks for values within the brace format ${}. So ${bob} would return the value 'bob'.
    static final String INBOUND_TOKEN_PATTERN = "\\$\\{(.*?)\\}";

    // Thread safe class, provided all config is defined before it's use.
    static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    static {

        JSON_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    }

    public final static String generateUUID() {
        return UUID.randomUUID().toString();
    }

    // Should be set to UTC from command line
    public final static Date getCurrentDate() {
        return Date.from(getCurrentDateTime().atZone(ZoneId.systemDefault()).toInstant());
    }

    public final static Date toDate(final LocalDateTime localDateTime) {
        return Date.from(localDateTime.toInstant(ZoneOffset.UTC));
    }

    public final static LocalDateTime getCurrentDateTime() {
        return LocalDateTime.now();
    }

    public final static Instant getCurrentDateTimeInstant() {
        return getCurrentDateTime().toInstant(ZoneOffset.UTC);
    }

    public final static String createFileNameUniqueTimeStamp() {
        return new SimpleDateFormat(UNIQUE_TIMESTAMP_FORMAT)
                .format(getCurrentDate());
    }

    // NOTE It is important that this preserves any whitespaces around the token
    public static String findFirstInboundParamMatch(final String input) {

        if (input == null) {
            return null;
        }

        final Pattern pattern = Pattern.compile(INBOUND_TOKEN_PATTERN);
        final Matcher matcher = pattern.matcher(input);

        while (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    /**
     *
     * Returns the header value for the given name.
     * Look up is case insensitive (as Java Spark handles header look ups with case sensitivity, which is wrong)
     *
     * @param request
     * @param headerName
     * @returns String
     *
     */
    public static String findHeaderIgnoreCase(final Request request, final String headerName) {

        for (String h : request.headers()) {
            if (h.equalsIgnoreCase(headerName)) {
                return request.headers(h);
            }
        }

        return null;
    }

    /**
     *
     * Returns the request parameter value for the given name.
     * Look up is case insensitive (as Java Spark handles request parameter look ups with case sensitivity. Unclear on what the standard is for this...)
     *
     * @param request
     * @param requestParamName
     * @returns String
     *
     */
    public static String findRequestParamIgnoreCase(final Request request, final String requestParamName) {

        for (String q : request.queryParams()) {
            if (q.equalsIgnoreCase(requestParamName)) {
                return request.queryParams(q);
            }
        }

        return null;
    }

    /**
     *
     * Returns the request parameter value for the given name.
     * Look up is case insensitive (as Java Spark handles request parameter look ups with case sensitivity. Unclear on what the standard is for this...)
     *
     * @param request
     * @param pathVarName
     * @returns String
     *
     */
    public static String findPathVarIgnoreCase(final Request request, final String pathVarName) {

        for (Map.Entry<String, String> pv : request.params().entrySet()) {
            if (pv.getKey().equalsIgnoreCase((pathVarName.startsWith(":"))?pathVarName:(":"+pathVarName))) {
                return pv.getValue();
            }
        }

        return null;
    }

    public static void checkForAndHandleSleep(final long sleepInMillis) {

        if (sleepInMillis > 0) {
            try {
                Thread.sleep(sleepInMillis);
            } catch (InterruptedException ex) {
                logger.error("Error pausing response for the specified period of " + sleepInMillis, ex);
            }
        }
    }

    public static String prefixPath(final String path) {

        if (StringUtils.isBlank(path)) {
            return null;
        }

        final String prefix = "/";

        if (!path.startsWith(prefix)) {
            return prefix + path;
        }

        return path;
    }

    public static int exactVersionNo(String versionNo) {

        if (versionNo == null)
            throw new IllegalArgumentException("versionNo is not defined");

        versionNo = org.apache.commons.lang3.StringUtils.removeIgnoreCase(versionNo, "-SNAPSHOT");
        versionNo = org.apache.commons.lang3.StringUtils.remove(versionNo, ".");

        if (!NumberUtils.isDigits(versionNo))
            throw new IllegalArgumentException("extracted versionNo is not a valid number: " + versionNo);

        return Integer.valueOf(versionNo);
    }

    public static String removeAllLineBreaks(final String original) {
        return StringUtils.replaceAll(original, System.getProperty("line.separator"), "");
    }

    public static Map<String, ?> deserialiseJSONToMap(final String jsonStr) {
        return deserialiseJson(jsonStr);
    }

    public static <T> T deserialiseJson(final String jsonStr) {

        if (jsonStr != null) {
            try {
                return JSON_MAPPER.readValue(jsonStr, new TypeReference<T>() {});
            } catch (IOException e) {
                logger.error("Error de-serialising json", e);
                // fail silently
            }
        }

        return null;
    }

    public static <T> T deserialiseJson(final String jsonStr, final TypeReference<T> type) {

        if (jsonStr != null) {
            try {
                return JSON_MAPPER.readValue(jsonStr, type);
            } catch (IOException e) {
                logger.error("Error de-serialising json", e);
                // fail silently
            }
        }

        return null;
    }

    public static <T> String serialiseJson(final T t) {

        try {
            return JSON_MAPPER.writeValueAsString(t);
        } catch (JsonProcessingException e) {
            logger.error("Error serialising json", e);
            // fail silently
        }

        return null;
    }

    public static String extractOAuthToken(final String bearerToken) {

        if (bearerToken == null) {
            return null;
        }

        return StringUtils.replace(bearerToken, OAUTH_HEADER_VALUE_PREFIX, "").trim();
    }

    public static String getFileTypeExtension(final String fileName) {

        if (fileName == null) {
            return null;
        }

        final int extPos = fileName.lastIndexOf(".");

        if (extPos == -1) {
            return null;
        }

        return fileName.substring(extPos);
    }

    // TODO fix this!
    public static byte[] createArchive(final File[] files) {

        ByteArrayOutputStream bos = null;
        ZipOutputStream zipOut = null;

        final byte[] buffer = new byte[1024];

        try {

            bos = new ByteArrayOutputStream();
            zipOut = new ZipOutputStream(bos);

            for (File fileToZip : files) {

                FileInputStream fis = null;

                try {

                    fis = new FileInputStream(fileToZip);
                    final ZipEntry zipEntry = new ZipEntry(fileToZip.getName());

                    zipOut.putNextEntry(zipEntry);

                    int length;
                    while ((length = fis.read(buffer)) >= 0) {
                        zipOut.write(buffer, 0, length);
                    }

                    zipOut.closeEntry();

                } catch (IOException e) {
                    throw e;
                } finally {
                    if (fis != null) {
                        fis.close();
                    }
                }

            }

            return bos.toByteArray();

        } catch (IOException e) {
            logger.error("Error creating archive file", e);
        } finally {
            if (zipOut != null) {
                try {
                zipOut.close();
                } catch (IOException e) {}
            }
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {}
            }
        }

        return null;
    }

    public static void unpackArchive(final String zipFilePath, final String destDir) {

        final File dir = new File(destDir);

        if (!dir.exists())
            dir.mkdirs();

        final byte[] buffer = new byte[1024];
        FileInputStream fis = null;

        try {

            fis = new FileInputStream(zipFilePath);
            final ZipInputStream zis = new ZipInputStream(fis);

            ZipEntry ze = zis.getNextEntry();

            while (ze != null) {

                final File newFile = new File(destDir + File.separator + ze.getName());

                if (ze.isDirectory()) {

                    newFile.mkdir();

                } else {

                    FileOutputStream fos = null;

                    try {
                        fos = new FileOutputStream(newFile);
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }

                    } finally {
                        if (fos != null)
                            fos.close();
                    }

                }

                zis.closeEntry();
                ze = zis.getNextEntry();
            }

            zis.closeEntry();
            zis.close();

        } catch (IOException e) {
            logger.error("Error unpacking archive file", e);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {

                }
            }
        }

    }

}
