package com.smockin.mockserver.service;

import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.persistence.dao.RestfulMockDAO;
import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import com.smockin.mockserver.service.dto.RestfulResponseDTO;
import com.smockin.utils.GeneralUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spark.Request;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Transactional
public class StatefulServiceImpl implements StatefulService {

    private final Logger logger = LoggerFactory.getLogger(StatefulServiceImpl.class);

    private final static String ID_FIELD = "id";

    /*
        Key: RestfulMock.externalId of stateful parent.
        Value: JSON String Data
    */
    private Map<String, List<Map<String, Object>>> state = new ConcurrentHashMap<>();

    @Autowired
    private RestfulMockDAO restfulMockDAO;

    @Override
    public RestfulResponseDTO process(final Request req, final RestfulMock mock) {

        final RestfulMock parent = loadStatefulParent(mock);

        if (!state.containsKey(parent.getExtId())) {
            state.put(parent.getExtId(), new ArrayList<>());
        }

        final List<Map<String, Object>> currentStateContentForMock = state.get(parent.getExtId());

        final int httpResponseCode;
        final String response;

        final Map<String, String> pathVars = GeneralUtils.findAllPathVars(req.pathInfo(), mock.getPath());
        final String id = pathVars.get(ID_FIELD);

        switch (RestMethodEnum.findByName(req.requestMethod())) {
            case GET:

                if (id != null) {

                    final Optional<Map<String, Object>> resultOpt = findStatefulDataById(id, currentStateContentForMock);

                    if (!resultOpt.isPresent()) {
                        response = null;
                        httpResponseCode = HttpStatus.SC_NOT_FOUND;
                        break;
                    }

                    response = GeneralUtils.serialiseJson(resultOpt.get());

                } else {

                    response = GeneralUtils.serialiseJson(currentStateContentForMock);
                }

                httpResponseCode = HttpStatus.SC_OK;

                break;
            case POST:

                // Validate is valid json body and append ID if none present
                final Optional<Map<String, Object>> jsonDataMapOpt = convertToJsonMap(req.body());

                if (jsonDataMapOpt.isPresent()) {

                    final Map<String, Object> jsonDataMap = jsonDataMapOpt.get();

                    if (!jsonDataMap.containsKey(ID_FIELD)) {
                        jsonDataMap.put(ID_FIELD, GeneralUtils.generateUUID());
                    }

                    currentStateContentForMock.add(jsonDataMap);
                    state.put(parent.getExtId(), currentStateContentForMock);

                    response = null;
                    httpResponseCode = HttpStatus.SC_CREATED;

                    break;
                }

                response = "Invalid JSON in request body";
                httpResponseCode = HttpStatus.SC_BAD_REQUEST;

                break;
            case PUT:

                if (id == null) {
                    response = null;
                    httpResponseCode = HttpStatus.SC_BAD_REQUEST;
                    break;
                }

                // TODO

                response = null;
                httpResponseCode = HttpStatus.SC_NO_CONTENT;

                break;
            case PATCH:

                if (id == null) {
                    response = null;
                    httpResponseCode = HttpStatus.SC_BAD_REQUEST;
                    break;
                }

                // TODO

                response = null;
                httpResponseCode = HttpStatus.SC_NO_CONTENT;

                break;
            case DELETE:

                if (id == null) {
                    response = null;
                    httpResponseCode = HttpStatus.SC_BAD_REQUEST;
                    break;
                }

                final int originalSize = currentStateContentForMock.size();

                final List<Map<String, Object>> filteredCurrentStateContentForMock = currentStateContentForMock
                        .stream()
                        .filter(f ->
                            !(StringUtils.equals(id, (String)f.get(ID_FIELD))))
                        .collect(Collectors.toList());

                state.put(parent.getExtId(), filteredCurrentStateContentForMock);

                if (filteredCurrentStateContentForMock.size() < originalSize) {
                    httpResponseCode = HttpStatus.SC_NO_CONTENT;
                } else {
                    httpResponseCode = HttpStatus.SC_NOT_FOUND;
                }

                response = null;

                break;
            default:

                response = null;
                httpResponseCode = HttpStatus.SC_NOT_FOUND;
                break;
        }

        return new RestfulResponseDTO(httpResponseCode, ContentType.APPLICATION_JSON.getMimeType(), response);
    }

    @Override
    public void resetState(final String externalId, final String userToken) throws RecordNotFoundException {

        final RestfulMock restfulMock = restfulMockDAO.findByExtId(externalId);

        if (restfulMock == null) {
            throw new RecordNotFoundException();
        }

        final RestfulMock parent = loadStatefulParent(restfulMock);

        state.remove(parent.getExtId());

    }

    RestfulMock loadStatefulParent(final RestfulMock mock) {

        return (mock.getStatefulParent() != null)
                ? mock.getStatefulParent()
                : mock;
    }

    Optional<Map<String, Object>> findStatefulDataById(final String id,
                             final List<Map<String, Object>> currentStateContent) {

        return currentStateContent
                .stream()
                .filter(f -> {

                    try {
                        return (StringUtils.equals(id, (String)f.get(ID_FIELD)));
                    } catch (Throwable ex) {}

                    return false;
                })
                .findFirst();

    }

    Optional<Map<String, Object>> convertToJsonMap(final String json) {

        try {
            final Map<String, Object> dataMap = (Map<String, Object>)GeneralUtils.deserialiseJSONToMap(json);
            return Optional.of(dataMap);
        } catch (Throwable ex) {
            return Optional.empty();
        }

    }

}
