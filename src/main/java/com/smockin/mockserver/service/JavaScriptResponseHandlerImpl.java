package com.smockin.mockserver.service;

import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.mockserver.service.dto.RestfulResponseDTO;
import com.smockin.utils.GeneralUtils;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spark.Request;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class JavaScriptResponseHandlerImpl implements JavaScriptResponseHandler {

    private final Logger logger = LoggerFactory.getLogger(JavaScriptResponseHandlerImpl.class);

    public RestfulResponseDTO executeUserResponse(final Request req, final RestfulMock mock) {
        logger.debug("executeUserResponse called");

        Object engineResponse;

        try {

            engineResponse = executeJS(
                    defaultRequestObject
                        + populateRequestObjectWithInbound(req, mock.getPath())
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
        return buildEngine().eval(js);
    }

    String populateRequestObjectWithInbound(final Request req, final String mockPath) {

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
                    .append("'").append(req.body()).append("'")
                    .append(";");
        }

        applyMapValuesToStringBuilder("request.pathVars", GeneralUtils.findAllPathVars(req.pathInfo(), mockPath), reqObject);
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
                    .collect(Collectors.toMap(k -> k.getName(), v -> v.getValue()));
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

    private ScriptEngine buildEngine() {
        final ScriptEngineManager factory = new ScriptEngineManager();
        return factory.getEngineByName(jsEngine);
    }

}
