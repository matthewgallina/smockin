package com.smockin.mockserver.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.persistence.dao.RestfulMockDAO;
import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.admin.persistence.entity.RestfulMockStatefulMeta;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import com.smockin.mockserver.service.dto.RestfulResponseDTO;
import com.smockin.utils.GeneralUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spark.Request;
import java.io.Serializable;
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
                statefulResponse = handleGet(dataId, mockStateContent, parent.getRestfulMockStatefulMeta());
                break;

            case POST:
                statefulResponse = handlePost(parent.getExtId(), req.body(), mockStateContent, parent.getRestfulMockStatefulMeta());
                break;

            case PUT:
                statefulResponse = handlePut(dataId, parent.getExtId(), req.body(), mockStateContent, parent.getRestfulMockStatefulMeta());
                break;

            case PATCH:
                statefulResponse = handlePatch(dataId, parent.getExtId(), req.body(), mockStateContent, parent.getRestfulMockStatefulMeta());
                break;

            case DELETE:
                statefulResponse = handleDelete(dataId, parent.getExtId(), mockStateContent, parent.getRestfulMockStatefulMeta());
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

        // TODO userToken

        final RestfulMock parent = loadStatefulParent(restfulMock);

        state.remove(parent.getExtId());

    }

    StatefulResponse handleGet(final String dataId, final List<Map<String, Object>> currentStateContentForMock, final RestfulMockStatefulMeta restfulMockStatefulMeta) {

        // GET All
        if (dataId == null) {
            return new StatefulResponse(HttpStatus.SC_OK,
                    GeneralUtils.serialiseJson(currentStateContentForMock));
        }

        // GET by ID
        final Optional<Map<String, Object>> stateDataOpt =
                findStatefulDataById(dataId, currentStateContentForMock, restfulMockStatefulMeta);

        if (!stateDataOpt.isPresent()) {
            return new StatefulResponse(HttpStatus.SC_NOT_FOUND);
        }

        return new StatefulResponse(HttpStatus.SC_OK,
                GeneralUtils.serialiseJson(stateDataOpt.get()));
    }

    StatefulResponse handlePost(final String parentExtId, final String requestBody, final List<Map<String, Object>> currentStateContentForMock, final RestfulMockStatefulMeta restfulMockStatefulMeta) {

        // Validate is valid json body
        final Optional<Map<String, Object>> requestDataMapOpt = convertToJsonMap(requestBody);

        if (!requestDataMapOpt.isPresent()) {
            return new StatefulResponse(HttpStatus.SC_BAD_REQUEST,
                    "Invalid JSON in request body");
        }

        final Map<String, Object> requestDataMap = requestDataMapOpt.get();

        final String fieldId = restfulMockStatefulMeta.getIdFieldName();
        final String fieldIdPathPattern = restfulMockStatefulMeta.getIdFieldLocation();

        appendIdToJson(requestDataMap, fieldId, fieldIdPathPattern);

        currentStateContentForMock.add(requestDataMap);

        state.put(parentExtId, currentStateContentForMock);

        return new StatefulResponse(HttpStatus.SC_CREATED);
    }

    StatefulResponse handleDelete(final String dataId, final String parentExtId, final List<Map<String, Object>> currentStateContentForMock, final RestfulMockStatefulMeta restfulMockStatefulMeta) {

        if (dataId == null) {
            return new StatefulResponse(HttpStatus.SC_BAD_REQUEST);
        }

        final int originalDataStateSize = currentStateContentForMock.size();

        final String fieldId = restfulMockStatefulMeta.getIdFieldName();
        final String fieldIdPathPattern = restfulMockStatefulMeta.getIdFieldLocation(); // TODO

        final String[] pathArray =
                (fieldIdPathPattern != null && fieldIdPathPattern.indexOf(".") > -1)
                    ?  StringUtils.split(fieldIdPathPattern, ".")
                    : null;

        final List<Map<String, Object>> filteredCurrentStateContentForMock = currentStateContentForMock
                .stream()
                .filter(f -> {

                    return !(StringUtils.equals(dataId, (String) f.get(fieldId)));

                })
                .collect(Collectors.toList());

        state.put(parentExtId, filteredCurrentStateContentForMock);

        if (filteredCurrentStateContentForMock.size() == originalDataStateSize) {
            return new StatefulResponse(HttpStatus.SC_NOT_FOUND);
        }

        return new StatefulResponse(HttpStatus.SC_NO_CONTENT);
    }

    StatefulResponse handlePut(final String dataId, final String parentExtId, final String requestBody, final List<Map<String, Object>> currentStateContentForMock, final RestfulMockStatefulMeta restfulMockStatefulMeta) {

        if (dataId == null) {
            return new StatefulResponse(HttpStatus.SC_BAD_REQUEST);
        }

        // Ensure json body is valid
        final Optional<Map<String, Object>> requestDataMapOpt = convertToJsonMap(requestBody);

        if (!requestDataMapOpt.isPresent()) {
            return new StatefulResponse(HttpStatus.SC_BAD_REQUEST,
                    "Invalid JSON in request body");
        }

        final String fieldId = restfulMockStatefulMeta.getIdFieldName();
        final String fieldIdPathPattern = restfulMockStatefulMeta.getIdFieldLocation(); // TODO

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

    StatefulResponse handlePatch(final String dataId, final String parentExtId, final String requestBody, final List<Map<String, Object>> currentStateContentForMock, final RestfulMockStatefulMeta restfulMockStatefulMeta) {

        if (dataId == null) {
            return new StatefulResponse(HttpStatus.SC_BAD_REQUEST);
        }

        // Ensure json body is valid
        final Optional<Map<String, Object>> requestDataMapOpt = convertToJsonMap(requestBody);

        if (!requestDataMapOpt.isPresent()) {
            return new StatefulResponse(HttpStatus.SC_BAD_REQUEST,
                    "Invalid JSON in request body");
        }

        final String fieldId = restfulMockStatefulMeta.getIdFieldName();
        final String fieldIdPathPattern = restfulMockStatefulMeta.getIdFieldLocation(); // TODO

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

    Optional<Map<String, Object>> findStatefulDataById(final String id,
                                                       final List<Map<String, Object>> currentStateContent,
                                                       final RestfulMockStatefulMeta restfulMockStatefulMeta) {

        final String fieldId = restfulMockStatefulMeta.getIdFieldName();
        final String fieldIdPathPattern = restfulMockStatefulMeta.getIdFieldLocation();

        return currentStateContent
                .stream()
                .filter(f -> {

                    try {

                        if (fieldIdPathPattern != null && fieldIdPathPattern.indexOf(".") > -1) {

                            final String[] pathArray = StringUtils.split(fieldIdPathPattern, ".");

                            int index = 0;
                            Object currentJsonObject = f;

                            for (String pathElement : pathArray) {

                                if (currentJsonObject == null) {
                                    return false;
                                }

                                Map<String, Object> currentJsonObjectMap = null;

                                if (currentJsonObject instanceof Map) {

                                    currentJsonObjectMap = (Map<String, Object>)currentJsonObject;

                                } else if (currentJsonObject instanceof List) {

                                    final List<Map<String, Object>> currentJsonObjectList = (List<Map<String, Object>>)currentJsonObject;

                                    if (!currentJsonObjectList.isEmpty()) {
                                        currentJsonObjectMap = currentJsonObjectList.get(0); // TODO make this recursive
                                    }

                                }

                                if (index == (pathArray.length -1)) {

                                    return (currentJsonObjectMap != null
                                            && StringUtils.equals(id, (String)currentJsonObjectMap.get(pathElement)));

                                } else {

                                    if (currentJsonObjectMap != null
                                            && currentJsonObjectMap.containsKey(pathElement)) {
                                        currentJsonObject = currentJsonObjectMap.get(pathElement);
                                    }

                                }

                                index++;
                            }

                        } else {

                            return (StringUtils.equals(id, (String)f.get(fieldId)));
                        }

                    } catch (Throwable ex) {}

                    return false;
                })
                .findFirst();

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

    Optional<Map<String, Object>> convertToJsonMap(final String json) {

        try {
            final Map<String, Object> dataMap = (Map<String, Object>)GeneralUtils.deserialiseJSONToMap(json, false);
            return Optional.of(dataMap);
        } catch (Throwable ex) {
            return Optional.empty();
        }

    }

    void appendIdToJson(final Map<String, Object> jsonDataMap, final String fieldId, final String fieldIdPathPattern) {

        if (fieldIdPathPattern != null && fieldIdPathPattern.indexOf(".") > -1) {
            final String[] pathArray = StringUtils.split(fieldIdPathPattern, ".");

            int index = 0;
            Object currentJsonObject = jsonDataMap;

            for (String e : pathArray) {

                if (currentJsonObject == null) {
                    break;
                }

                final Optional<Object> currentJsonObjectOpt = appendIdToJsonIdLocator(currentJsonObject, index, e, pathArray.length);

                if (!currentJsonObjectOpt.isPresent()) {
                    break;
                }

                currentJsonObject = currentJsonObjectOpt.get();

                /*
                Map<String, Object> currentJsonObjectMap = null;

                if (currentJsonObject instanceof Map) {

                    currentJsonObjectMap = (Map<String, Object>)currentJsonObject;

                } else if (currentJsonObject instanceof List) {

                    final List<Map<String, Object>> currentJsonObjectList = (List<Map<String, Object>>)currentJsonObject;

                    if (!currentJsonObjectList.isEmpty()) {
                        currentJsonObjectMap = currentJsonObjectList.get(0);
                    }

                }

                if (index == (pathArray.length -1)) {

                    if (currentJsonObjectMap != null
                            && !currentJsonObjectMap.containsKey(e)) {
                        currentJsonObjectMap.put(e, GeneralUtils.generateUUID());
                    }

                    break;
                }

                if (currentJsonObjectMap.containsKey(e)) {
                    currentJsonObject = currentJsonObjectMap.get(e);
                }
                */

                index++;
            }

        } else {

            // Append ID if none present
            if (!jsonDataMap.containsKey(fieldId)) {
                jsonDataMap.put(fieldId, GeneralUtils.generateUUID());
            }

        }

    }

    Optional<Object> appendIdToJsonIdLocator(Object currentJsonObject,
                                         final int index,
                                         final String path,
                                         final int pathArrayLength) {

        Map<String, Object> currentJsonObjectMap = null;

        if (currentJsonObject instanceof Map) {

            currentJsonObjectMap = (Map<String, Object>)currentJsonObject;

        } else if (currentJsonObject instanceof List) {

            final List<Map<String, Object>> currentJsonObjectList = (List<Map<String, Object>>)currentJsonObject;

            if (!currentJsonObjectList.isEmpty()) {
                currentJsonObjectMap = currentJsonObjectList.get(0);
            }

        }

        if (index == (pathArrayLength -1)) {

            if (currentJsonObjectMap != null
                    && !currentJsonObjectMap.containsKey(path)) {
                currentJsonObjectMap.put(path, GeneralUtils.generateUUID());
            }

            return Optional.empty();
        }

        if (currentJsonObjectMap.containsKey(path)) {
            return Optional.of(currentJsonObjectMap.get(path));
        }

        return Optional.of(currentJsonObject);
    }

    Optional<Map<String, Object>> findDataStateRecord(
            final List<Map<String, Object>> allStateData,
            final String fieldIdPathPattern,
            final String targetId) {

        final String[] pathArray =
                (fieldIdPathPattern != null && fieldIdPathPattern.indexOf(".") > -1)
                        ?  StringUtils.split(fieldIdPathPattern, ".")
                        : null;

        final Optional<String> jsonPath = findDataStateRecordPath(allStateData, pathArray, targetId);

        if (!jsonPath.isPresent()) {
            return Optional.empty();
        }

        return findDataStateRecordByPath(allStateData, jsonPath.get());
    }

    Optional<Map<String, Object>> findDataStateRecordByPath(
            final List<Map<String, Object>> allStateDataSrc,
            final String path) {

        final List<Map<String, Object>> allStateDataCopy
                = SerializationUtils.clone(new StatefulSearchData(allStateDataSrc)).getData();

        final String[] pathArray = StringUtils.split(path,".");

        Map<String, Object> mainDataRecord = null;
        Object currentDataRecordObject = null;

        for (String p : pathArray) {

            if (mainDataRecord == null) {
                final Integer arrayPosition = extractArrayPosition(p);
                mainDataRecord = allStateDataCopy.get(arrayPosition);
                currentDataRecordObject = mainDataRecord;
                continue;
            }

            if (mainDataRecord == null || currentDataRecordObject == null) {
                return Optional.empty();
            }

            if (p.contains("[") && p.contains("]")) {

                // List

                final Integer arrayPosition = extractArrayPosition(p);

                if (arrayPosition == null) {
                    return Optional.empty();
                }

                Iterator<Object> dataRecordListItr = ((List<Object>) currentDataRecordObject).iterator();

                int dataRecordListItrIdx = 0;

                while (dataRecordListItr.hasNext()) {

                    Object o = dataRecordListItr.next();

                    if (arrayPosition != dataRecordListItrIdx) {
                        dataRecordListItr.remove();
                    } else {
                        currentDataRecordObject = o;
                    }

                    dataRecordListItrIdx++;
                }

            } else if (p.contains("=")) {

                // ID matching

                final String[] args = StringUtils.split(p,"=");
                final String idName = args[0];
                final String idValue = args[1];

                final String actualIdValue = (String)((Map<String, Object>)currentDataRecordObject).get(idName);

                if (!StringUtils.equals(idValue, actualIdValue)) {
                    return Optional.empty();
                }

            } else {

                // Map

                currentDataRecordObject = ((Map<String, Object>) currentDataRecordObject).get(p);

            }

        }

        return Optional.ofNullable(mainDataRecord);
    }

    Integer extractArrayPosition(final String pathElement) {

        if (StringUtils.isBlank(pathElement)) {
            return null;
        }

        final String s1 = StringUtils.remove(pathElement, "[");
        final String s2 = StringUtils.remove(s1, "]");
        final int result = NumberUtils.toInt(s2, -1);

        return (result != -1) ? result : null;
    }

    Optional<String> findDataStateRecordPath(
            final List<Map<String, Object>> allStateData,
            final String[] pathArray,
            final String targetId) {

        final StatefulServiceImpl.StatefulSearchPathResult result = new StatefulServiceImpl.StatefulSearchPathResult();

        int index = 0;

        for (Map<String, Object> m : allStateData) {

            findStateIndex(pathArray, 0, targetId, m, result, "[" + index++ + "]");

            if (result.getPath().isPresent()) {
                return result.getPath();
            }
        }

        return Optional.empty();
    }

    private void findStateIndex(
            final String[] pathArray,
            final int pathLevel,
            final String targetId,
            final Object currentJsonObject,
            final StatefulSearchPathResult result,
            final String myPath) {

        if (currentJsonObject == null) {
            return;
        }

        if (currentJsonObject instanceof String) {

            if (pathLevel == pathArray.length
                    && currentJsonObject instanceof String
                    && StringUtils.equals(targetId, (String)currentJsonObject)) {
                result.path = Optional.of(myPath + "=" + currentJsonObject);
            }

            return;

        } else if (currentJsonObject instanceof Map) {

            if (pathLevel == pathArray.length) {
                return;
            }

            final String currentField = pathArray[pathLevel];

            Map<String, Object> data = ((Map<String, Object>)currentJsonObject);

            findStateIndex(pathArray, pathLevel+1, targetId, data.get(currentField), result, myPath + "." + currentField);

        } else if (currentJsonObject instanceof List) {

            if (pathLevel == pathArray.length) {
                return;
            }

            final List<Map<String, Object>> currentJsonObjectList = (List<Map<String, Object>>)currentJsonObject;

            if (currentJsonObjectList.isEmpty()) {
                return;
            }

            int index = 0;
            for (Map<String, Object> mapElement : currentJsonObjectList) {
                findStateIndex(pathArray, pathLevel, targetId, mapElement, result, myPath + ".["+ (index++) + "]");
            }

        }

    }

    final static class StatefulSearchPathResult {

        private Optional<String> path = Optional.empty();

        public Optional<String> getPath() {
            return path;
        }
    }

    final static class StatefulSearchData implements Serializable {

        private final List<Map<String, Object>> data;

        public StatefulSearchData(final List<Map<String, Object>> data) {
            this.data = data;
        }

        public List<Map<String, Object>> getData() {
            return data;
        }
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
