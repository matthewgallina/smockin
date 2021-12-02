package com.smockin.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smockin.admin.enums.UserModeEnum;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.AntPathMatcher;
import spark.Request;

import java.io.*;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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

    public static final String S3_HOST = "localhost";

    public static final String LOG_REQ_ID = "X-Smockin-Trace-ID";
    public static final String PROXIED_DOWNSTREAM_URL_HEADER = "X-Proxied-Downstream-Url";
    public static final String PATH_WILDCARD = "*";
    public static final String URL_PATH_SEPARATOR = "/";

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

    public static String findPathVarIgnoreCase(final String inboundPath, final String mockPath, final String pathVarName) {

        if (pathVarName == null) {
            return null;
        }

        return findAllPathVars(inboundPath, mockPath).get(pathVarName.toLowerCase());
    }

    public static Map<String, String> findAllPathVars(final String inboundPath, final String mockPath) {

        final String[] inboundPathSegments = StringUtils.split(inboundPath, URL_PATH_SEPARATOR);
        final String[] mockPathSegments = StringUtils.split(mockPath, URL_PATH_SEPARATOR);

        final Map<String, String> pathVars = new HashMap<>();

        if (inboundPathSegments.length < mockPathSegments.length) {
            return pathVars;
        }

        int index = 0;

        for (String segment : mockPathSegments) {

            if (segment != null
                    && ("*".equals(segment)
                            || (segment.startsWith("{") && segment.endsWith("}")))) {

                segment = StringUtils.remove(segment, "{");
                segment = StringUtils.remove(segment, "}");

                if ("*".equals(segment)) {
                    segment = "*" + index;
                }

                pathVars.put(segment.toLowerCase(), inboundPathSegments[index]);
            }

            index++;
        }

        return pathVars;
    }

    public static String sanitizeMultiUserPath(final UserModeEnum usermode, final String pathInfo, final String ctxPath) {

        return ( UserModeEnum.ACTIVE.equals(usermode) && StringUtils.isNotBlank(ctxPath) )
                    ? StringUtils.removeFirst(pathInfo, ctxPath)
                    : pathInfo;
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

        if (!path.startsWith(URL_PATH_SEPARATOR)) {
            return URL_PATH_SEPARATOR + path;
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

    public static List<Map<String, ?>> deserialiseJSONToList(final String jsonStr) {
        return deserialiseJson(jsonStr, false);
    }

    public static Map<String, ?> deserialiseJSONToMap(final String jsonStr) {
        return deserialiseJson(jsonStr, false);
    }

    public static <T> T deserialiseJson(final String jsonStr) {
        return deserialiseJson(jsonStr, true);
    }

    public static Map<String, ?> deserialiseJSONToMap(final String jsonStr, final boolean logFailure) {
        return deserialiseJson(jsonStr, logFailure);
    }

    public static <T> T deserialiseJson(final String jsonStr, final boolean logFailure) {

        if (jsonStr != null) {
            try {
                return JSON_MAPPER.readValue(jsonStr, new TypeReference<T>() {});
            } catch (IOException e) {
                if (logFailure) {
                    logger.error("Error de-serialising json", e);
                }
                // always fail silently
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

    public static byte[] createArchive(final String zipFileName, final byte[] zipFileContent) {

        ZipOutputStream zos = null;
        ByteArrayOutputStream baos = null;

        try {

            baos = new ByteArrayOutputStream();
            zos = new ZipOutputStream(baos);
            final ZipEntry entry = new ZipEntry(zipFileName);

            entry.setSize(zipFileContent.length);

            zos.putNextEntry(entry);

            zos.write(zipFileContent);
            zos.closeEntry();

            closeOSQuietly(zos);

            return baos.toByteArray();

        } catch (Throwable ex) {
            logger.error("Error creating zip archive", ex);

            closeOSQuietly(zos);
            closeOSQuietly(baos);
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
                        closeOSQuietly(fos);
                    }

                }

                zis.closeEntry();
                ze = zis.getNextEntry();
            }

            zis.closeEntry();
            closeISQuietly(zis);

        } catch (IOException e) {
            logger.error("Error unpacking archive file", e);
        } finally {
            closeISQuietly(fis);
        }

    }

    private static void closeISQuietly(final InputStream is) {
        if (is != null) {
            try {
                is.close();
            } catch (IOException e) {
                logger.error("Error closing input stream", e);
            }
        }
    }

    private static void closeOSQuietly(final OutputStream os) {
        if (os != null) {
            try {
                os.close();
            } catch (IOException e) {
                logger.error("Error closing output stream", e);
            }
        }
    }

    public static String extractRequestParamByName(final Request req, final String fieldName) {

        return extractAllRequestParams(req)
                .entrySet()
                .stream()
                .filter(p ->
                        StringUtils.equalsIgnoreCase(fieldName, p.getKey()))
                .map(p ->
                        p.getValue())
                .findFirst()
                .orElse(null);
    }

    public static Map<String, String> extractAllRequestParams(final Request req) {

        if (!StringUtils.equalsIgnoreCase(HttpMethod.POST.name(), req.requestMethod())
                && !StringUtils.equalsIgnoreCase(HttpMethod.PUT.name(), req.requestMethod())
                && !StringUtils.equalsIgnoreCase(HttpMethod.PATCH.name(), req.requestMethod())) {

            if (req.queryParams().isEmpty()) {
                return new HashMap<>();
            }

            return req.queryParams()
                    .stream()
                    .collect(Collectors.toMap(k -> k, k -> req.queryParams(k)));
        }

        final Map<String, String> allParams = req.queryMap()
                .toMap()
                .entrySet()
                .stream()
                .collect(HashMap::new,
                        (m,v) ->
                            m.put(v.getKey(), (v.getValue() != null && v.getValue().length != 0) ? v.getValue()[0] : null),
                        HashMap::putAll);


        if (req.contentType() != null
                && (req.contentType().contains(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                ||  req.contentType().contains(MediaType.MULTIPART_FORM_DATA_VALUE))) {

            if (req.body() != null && req.body().contains("=")) {
                allParams.putAll(URLEncodedUtils.parse(req.body(), Charset.defaultCharset())
                                    .stream()
                                    .collect(HashMap::new, (m,v) -> m.put(v.getName(), v.getValue()), HashMap::putAll));
            }
        }

        return allParams;
    }

    public static String removeJsComments(final String jsSrc) {

        final String carriage = "\n";
        final String comment = "//";

        if (jsSrc == null
                || StringUtils.indexOf(jsSrc, comment) == -1) {
            return jsSrc;
        }

        // i.e single line
        if (StringUtils.indexOf(jsSrc, carriage) == -1) {
            return StringUtils.substring(jsSrc, 0, StringUtils.indexOf(jsSrc, comment)).trim();
        }

        final String[] lines = StringUtils.split(jsSrc, carriage);

        return Stream.of(lines)
                .filter(l ->
                    !l.trim().startsWith(comment))
                .map(l -> {

                    final int commentInLine = StringUtils.indexOf(l, comment);

                    return (commentInLine > -1)
                            ? StringUtils.substring(l, 0, commentInLine)
                            : l;
                })
                .collect(Collectors.joining(carriage))
                .trim();
    }

    public static boolean matchPaths(final String mockPath, final String inboundPath) {

        final AntPathMatcher matcher = new AntPathMatcher(AntPathMatcher.DEFAULT_PATH_SEPARATOR);

        return matcher.match(mockPath, inboundPath);
    }

    public static Optional<String> convertInputStreamToString(final InputStream inputStream,
                                                              final boolean closeStream) {

        try {
            return Optional.of(IOUtils.toString(inputStream, Charset.defaultCharset()));
        } catch (IOException ex) {
            logger.error("Error reading input stream to string", ex);
            return Optional.empty();
        } finally {
            if (closeStream) {
                GeneralUtils.closeSilently(inputStream);
            }
        }

    }

    public static void closeSilently(final InputStream fis) {
        if (fis != null) {
            try {
                fis.close();
            } catch (IOException ex) {
                logger.error("Error closing inputstream", ex);
            }
        }
    }

}
