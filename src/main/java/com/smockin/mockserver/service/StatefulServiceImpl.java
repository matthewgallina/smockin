package com.smockin.mockserver.service;

import com.fasterxml.jackson.core.type.TypeReference;
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

    /*
        Key: RestfulMock.externalId of stateful parent.
        Value: JSON Data List
    */
    private final Map<String, List<Map<String, Object>>> state = new ConcurrentHashMap<>();

    @Autowired
    private RestfulMockDAO restfulMockDAO;

    @Override
    public RestfulResponseDTO process(final Request req, final RestfulMock mock) {

        final RestfulMock parent = loadStatefulParent(mock);

        final List<Map<String, Object>> mockStateContent = loadStateForMock(parent);
        final Map<String, String> pathVars = GeneralUtils.findAllPathVars(req.pathInfo(), mock.getPath());
        final String fieldId = parent.getRestfulMockStatefulMeta().getIdFieldName();
        final String dataId = pathVars.get(fieldId);

        final StatefulResponse statefulResponse;

        switch (RestMethodEnum.findByName(req.requestMethod())) {

            case GET:
                statefulResponse = handleGet(dataId, mockStateContent, fieldId);
                break;

            case POST:
                statefulResponse = handlePost(parent.getExtId(), req.body(), mockStateContent, fieldId);
                break;

            case PUT:
                statefulResponse = handlePut(dataId, parent.getExtId(), req.body(), mockStateContent, fieldId);
                break;

            case PATCH:
                statefulResponse = handlePatch(dataId, parent.getExtId(), req.body(), mockStateContent, fieldId);
                break;

            case DELETE:
                statefulResponse = handleDelete(dataId, parent.getExtId(), mockStateContent, fieldId);
                break;

            default:
                statefulResponse = new StatefulResponse(HttpStatus.SC_NOT_FOUND, "Invalid JSON in request body");
                break;
        }

        return new RestfulResponseDTO(statefulResponse.httpResponseCode,
                ContentType.APPLICATION_JSON.getMimeType(),
                statefulResponse.responseBody);
    }

    @Override
    public void resetState(final String externalId, final String userToken) throws RecordNotFoundException {
        logger.debug("resetState called");

        final RestfulMock restfulMock = restfulMockDAO.findByExtId(externalId);

        if (restfulMock == null) {
            throw new RecordNotFoundException();
        }

        final RestfulMock parent = loadStatefulParent(restfulMock);

        state.remove(parent.getExtId());

    }

    List<Map<String, Object>> loadStateForMock(final RestfulMock parent) {

        if (!state.containsKey(parent.getExtId())) {

            final String initialBody = parent.getRestfulMockStatefulMeta().getInitialResponseBody();

            if (initialBody != null) {
                state.put(parent.getExtId(), GeneralUtils.deserialiseJson(initialBody,
                        new TypeReference<List<Map<String, Object>>>() {}));
            } else {
                state.put(parent.getExtId(), new ArrayList<>());
            }
        }

        return state.get(parent.getExtId());
    }

    RestfulMock loadStatefulParent(final RestfulMock mock) {

        return (mock.getStatefulParent() != null)
                ? mock.getStatefulParent()
                : mock;
    }

    Optional<Map<String, Object>> findStatefulDataById(final String id,
                             final List<Map<String, Object>> currentStateContent,
                             final String fieldId) {

        return currentStateContent
                .stream()
                .filter(f -> {

                    try {
                        return (StringUtils.equals(id, (String)f.get(fieldId)));
                    } catch (Throwable ex) {}

                    return false;
                })
                .findFirst();

    }

    Optional<Map<String, Object>> convertToJsonMap(final String json) {

        try {
            final Map<String, Object> dataMap = (Map<String, Object>)GeneralUtils.deserialiseJSONToMap(json, false);
            return Optional.of(dataMap);
        } catch (Throwable ex) {
            return Optional.empty();
        }

    }

    void appendIdToJson(final Map<String, Object> jsonDataMap, final String fieldId) {

        // Append ID if none present
        if (!jsonDataMap.containsKey(fieldId)) {
            jsonDataMap.put(fieldId, GeneralUtils.generateUUID());
        }
    }

    StatefulResponse handleGet(final String dataId, final List<Map<String, Object>> currentStateContentForMock, final String fieldId) {

        // GET All
        if (dataId == null) {
            return new StatefulResponse(HttpStatus.SC_OK,
                    GeneralUtils.serialiseJson(currentStateContentForMock));
        }

        // GET by ID
        final Optional<Map<String, Object>> stateDataOpt =
                findStatefulDataById(dataId, currentStateContentForMock, fieldId);

        if (!stateDataOpt.isPresent()) {
            return new StatefulResponse(HttpStatus.SC_NOT_FOUND);
        }

        return new StatefulResponse(HttpStatus.SC_OK,
                GeneralUtils.serialiseJson(stateDataOpt.get()));
    }

    StatefulResponse handlePost(final String parentExtId, final String requestBody, final List<Map<String, Object>> currentStateContentForMock, final String fieldId) {

        // Validate is valid json body
        final Optional<Map<String, Object>> requestDataMapOpt = convertToJsonMap(requestBody);

        if (!requestDataMapOpt.isPresent()) {
            return new StatefulResponse(HttpStatus.SC_BAD_REQUEST,
                    "Invalid JSON in request body");
        }

        final Map<String, Object> requestDataMap = requestDataMapOpt.get();

        appendIdToJson(requestDataMap, fieldId);

        currentStateContentForMock.add(requestDataMap);

        state.put(parentExtId, currentStateContentForMock);

        return new StatefulResponse(HttpStatus.SC_CREATED);
    }

    StatefulResponse handleDelete(final String dataId, final String parentExtId, final List<Map<String, Object>> currentStateContentForMock, final String fieldId) {

        if (dataId == null) {
            return new StatefulResponse(HttpStatus.SC_BAD_REQUEST);
        }

        final int originalDataStateSize = currentStateContentForMock.size();

        final List<Map<String, Object>> filteredCurrentStateContentForMock = currentStateContentForMock
                .stream()
                .filter(f ->
                        !(StringUtils.equals(dataId, (String)f.get(fieldId))))
                .collect(Collectors.toList());

        state.put(parentExtId, filteredCurrentStateContentForMock);

        if (filteredCurrentStateContentForMock.size() == originalDataStateSize) {
            return new StatefulResponse(HttpStatus.SC_NOT_FOUND);
        }

        return new StatefulResponse(HttpStatus.SC_NO_CONTENT);
    }

    StatefulResponse handlePut(final String dataId, final String parentExtId, final String requestBody, final List<Map<String, Object>> currentStateContentForMock, final String fieldId) {

        if (dataId == null) {
            return new StatefulResponse(HttpStatus.SC_BAD_REQUEST);
        }

        // Ensure json body is valid
        final Optional<Map<String, Object>> requestDataMapOpt = convertToJsonMap(requestBody);

        if (!requestDataMapOpt.isPresent()) {
            return new StatefulResponse(HttpStatus.SC_BAD_REQUEST,
                    "Invalid JSON in request body");
        }

        // Check path and request body IDs match
        final Map<String, Object> requestDataMap = requestDataMapOpt.get();
        final String requestIdField = (String)requestDataMap.get(fieldId);

        if (requestIdField == null || !StringUtils.equals(requestIdField, dataId)) {
            return new StatefulResponse(HttpStatus.SC_BAD_REQUEST,
                    "Missing matching id in request body");
        }

        // Locate state in cache
        final Optional<Map<String, Object>> stateDataOpt = currentStateContentForMock
                .stream()
                .filter(f -> (StringUtils.equals(dataId, (String)f.get(fieldId))))
                .findFirst();

        if (!stateDataOpt.isPresent()) {
            return new StatefulResponse(HttpStatus.SC_NOT_FOUND);
        }

        /*
        final List<Map<String, Object>> filteredCurrentStateContentForMock = currentStateContentForMock
                .stream()
                .map(f -> {
                    if (StringUtils.equals(dataId, (String)f.get(ID_FIELD))) {
                        return jsonDataMap;
                    }
                    return f;
                })
                .collect(Collectors.toList());
*/

        stateDataOpt.get().clear();
        stateDataOpt.get().putAll(requestDataMap);

        state.put(parentExtId, currentStateContentForMock);

        return new StatefulResponse(HttpStatus.SC_NO_CONTENT);
    }

    StatefulResponse handlePatch(final String dataId, final String parentExtId, final String requestBody, final List<Map<String, Object>> currentStateContentForMock, final String fieldId) {

        if (dataId == null) {
            return new StatefulResponse(HttpStatus.SC_BAD_REQUEST);
        }

        // Ensure json body is valid
        final Optional<Map<String, Object>> requestDataMapOpt = convertToJsonMap(requestBody);

        if (!requestDataMapOpt.isPresent()) {
            return new StatefulResponse(HttpStatus.SC_BAD_REQUEST,
                    "Invalid JSON in request body");
        }

        // Check path and request body IDs match
        final Map<String, Object> requestDataMap = requestDataMapOpt.get();
        final String reqIdField = (String)requestDataMap.get(fieldId);

        if (reqIdField == null || !StringUtils.equals(reqIdField, dataId)) {
            return new StatefulResponse(HttpStatus.SC_BAD_REQUEST,
                    "Missing matching id in request body");
        }

        // Locate state in cache
        final Optional<Map<String, Object>> stateDataOpt = currentStateContentForMock
                .stream()
                .filter(f -> (StringUtils.equals(dataId, (String)f.get(fieldId))))
                .findFirst();

        if (!stateDataOpt.isPresent()) {
            return new StatefulResponse(HttpStatus.SC_NOT_FOUND);
        }


        // TODO swap around child fields in 'stateData' with those from 'requestDataMap'
        final Map<String, Object> stateData = stateDataOpt.get();
        // requestDataMap;


        state.put(parentExtId, currentStateContentForMock);

        return new StatefulResponse(HttpStatus.SC_NO_CONTENT);
    }

    private final static class StatefulResponse {

        private final int httpResponseCode;
        private final String responseBody;

        public StatefulResponse(int httpResponseCode) {
            this.httpResponseCode = httpResponseCode;
            this.responseBody = null;
        }
        public StatefulResponse(int httpResponseCode, String responseBody) {
            this.httpResponseCode = httpResponseCode;
            this.responseBody = responseBody;
        }

    }

}
