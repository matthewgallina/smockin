package com.smockin.mockserver.service;

import com.smockin.admin.dto.UserKeyValueDataDTO;
import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.admin.service.SmockinUserService;
import com.smockin.admin.service.UserKeyValueDataService;
import com.smockin.mockserver.service.dto.RestfulResponseDTO;
import com.smockin.utils.GeneralUtils;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spark.Request;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class JavaScriptResponseHandlerImpl implements JavaScriptResponseHandler {

    private final Logger logger = LoggerFactory.getLogger(JavaScriptResponseHandlerImpl.class);

    @Autowired
    private SmockinUserService smockinUserService;

    @Autowired
    private UserKeyValueDataService userKeyValueDataService;

    private final String extensionsDir = "js-extensions/";

    public RestfulResponseDTO executeUserResponse(final Request req, final RestfulMock mock) {
        logger.debug("executeUserResponse called");

        Object engineResponse;

        try {

            engineResponse = executeJS(
                    defaultRequestObject
                        + populateRequestObjectWithInbound(req, mock.getPath(), mock.getCreatedBy().getCtxPath())
                        + populateKVPs(req, mock)
                        + keyValuePairFindFunc
                        + defaultResponseObject
                        + userResponseFunctionInvoker
                        + mock.getJavaScriptHandler().getSyntax());

        } catch (ScriptException ex) {

            return new RestfulResponseDTO(500,
                    "text/plain",
                    "Looks like there is an issue with the Javascript driving this mock " + ex.getMessage());
        }

        if (!(engineResponse instanceof ScriptObjectMirror)) {
            return new RestfulResponseDTO(500,
                    "text/plain",
                    "Looks like there is an issue with the Javascript driving this mock!");
        }

        final ScriptObjectMirror response = (ScriptObjectMirror) engineResponse;

        return new RestfulResponseDTO(
                (int) response.get("status"),
                (String) response.get("contentType"),
                (String) response.get("body"),
                convertResponseHeaders(response));
    }

    Object executeJS(final String js) throws ScriptException {
        if (logger.isDebugEnabled())
            logger.debug(js);
        return buildEngine().eval(js);
    }

    String populateRequestObjectWithInbound(final Request req, final String mockPath, final String ctxPath) {

        final Map<String, String> reqHeaders =
                req.headers()
                    .stream()
                    .collect(Collectors.toMap(k -> k, k -> req.headers(k)));

        final StringBuilder reqObject = new StringBuilder();

        reqObject.append("request.path=")
                .append("'").append(req.pathInfo()).append("'")
                .append("; ");

        if (StringUtils.isNotBlank(req.body())) {
            reqObject.append("request.body=")
                    .append("'").append(removeLineBreaks(req.body())).append("'")
                    .append(";");
        }

        final String sanitizedInboundPath = GeneralUtils.sanitizeMultiUserPath(smockinUserService.getUserMode(), req.pathInfo(), ctxPath);
        applyMapValuesToStringBuilder("request.pathVars", GeneralUtils.findAllPathVars(sanitizedInboundPath, mockPath), reqObject);
        applyMapValuesToStringBuilder("request.parameters", extractAllRequestParams(req), reqObject);
        applyMapValuesToStringBuilder("request.headers", reqHeaders, reqObject);

        return reqObject.toString();
    }

    void applyMapValuesToStringBuilder(final String field, final Map<String, String> values, final StringBuilder reqObject) {

        if (values == null || values.isEmpty()) {
            return;
        }

        values.entrySet().forEach(e ->
                reqObject.append(" ")
                        .append(field)
                        .append("['").append(e.getKey()).append("']")
                        .append("=")
                        .append("'").append(e.getValue()).append("'")
                        .append(";"));
    }

    Map<String, String> extractAllRequestParams(final Request req) {

        // Java Spark does not provide a convenient way of extracting form based request parameters,
        // so have to parse these manually.
        if (req.contentType() != null
                && (req.contentType().contains(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                ||  req.contentType().contains(MediaType.MULTIPART_FORM_DATA_VALUE))) {

            return URLEncodedUtils.parse(req.body(), Charset.defaultCharset())
                    .stream()
                    .collect(HashMap::new, (m, v) -> m.put(v.getName(), v.getValue()), HashMap::putAll);
        }

        return req.queryParams()
                .stream()
                .collect(Collectors.toMap(k -> k, k -> req.queryParams(k)));
    }

    Set<Map.Entry<String, String>> convertResponseHeaders(final ScriptObjectMirror response) {

        final Object headersJS = response.get("headers");

        final Map<String, String> responseHeaders = new HashMap<>();

        if (headersJS instanceof ScriptObjectMirror) {
            ((ScriptObjectMirror) headersJS)
                    .entrySet()
                    .forEach(e ->
                            responseHeaders.put(e.getKey(), (String)e.getValue()));
        }

        return responseHeaders.entrySet();
    }

    String populateKVPs(final Request req, final RestfulMock mock) throws ScriptException {
        logger.debug("populateKVPs called");

        final String handleResponseFunc = GeneralUtils.removeJsComments(mock.getJavaScriptHandler().getSyntax());
        final long mockOwnerUserId = mock.getCreatedBy().getId();

        final int MAX_PASSES = 500;
        int currentPos = 0;
        final String keyValuePairFuncPrefix = keyValuePairFindFuncName + "(";

        final Map<String, String> kvps = new HashMap<>();

        for (int i=0; i < MAX_PASSES; i++) {

            final int startPos = StringUtils.indexOf(handleResponseFunc, keyValuePairFuncPrefix, currentPos);

            if (startPos == -1) {
                break;
            }

            final int closingParenthesisPos = StringUtils.indexOf(handleResponseFunc, ")", startPos);
            final String sanitizedKey = findKvpKey(startPos, closingParenthesisPos, req, mock, keyValuePairFuncPrefix, handleResponseFunc);

            if (sanitizedKey != null) {
                final UserKeyValueDataDTO userKeyValueDataDTO = userKeyValueDataService.loadByKey(sanitizedKey, mockOwnerUserId);
                kvps.put(sanitizedKey, (userKeyValueDataDTO != null) ? userKeyValueDataDTO.getValue() : "");
            }

            currentPos = closingParenthesisPos;
        }

        if (!kvps.isEmpty()) {
            return defaultKeyValuePairStoreObjectStart
                    + GeneralUtils.serialiseJson(kvps)
                    + ";";
        }

        return defaultKeyValuePairStoreObject;
    }

    private String findKvpKey(final int startPos, final int closingParenthesisPos, final Request req, final RestfulMock mock, final String keyValuePairFuncPrefix, final String handleResponseFunc)
            throws ScriptException {
        logger.debug("findKvpKey called");

        final String invalidMsgPrefix = "Invalid lookUpKvp(...) syntax. ";

        if (closingParenthesisPos == -1) {
            throw new ScriptException(invalidMsgPrefix + "Unable to determine closing parenthesis position");
        }

        final String keyName = StringUtils.substring(handleResponseFunc, (startPos + keyValuePairFuncPrefix.length()), closingParenthesisPos);

        if (StringUtils.isBlank(keyName)) {
            throw new ScriptException(invalidMsgPrefix + "key within find parenthesis is undefined");
        }

        logger.debug(String.format("keyName: %s", keyName));

        final String sanitizedKey;

        if (keyName.startsWith("'") && keyName.endsWith("'")) {
            sanitizedKey = StringUtils.remove(keyName, "'");
        } else if (keyName.startsWith("\"") && keyName.endsWith("\"")) {
            sanitizedKey = StringUtils.remove(keyName, "\"");
        } else if (keyName.indexOf("request.") > -1) {

            final String requestObjectField = StringUtils.remove(keyName, "request.").trim();

            if (requestObjectField.startsWith("pathVars")) {

                final String pathVarsObjectField = StringUtils.remove(requestObjectField, "pathVars").trim();
                final String sanitizedInboundPath = GeneralUtils.sanitizeMultiUserPath(smockinUserService.getUserMode(), req.pathInfo(), mock.getCreatedBy().getCtxPath());
                sanitizedKey = GeneralUtils.findAllPathVars(sanitizedInboundPath, mock.getPath())
                        .get(extractObjectField(StringUtils.lowerCase(pathVarsObjectField)));

            } else if ("body".equals(requestObjectField)) {

                if (StringUtils.isBlank(req.body())) {
                    throw new ScriptException(invalidMsgPrefix + "request.body is undefined");
                }

                sanitizedKey = removeLineBreaks(req.body());

            } else if (requestObjectField.startsWith("headers")) {

                final String headersObjectField = StringUtils.remove(requestObjectField, "headers").trim();

                sanitizedKey = req.headers()
                        .stream()
                        .collect(Collectors.toMap(k -> k, k -> req.headers(k)))
                        .get(extractObjectField(headersObjectField));

            } else if (requestObjectField.startsWith("parameters")) {

                final String parametersObjectField = StringUtils.remove(requestObjectField, "parameters").trim();

                sanitizedKey = extractAllRequestParams(req).get(extractObjectField(parametersObjectField));

            } else {
                throw new ScriptException(invalidMsgPrefix + "Unable to determine request based key look up");
            }

        } else {
            throw new ScriptException(invalidMsgPrefix + "Unable to determine key lookup type");
        }

        return (sanitizedKey != null)
                ? sanitizedKey.trim()
                : null;

    }

    private String extractObjectField(final String objectField) {

        if (StringUtils.startsWith(objectField, ".")) {

            return StringUtils.remove(objectField, ".");
        } else if (StringUtils.startsWith(objectField, "[")) {

            final String objectFieldP1 = StringUtils.remove(objectField, "['");
            return StringUtils.remove(objectFieldP1, "']");
        }

        return null;
    }

    private ScriptEngine buildEngine() {

        final ScriptEngine engine = new NashornScriptEngineFactory()
                .getScriptEngine(
                        engineSecurityArgs,
                        null,
                            (s) -> false);

        loadEngineExtensions(engine);
        applyEngineBindings(engine);

        return engine;
    }

    private void loadEngineExtensions(final ScriptEngine engine) {

        try {

            engine.eval(new FileReader(getExtensionsFilePath("from-xml.min.js"))); // XML support

        } catch (ScriptException | FileNotFoundException e) {
            logger.error("Error loading JS extensions", e);
        }
    }

    private String getExtensionsFilePath(final String extensionsFileName) {

        return getClass()
                .getClassLoader()
                .getResource(extensionsDir + extensionsFileName)
                .getFile();
    }

    // A few security restrictions...
    private void applyEngineBindings(final ScriptEngine engine) {

        final Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
        bindings.remove("exit");
        bindings.remove("java");
        bindings.remove("javax");
        bindings.remove("sun");

    }

    private String removeLineBreaks(final String input) {

        final String systemCarriage = System.getProperty("line.separator");
        final String carriage = "\\r\\n|\\r|\\n";

        return StringUtils.replaceAll(input, (systemCarriage != null) ? systemCarriage : carriage, "");
    }

}
