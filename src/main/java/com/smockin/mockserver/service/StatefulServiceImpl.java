package com.smockin.mockserver.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.dao.RestfulMockDAO;
import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.admin.persistence.entity.RestfulMockStatefulMeta;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import com.smockin.admin.service.SmockinUserService;
import com.smockin.admin.service.utils.UserTokenServiceUtils;
import com.smockin.mockserver.service.dto.RestfulResponseDTO;
import com.smockin.mockserver.service.enums.PatchCommandEnum;
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
import java.util.concurrent.atomic.AtomicBoolean;
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

    @Autowired
    private UserTokenServiceUtils userTokenServiceUtils;

    @Autowired
    private SmockinUserService smockinUserService;


    @Override
    public RestfulResponseDTO process(final Request req, final RestfulMock mock) {

        final RestfulMock parent = loadStatefulParent(mock);

        final String sanitizedInboundPath = GeneralUtils.sanitizeMultiUserPath(smockinUserService.getUserMode(), req.pathInfo(), mock.getCreatedBy().getCtxPath());

        final List<Map<String, Object>> mockStateContent = loadStateForMock(parent);
        final Map<String, String> pathVars = GeneralUtils.findAllPathVars(sanitizedInboundPath, mock.getPath());
        final String fieldId = parent.getRestfulMockStatefulMeta().getIdFieldName();
        final String dataId = pathVars.get(fieldId);

        StatefulResponse statefulResponse;

        try {

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

        } catch (StatefulValidationException ex) {

            final int status = (ex.getStatus() != null)
                    ? ex.getStatus()
                    : HttpStatus.SC_BAD_REQUEST;

            statefulResponse = (ex.getMessage() != null)
                    ? new StatefulResponse(status, ex.getMessage())
                    : new StatefulResponse(status);
        }

        return new RestfulResponseDTO(statefulResponse.httpResponseCode,
                ContentType.APPLICATION_JSON.getMimeType(),
                statefulResponse.responseBody);
    }

    @Override
    public void resetState(final String externalId, final String userToken) throws RecordNotFoundException, ValidationException {
        logger.debug("resetState called");

        final RestfulMock restfulMock = restfulMockDAO.findByExtId(externalId);

        if (restfulMock == null) {
            throw new RecordNotFoundException();
        }

        final RestfulMock parent = loadStatefulParent(restfulMock);

        userTokenServiceUtils.validateRecordOwner(parent.getCreatedBy(), userToken);

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

        // TODO amend id handler here to add id according to the path...
        appendIdToJson(requestDataMap, restfulMockStatefulMeta);

        final String fieldIdPathPattern = restfulMockStatefulMeta.getIdFieldLocation();

        if (isComplexJsonStructure(fieldIdPathPattern)) {

            // TODO
            // Amend POST to add items according to path...

            state.put(parentExtId, currentStateContentForMock); // TODO use merge

        } else {

            state.merge(parentExtId, currentStateContentForMock, (currentValue, p) -> {
                currentValue.add(requestDataMap);
                return currentValue;
            });

        }

        return new StatefulResponse(HttpStatus.SC_CREATED);
    }

    StatefulResponse handleDelete(final String dataId,
                                  final String parentExtId,
                                  final List<Map<String, Object>> currentStateContentForMock,
                                  final RestfulMockStatefulMeta restfulMockStatefulMeta) {

        if (dataId == null) {
            return new StatefulResponse(HttpStatus.SC_BAD_REQUEST);
        }

        final String fieldIdPathPattern = restfulMockStatefulMeta.getIdFieldLocation();

        if (isComplexJsonStructure(fieldIdPathPattern)) {

            final Optional<StatefulServiceImpl.StatefulPath> pathOpt =
                    findDataStateRecordPath(currentStateContentForMock,
                                            StringUtils.split(fieldIdPathPattern,"."),
                                            dataId);

            if (!pathOpt.isPresent()) {
                return new StatefulResponse(HttpStatus.SC_NOT_FOUND);
            }

            // Drills down into path and removes specific object.
            removeDataStateRecordByPath(currentStateContentForMock, pathOpt.get().getPath());

            state.put(parentExtId, currentStateContentForMock); // TODO use merge

        } else {

            final String fieldId = restfulMockStatefulMeta.getIdFieldName();
            final int originalDataStateSize = currentStateContentForMock.size();

            final List<Map<String, Object>> filteredCurrentStateContentForMock
                    = state.merge(parentExtId, currentStateContentForMock, (currentValue, p) ->
                        currentValue
                            .stream()
                            .filter(f -> !(StringUtils.equals(dataId, (String) f.get(fieldId))))
                            .collect(Collectors.toList())
            );

            if (filteredCurrentStateContentForMock.size() == originalDataStateSize) {
                return new StatefulResponse(HttpStatus.SC_NOT_FOUND);
            }

        }

        return new StatefulResponse(HttpStatus.SC_NO_CONTENT);
    }

    StatefulResponse handlePut(final String dataId,
                               final String parentExtId,
                               final String requestBody,
                               final List<Map<String, Object>> currentStateContentForMock,
                               final RestfulMockStatefulMeta restfulMockStatefulMeta) {

        if (dataId == null) {
            return new StatefulResponse(HttpStatus.SC_BAD_REQUEST);
        }

        // Ensure json body is valid
        final Optional<Map<String, Object>> requestDataMapOpt = convertToJsonMap(requestBody);

        if (!requestDataMapOpt.isPresent()) {
            return new StatefulResponse(HttpStatus.SC_BAD_REQUEST,
                    "Invalid JSON in request body");
        }

        final String fieldIdPathPattern = restfulMockStatefulMeta.getIdFieldLocation();

        if (isComplexJsonStructure(fieldIdPathPattern)) {

            final Optional<StatefulServiceImpl.StatefulPath> pathOpt =
                    findDataStateRecordPath(currentStateContentForMock,
                            StringUtils.split(fieldIdPathPattern, "."),
                            dataId);

            if (!pathOpt.isPresent()) {
                return new StatefulResponse(HttpStatus.SC_NOT_FOUND);
            }

            currentStateContentForMock.remove(pathOpt.get().getIndex().intValue());
            currentStateContentForMock.add(pathOpt.get().getIndex(), requestDataMapOpt.get());

            state.put(parentExtId, currentStateContentForMock); // TODO use merge

        } else {

            final String fieldId = restfulMockStatefulMeta.getIdFieldName();
            final Object bodyId = requestDataMapOpt.get().get(fieldId);

            // Ensure ids in url and body match
            if (bodyId == null
                    || !(bodyId instanceof String)
                    || !StringUtils.equals((String)bodyId, dataId)) {
                return new StatefulResponse(HttpStatus.SC_BAD_REQUEST);
            }

            final AtomicBoolean recordFound = new AtomicBoolean(false);

            state.merge(parentExtId, currentStateContentForMock, (currentValue, p) ->
                currentValue
                        .stream()
                        .map(m -> {

                            final boolean match = StringUtils.equals(dataId, (String) m.get(fieldId));

                            if (match) {
                                recordFound.set(true);
                            }

                            return (match)
                                    ? requestDataMapOpt.get()
                                    : m;
                        })
                        .collect(Collectors.toList())
            );

            if (!recordFound.get()) {
                return new StatefulResponse(HttpStatus.SC_NOT_FOUND);
            }

        }

        return new StatefulResponse(HttpStatus.SC_NO_CONTENT);
    }

    // https://sookocheff.com/post/api/understanding-json-patch/
    // https://www.baeldung.com/spring-rest-json-patch

    // Valid PATCH operations are add, remove, replace, move, copy and test. Any other operation is considered an error.
    StatefulResponse handlePatch(final String dataId,
                                 final String parentExtId,
                                 final String requestBody,
                                 final List<Map<String, Object>> currentStateContentForMock,
                                 final RestfulMockStatefulMeta restfulMockStatefulMeta) throws StatefulValidationException {

        if (dataId == null) {
            return new StatefulResponse(HttpStatus.SC_BAD_REQUEST);
        }

        // TODO Should only accept following header content type, but will ignore this rule for now...
        // -H "Content-Type: application/json-patch+json"

        final Optional<Map<String, Object>> requestDataMapOpt = convertToJsonMap(requestBody);

        if (!requestDataMapOpt.isPresent()) {
            return new StatefulResponse(HttpStatus.SC_BAD_REQUEST,
                    "Invalid JSON in request body");
        }

        final Map<String, Object> requestDataMap = requestDataMapOpt.get();
        final String op = (String)requestDataMap.get("op");
        final String prefixedPath = (String)requestDataMap.get("path");
        final String prefixedFrom = (String)requestDataMap.get("from");
        final Object value = requestDataMap.get("value");

        if (op == null || prefixedPath == null) {
            return new StatefulResponse(HttpStatus.SC_BAD_REQUEST,
                    "Invalid JSON in request body, required 'op' and 'path' fields are missing");
        }

        final PatchCommandEnum patchCommand;

        try {
            patchCommand = PatchCommandEnum.valueOf(op);
        } catch (IllegalArgumentException ex) {
            return new StatefulResponse(HttpStatus.SC_BAD_REQUEST,
                    String.format(StatefulValidationException.INVALID_PATCH_INSTRUCTION, "'op' is not a valid value"));
        }

        if (!prefixedPath.startsWith("/")) {
            return new StatefulResponse(HttpStatus.SC_BAD_REQUEST,
                    String.format(StatefulValidationException.INVALID_PATCH_INSTRUCTION, "path should begin with '/' (e.g '/age'"));
        }

        final String path = prefixedPath.substring(1);
        final String fieldIdPathPattern = restfulMockStatefulMeta.getIdFieldLocation();

        if (isComplexJsonStructure(fieldIdPathPattern)) {

            final Optional<StatefulServiceImpl.StatefulPath> pathOpt =
                    findDataStateRecordPath(currentStateContentForMock,
                            StringUtils.split(fieldIdPathPattern, "."),
                            dataId);

            if (!pathOpt.isPresent()) {
                return new StatefulResponse(HttpStatus.SC_NOT_FOUND);
            }

            final Optional<Map<String, Object>> currentDataOpt = findDataStateRecordByPath(currentStateContentForMock, pathOpt.get().getPath());

            if (!currentDataOpt.isPresent()) {
                return new StatefulResponse(HttpStatus.SC_NOT_FOUND);
            }

            // TODO

            state.put(parentExtId, currentStateContentForMock); // TODO use merge

        } else {

            final String fieldId = restfulMockStatefulMeta.getIdFieldName();
            final AtomicBoolean recordFound = new AtomicBoolean(false);

            switch (patchCommand) {
                case ADD:

                    if (value == null) {
                        return new StatefulResponse(HttpStatus.SC_BAD_REQUEST,
                                String.format(StatefulValidationException.INVALID_PATCH_INSTRUCTION, "'value' is required"));
                    }

                    state.merge(parentExtId, currentStateContentForMock, (currentValue, nu) ->
                            currentValue
                                    .stream()
                                    .map(m -> {

                                        final boolean matchOnIdMade = StringUtils.equals(dataId, (String) m.get(fieldId));

                                        if (matchOnIdMade) {
                                            recordFound.set(true);
                                        }

                                        if (matchOnIdMade) {

                                            patchAddOperation(path, m, value, false);

                                            return m;
                                        } else {
                                            return m;
                                        }
                                    })
                                    .collect(Collectors.toList())
                    );

                    break;
                case REMOVE:

                    state.merge(parentExtId, currentStateContentForMock, (currentValue, nu) ->
                            currentValue
                                    .stream()
                                    .map(m -> {

                                        final boolean matchOnIdMade = StringUtils.equals(dataId, (String) m.get(fieldId));

                                        if (matchOnIdMade) {
                                            recordFound.set(true);
                                        }

                                        if (matchOnIdMade) {

                                            patchRemoveOperation(path, m);

                                            return m;
                                        } else {
                                            return m;
                                        }
                                    })
                                    .collect(Collectors.toList())
                    );

                    break;
                case REPLACE:

                    if (value == null) {
                        return new StatefulResponse(HttpStatus.SC_BAD_REQUEST,
                                String.format(StatefulValidationException.INVALID_PATCH_INSTRUCTION, "'value' is required"));
                    }

                    state.merge(parentExtId, currentStateContentForMock, (currentValue, nu) ->
                            currentValue
                                    .stream()
                                    .map(m -> {

                                        final boolean matchOnIdMade = StringUtils.equals(dataId, (String) m.get(fieldId));

                                        if (matchOnIdMade) {
                                            recordFound.set(true);
                                        }

                                        if (matchOnIdMade) {

                                            addReplaceOperation(path, m, value);

                                            return m;
                                        } else {
                                            return m;
                                        }
                                    })
                                    .collect(Collectors.toList())
                    );

                    break;
                case COPY:

                    if (prefixedFrom == null) {
                        return new StatefulResponse(HttpStatus.SC_BAD_REQUEST,
                                String.format(StatefulValidationException.INVALID_PATCH_INSTRUCTION, "'from' is required"));
                    }

                    if (!prefixedFrom.startsWith("/")) {
                        return new StatefulResponse(HttpStatus.SC_BAD_REQUEST,
                                String.format(StatefulValidationException.INVALID_PATCH_INSTRUCTION, "'from' should begin with '/' (e.g '/age'"));
                    }

                    final String fromInCopyOp = prefixedFrom.substring(1);

                    state.merge(parentExtId, currentStateContentForMock, (currentValue, nu) ->
                            currentValue
                                    .stream()
                                    .map(m -> {

                                        final boolean matchOnIdMade = StringUtils.equals(dataId, (String) m.get(fieldId));

                                        if (matchOnIdMade) {
                                            recordFound.set(true);
                                        }

                                        if (matchOnIdMade) {

                                            patchCopyOperation(fromInCopyOp, m, path);

                                            return m;
                                        } else {
                                            return m;
                                        }
                                    })
                                    .collect(Collectors.toList())
                    );

                    break;
                case MOVE:

                    if (prefixedFrom == null) {
                        return new StatefulResponse(HttpStatus.SC_BAD_REQUEST,
                                String.format(StatefulValidationException.INVALID_PATCH_INSTRUCTION, "'from' is required"));
                    }

                    if (!prefixedFrom.startsWith("/")) {
                        return new StatefulResponse(HttpStatus.SC_BAD_REQUEST,
                                String.format(StatefulValidationException.INVALID_PATCH_INSTRUCTION, "'from' should begin with '/' (e.g '/age'"));
                    }

                    final String fromInMoveOp = prefixedFrom.substring(1);


                    state.merge(parentExtId, currentStateContentForMock, (currentValue, nu) ->
                            currentValue
                                    .stream()
                                    .map(m -> {

                                        final boolean matchOnIdMade = StringUtils.equals(dataId, (String) m.get(fieldId));

                                        if (matchOnIdMade) {
                                            recordFound.set(true);
                                        }

                                        if (matchOnIdMade) {

                                            patchMoveOperation(fromInMoveOp, m, path);

                                            return m;
                                        } else {
                                            return m;
                                        }
                                    })
                                    .collect(Collectors.toList())
                    );

                    break;
                case TEST:

                    return new StatefulResponse(HttpStatus.SC_NOT_IMPLEMENTED, "PATCH 'TEST' operation is not supported");
                default:

                    return new StatefulResponse(HttpStatus.SC_NOT_IMPLEMENTED, "PATCH operation is not supported");
            }



            if (!recordFound.get()) {
                return new StatefulResponse(HttpStatus.SC_NOT_FOUND);
            }

        }

        return new StatefulResponse(HttpStatus.SC_NO_CONTENT);
    }


    Optional<Map<String, Object>> findStatefulDataById(final String id,
                                                       final List<Map<String, Object>> currentStateContent,
                                                       final RestfulMockStatefulMeta restfulMockStatefulMeta) {


        final String fieldIdPathPattern = restfulMockStatefulMeta.getIdFieldLocation();

        if (isComplexJsonStructure(fieldIdPathPattern)) {

            return findDataStateRecord(currentStateContent, fieldIdPathPattern, id);

        } else {

            final String fieldId = restfulMockStatefulMeta.getIdFieldName();

            return currentStateContent
                    .stream()
                    .filter(f -> (StringUtils.equals(id, String.valueOf(f.get(fieldId)))))
                    .findFirst();

        }

    }


    List<Map<String, Object>> loadStateForMock(final RestfulMock parent) {

        return state.computeIfAbsent(parent.getExtId(), k -> {

            final String initialBody = parent.getRestfulMockStatefulMeta().getInitialResponseBody();

            return (initialBody != null)
                    ? GeneralUtils.deserialiseJson(initialBody, new TypeReference<List<Map<String, Object>>>() {})
                    : new ArrayList<>();
        });

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

    void appendIdToJson(final Map<String, Object> jsonDataMap,
                        final RestfulMockStatefulMeta restfulMockStatefulMeta) {

        final String fieldIdPathPattern = restfulMockStatefulMeta.getIdFieldLocation();

        if (isComplexJsonStructure(fieldIdPathPattern)) {

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

            final String fieldId = restfulMockStatefulMeta.getIdFieldName();

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

        final String[] pathArray = StringUtils.split(fieldIdPathPattern, ".");

        final Optional<StatefulPath> jsonPathOpt = findDataStateRecordPath(allStateData, pathArray, targetId);

        if (!jsonPathOpt.isPresent()) {
            return Optional.empty();
        }

        return findDataStateRecordByPath(allStateData, jsonPathOpt.get().getPath());
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

    void removeDataStateRecordByPath(
            final List<Map<String, Object>> allStateDataSrc,
            final String path) {

        final int lastArrayEndPos = path.lastIndexOf("].");

        if (lastArrayEndPos == -1) {
            return;
        }

        final String amendedPath = StringUtils.substring(path, 0, (lastArrayEndPos + 1));
        final String[] pathArray = StringUtils.split(amendedPath,".");

        Object currentDataRecordObject = null;

        for (int i=0; i < pathArray.length; i++ ) {

            final String p = pathArray[i];

            if (i == 0 && currentDataRecordObject == null) {
                final Integer arrayPosition = extractArrayPosition(p);
                currentDataRecordObject = allStateDataSrc.get(arrayPosition);
                continue;
            }

            if (currentDataRecordObject == null) {
                return;
            }

            if (p.contains("[") && p.contains("]")) {

                // List

                final Integer arrayPosition = extractArrayPosition(p);

                if (arrayPosition == null) {
                    return;
                }

                if (i == (pathArray.length - 1)) {
                    ((List<Object>) currentDataRecordObject).remove(arrayPosition.intValue());
                } else {
                    currentDataRecordObject = ((List<Object>) currentDataRecordObject).get(arrayPosition);
                }

            } else {

                // Map

                currentDataRecordObject = ((Map<String, Object>) currentDataRecordObject).get(p);

            }

        }

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

    Optional<StatefulPath> findDataStateRecordPath(
            final List<Map<String, Object>> allStateData,
            final String[] pathArray,
            final String targetId) {

        final StatefulServiceImpl.StatefulSearchPathResult result = new StatefulServiceImpl.StatefulSearchPathResult();

        int index = 0;

        for (Map<String, Object> m : allStateData) {

            final int thisIndex = index;

            findStateIndex(pathArray, 0, targetId, m, result, "[" + index++ + "]");

            if (result.getPath().isPresent()) {
                return Optional.of(new StatefulPath(result.getPath().get(), thisIndex));
            }
        }

        return Optional.empty();
    }

    private void patchAddOperation(final String path, final Map<String, Object> matchedMap, final Object value, final boolean canOverwriteExisting) {

        if (path.contains("/")) {

            final String[] paths = path.split("/");

            Object obj = matchedMap;

            for (int i=0; i < paths.length; i++) {

                final String p = paths[i];
                final boolean lastIteration = (i == (paths.length - 1));

                if (obj instanceof List) {

                    final int indx = NumberUtils.toInt(p, -1);

                    if (indx == -1) {
                        throw new StatefulValidationException(String.format(StatefulValidationException.PATH_STRUCTURE_MISALIGN, path));
                    }

                    final List l = ((List)obj);

                    if (lastIteration) {

                        if (l.size() < indx) {
                            throw new StatefulValidationException(String.format(StatefulValidationException.PATH_OUT_OF_RANGE_LIST_INDEX, path, indx));
                        }

                        if (!l.isEmpty()
                                && !l.get(0).getClass().equals(value.getClass())) {
                            throw new StatefulValidationException(
                                    String.format(StatefulValidationException.INVALID_PATCH_INSTRUCTION,
                                            "'value' in path '" + path + "' has an incompatible data type with existing values in list"));
                        }

                        l.add(indx, value);

                    } else {

                        if (l.size() <= indx) {
                            throw new StatefulValidationException(String.format(StatefulValidationException.PATH_OUT_OF_RANGE_LIST_INDEX, path, indx));
                        }

                        obj = l.get(indx);

                    }

                } else if (obj instanceof Map) {

                    final Map map = ((Map)obj);

                    if (lastIteration) {

                        if (!canOverwriteExisting && map.containsKey(p)) {
                            throw new StatefulValidationException(
                                    String.format(StatefulValidationException.INVALID_PATCH_INSTRUCTION, "'path' value '" + path + "' already exists"));
                        }

                        map.put(p, value);
                    } else {
                        obj = map.get(p);
                    }

                } else {
                    throw new StatefulValidationException(String.format(StatefulValidationException.PATH_STRUCTURE_MISALIGN, path));
                }

            }

        } else {

            if (matchedMap.get(path) instanceof List) {

                final List l = ((List)matchedMap.get(path));

                if (!l.isEmpty()
                        && !l.get(0).getClass().equals(value.getClass())) {
                    throw new StatefulValidationException(
                            String.format(StatefulValidationException.INVALID_PATCH_INSTRUCTION,
                                    "'value' in path '" + path + "' has an incompatible data type with existing values in list"));
                }

                l.add(value);

            } else {

                // Map, String, Int, etc...

                if (!canOverwriteExisting && matchedMap.containsKey(path)) {
                    throw new StatefulValidationException(
                            String.format(StatefulValidationException.INVALID_PATCH_INSTRUCTION, "'path' value '" + path + "' already exists"));
                }

                matchedMap.put(path, value);
            }

        }

    }

    private void patchRemoveOperation(final String path, final Map<String, Object> matchedMap) {

        if (path.contains("/")) {

            final String[] paths = path.split("/");

            Object obj = matchedMap;

            for (int i=0; i < paths.length; i++) {

                final String p = paths[i];
                final boolean lastIteration = (i == (paths.length - 1));

                if (obj instanceof List) {

                    final List l = ((List)obj);

                    if (lastIteration && "-".equals(p)) {

                        l.remove(0);

                    } else {

                        final int indx = NumberUtils.toInt(p, -1);

                        if (indx == -1) {
                            throw new StatefulValidationException(String.format(StatefulValidationException.PATH_STRUCTURE_MISALIGN, path));
                        }

                        if (l.size() <= indx) {
                            throw new StatefulValidationException(String.format(StatefulValidationException.PATH_OUT_OF_RANGE_LIST_INDEX, path, indx));
                        }

                        if (lastIteration) {
                            l.remove(indx);
                        } else {
                            obj = l.get(indx);
                        }

                    }

                } else if (obj instanceof Map) {

                    final Map map = ((Map)obj);

                    if (lastIteration) {

                        if (!map.containsKey(p)) {
                            throw new StatefulValidationException(HttpStatus.SC_NOT_FOUND);
                        }

                        map.remove(p);
                    } else {
                        obj = map.get(p);
                    }

                } else {
                    throw new StatefulValidationException(String.format(StatefulValidationException.PATH_STRUCTURE_MISALIGN, path));
                }

            }

        } else {

            if (!matchedMap.containsKey(path)) {
                throw new StatefulValidationException(HttpStatus.SC_NOT_FOUND);
            }

            matchedMap.remove(path);

        }

    }

    private void patchCopyOperation(final String from, final Map<String, Object> matchedMap, final String path) {

        if (from.contains("/")) {

            final String[] fromPaths = from.split("/");

            Object obj = matchedMap;

            for (int i=0; i < fromPaths.length; i++) {

                final String fp = fromPaths[i];
                final boolean lastIteration = (i == (fromPaths.length - 1));

                if (obj instanceof List) {

                    final int indx = NumberUtils.toInt(fp, -1);

                    if (indx == -1) {
                        throw new StatefulValidationException(String.format(StatefulValidationException.FROM_STRUCTURE_MISALIGN, from));
                    }

                    final List l = ((List)obj);

                    if (lastIteration) {

                        if (l.size() < indx) {
                            throw new StatefulValidationException(String.format(StatefulValidationException.FROM_OUT_OF_RANGE_LIST_INDEX, from, indx));
                        }

                        if (l.get(indx) == null) {
                            throw new StatefulValidationException(HttpStatus.SC_NOT_FOUND);
                        }

                        patchAddOperation(path, matchedMap, l.get(indx), true);

                    } else {

                        if (l.size() <= indx) {
                            throw new StatefulValidationException(String.format(StatefulValidationException.FROM_OUT_OF_RANGE_LIST_INDEX, from, indx));
                        }

                        obj = l.get(indx);

                    }

                } else if (obj instanceof Map) {

                    final Map map = ((Map)obj);

                    if (lastIteration) {

                        if (!map.containsKey(fp)) {
                            throw new StatefulValidationException(HttpStatus.SC_NOT_FOUND);
                        }

                        patchAddOperation(path, matchedMap, map.get(from), true);

                    } else {
                        obj = map.get(fp);
                    }

                } else {
                    throw new StatefulValidationException(String.format(StatefulValidationException.FROM_STRUCTURE_MISALIGN, from));
                }

            }

        } else {

            if (!matchedMap.containsKey(from)) {
                throw new StatefulValidationException(HttpStatus.SC_NOT_FOUND);
            }

            patchAddOperation(path, matchedMap, matchedMap.get(from), true);

        }

    }

    private void patchMoveOperation(final String from, final Map<String, Object> matchedMap, final String path) {

        if (from.contains("/")) {

            final String[] fromPaths = from.split("/");

            Object obj = matchedMap;

            for (int i=0; i < fromPaths.length; i++) {

                final String fp = fromPaths[i];
                final boolean lastIteration = (i == (fromPaths.length - 1));

                if (obj instanceof List) {

                    final int indx = NumberUtils.toInt(fp, -1);

                    if (indx == -1) {
                        throw new StatefulValidationException(String.format(StatefulValidationException.FROM_STRUCTURE_MISALIGN, from));
                    }

                    final List l = ((List)obj);

                    if (lastIteration) {

                        if (l.size() < indx) {
                            throw new StatefulValidationException(String.format(StatefulValidationException.FROM_OUT_OF_RANGE_LIST_INDEX, from, indx));
                        }

                        if (l.get(indx) == null) {
                            throw new StatefulValidationException(HttpStatus.SC_NOT_FOUND);
                        }

                        patchAddOperation(path, matchedMap, l.get(indx), true);
                        patchRemoveOperation(from, matchedMap);

                    } else {

                        if (l.size() <= indx) {
                            throw new StatefulValidationException(String.format(StatefulValidationException.FROM_OUT_OF_RANGE_LIST_INDEX, from, indx));
                        }

                        obj = l.get(indx);

                    }

                } else if (obj instanceof Map) {

                    final Map map = ((Map)obj);

                    if (lastIteration) {

                        if (!map.containsKey(fp)) {
                            throw new StatefulValidationException(HttpStatus.SC_NOT_FOUND);
                        }

                        patchAddOperation(path, matchedMap, map.get(from), true);
                        patchRemoveOperation(from, matchedMap);

                    } else {
                        obj = map.get(fp);
                    }

                } else {
                    throw new StatefulValidationException(String.format(StatefulValidationException.FROM_STRUCTURE_MISALIGN, from));
                }

            }

        } else {

            if (!matchedMap.containsKey(from)) {
                throw new StatefulValidationException(HttpStatus.SC_NOT_FOUND);
            }

            patchAddOperation(path, matchedMap, matchedMap.get(from), true);
            patchRemoveOperation(from, matchedMap);

        }

    }

    private void addReplaceOperation(final String path, final Map<String, Object> matchedMap, final Object value) {

        if (path.contains("/")) {

            final String[] paths = path.split("/");

            Object obj = matchedMap;

            for (int i=0; i < paths.length; i++) {

                final String p = paths[i];
                final boolean lastIteration = (i == (paths.length - 1));

                if (obj instanceof List) {

                    final int indx = NumberUtils.toInt(p, -1);

                    if (indx == -1) {
                        throw new StatefulValidationException(String.format(StatefulValidationException.PATH_STRUCTURE_MISALIGN, path));
                    }

                    final List l = ((List)obj);

                    if (l.size() <= indx) {
                        throw new StatefulValidationException(String.format(StatefulValidationException.PATH_OUT_OF_RANGE_LIST_INDEX, path, indx));
                    }

                    if (lastIteration) {

                        if (!l.isEmpty()
                                && !l.get(0).getClass().equals(value.getClass())) {
                            throw new StatefulValidationException("'value' in path '" + path + "' has an incompatible data type with existing values in list");
                        }

                        l.set(indx, value);

                    } else {

                        if (l.size() <= indx) {
                            throw new StatefulValidationException(String.format(StatefulValidationException.PATH_OUT_OF_RANGE_LIST_INDEX, path, indx));
                        }

                        obj = l.get(indx);

                    }

                } else if (obj instanceof Map) {

                    final Map map = ((Map)obj);

                    if (lastIteration) {

                        if (!map.containsKey(p)) {
                            throw new StatefulValidationException(HttpStatus.SC_NOT_FOUND);
                        }
                        if (!map.get(p).getClass().equals(value.getClass())) {
                            throw new StatefulValidationException("'value' in path '" + path + "' has an incompatible data type with existing value");
                        }

                        map.remove(p);
                        map.put(p, value);

                    } else {
                        obj = map.get(p);
                    }

                } else {
                    throw new StatefulValidationException(String.format(StatefulValidationException.PATH_STRUCTURE_MISALIGN, path));
                }

            }

        } else {

            if (!matchedMap.containsKey(path)) {
                throw new StatefulValidationException(HttpStatus.SC_NOT_FOUND);
            }
            if (!matchedMap.get(path).getClass().equals(value.getClass())) {
                throw new StatefulValidationException("'value' in path '" + path + "' has an incompatible data type with existing value");
            }

            matchedMap.put(path, value);

        }

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

    private boolean isComplexJsonStructure(final String fieldIdPathPattern) {
        return fieldIdPathPattern != null
                && fieldIdPathPattern.indexOf(".") > -1;
    }

    final static class StatefulSearchPathResult {

        private Optional<String> path = Optional.empty();

        public Optional<String> getPath() {
            return path;
        }
    }

    final static class StatefulPath {

        private final String path;
        private final Integer index;

        public StatefulPath(String path, Integer index) {
            this.path = path;
            this.index = index;
        }

        public String getPath() {
            return path;
        }
        public Integer getIndex() {
            return index;
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

    private class StatefulValidationException extends RuntimeException {

        private static final String PATH_STRUCTURE_MISALIGN = "Invalid path '%s' does align with structure of existing JSON";
        private static final String FROM_STRUCTURE_MISALIGN = "Invalid from '%s' does align with structure of existing JSON";
        private static final String PATH_OUT_OF_RANGE_LIST_INDEX = "Invalid path '%s', list index %s is out of range";
        private static final String FROM_OUT_OF_RANGE_LIST_INDEX = "Invalid from '%s', list index %s is out of range";
        private static final String INVALID_PATCH_INSTRUCTION = "Invalid PATCH instruction in request body, %s";

        private final Integer status;

        public StatefulValidationException(final String msg) {
            super(msg);
            this.status = null;
        }

        public StatefulValidationException(final Integer status) {
            super();
            this.status = status;
        }

        public StatefulValidationException(final String msg, final Integer status) {
            super(msg);
            this.status = status;
        }

        public Integer getStatus() {
            return status;
        }
    }

}
