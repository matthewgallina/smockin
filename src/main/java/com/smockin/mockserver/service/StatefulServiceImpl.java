package com.smockin.mockserver.service;

import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import com.smockin.mockserver.service.dto.RestfulResponseDTO;
import com.smockin.utils.GeneralUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spark.Request;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Transactional
public class StatefulServiceImpl implements StatefulService {

    private final Logger logger = LoggerFactory.getLogger(StatefulServiceImpl.class);

    // Key: RestfulMock.externalId of stateful parent.
    // Value: JSON String Data
    private Map<String, List<String>> state = new ConcurrentHashMap<>();

    @Override
    public RestfulResponseDTO process(final Request req, final RestfulMock mock) {

        final RestfulMock parent = loadStatefulParent(mock);

        if (!state.containsKey(parent.getExtId())) {
            state.put(parent.getExtId(), new ArrayList<>());
        }

        final List<String> currentStateContent = state.get(parent.getExtId());

        final int httpResponseCode;
        final String response;

        switch (RestMethodEnum.findByName(req.requestMethod())) {
            case GET:

                final Map<String, String> pathVars = GeneralUtils.findAllPathVars(req.pathInfo(), mock.getPath());

                if (pathVars.containsKey("id")) {

                    final String id = pathVars.get("id");

                    response = currentStateContent.stream()
                            .filter(f -> {

                                try {
                                    final Map<String, ?> dataMap = GeneralUtils.deserialiseJSONToMap(f);
                                    return (dataMap.containsKey("id") && StringUtils.equals(id, (String)dataMap.get("id")));
                                } catch (Throwable ex) {}

                                return false;
                            })
                            .findFirst()
                            .orElse(null);

                } else {

                    response = toJsonList(currentStateContent);

                }

                httpResponseCode = HttpStatus.SC_OK;
                break;
            case POST:

                    // TODO validate is valid json body

                    currentStateContent.add(req.body());
                    state.put(parent.getExtId(), currentStateContent);

                response = null;
                httpResponseCode = HttpStatus.SC_CREATED;
                break;
            case PUT:

                response = null;
                httpResponseCode = HttpStatus.SC_NO_CONTENT;
                break;
            case PATCH:

                response = null;
                httpResponseCode = HttpStatus.SC_NO_CONTENT;
                break;
            case DELETE:

                response = null;
                httpResponseCode = HttpStatus.SC_NO_CONTENT;
                break;
            default:

                response = null;
                httpResponseCode = HttpStatus.SC_NOT_FOUND;
                break;
        }

        return new RestfulResponseDTO(httpResponseCode, ContentType.APPLICATION_JSON.getMimeType(), response);
    }

    @Override
    public void resetState(final RestfulMock mock) {

        final RestfulMock parent = loadStatefulParent(mock);

        state.remove(parent.getExtId());

    }

    RestfulMock loadStatefulParent(final RestfulMock mock) {

        return (mock.getStatefulParent() != null)
                ? mock.getStatefulParent()
                : mock;
    }

    String toJsonList(final List<String> currentStateContent) {

        final StringBuilder sb = new StringBuilder();

        sb.append("[");
        final String records = currentStateContent.stream().collect(Collectors.joining(","));
        sb.append(records);
        sb.append("]");

        return sb.toString();
    }

}
