package com.smockin.admin.service;

import com.smockin.admin.dto.RestfulMockDTO;
import com.smockin.admin.dto.response.RestfulMockResponseDTO;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.dao.RestfulMockDAO;
import com.smockin.admin.persistence.dao.RestfulMockDefinitionRuleDAO;
import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import com.smockin.admin.persistence.enums.RestMockTypeEnum;
import com.smockin.admin.service.utils.RestfulMockServiceUtils;
import com.smockin.admin.service.utils.UserTokenServiceUtils;
import com.smockin.mockserver.service.MockOrderingCounterService;
import com.smockin.utils.GeneralUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * Created by mgallina.
 */
@Service
@Transactional
public class RestfulMockServiceImpl implements RestfulMockService {

    private final Logger logger = LoggerFactory.getLogger(RestfulMockServiceImpl.class);

    @Autowired
    private RestfulMockDAO restfulMockDAO;

    @Autowired
    private RestfulMockDefinitionRuleDAO restfulMockDefinitionRuleDAO;

    @Autowired
    private RestfulMockServiceUtils restfulMockServiceUtils;

    @Autowired
    private UserTokenServiceUtils userTokenServiceUtils;

    @Autowired
    private SmockinUserService smockinUserService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private MockOrderingCounterService mockOrderingCounterService;

    @Override
    public RestfulMockResponseDTO loadEndpoint(final String mockExtId, final String token) throws RecordNotFoundException, ValidationException {
        logger.debug("loadEndpoint called");

        final RestfulMock mock = loadRestMock(mockExtId);

        userTokenServiceUtils.validateRecordOwner(mock.getCreatedBy(), token);

        return restfulMockServiceUtils.buildRestfulMockDefinitionDTO(mock);
    }

    @Override
    public String createEndpoint(final RestfulMockDTO dto, final String token) throws RecordNotFoundException, ValidationException {
        logger.debug("createEndpoint called");

        restfulMockServiceUtils.amendPath(dto);

        final SmockinUser smockinUser = userTokenServiceUtils.loadCurrentUser(token);

        RestfulMock mainMock = buildRestfulMock(dto, smockinUser);

        if (RestMockTypeEnum.STATEFUL.equals(dto.getMockType())) {

            mainMock.setMethod(RestMethodEnum.GET);
            mainMock.setStatefulDefaultResponseBody(dto.getStatefulDefaultResponseBody());
            mainMock = restfulMockDAO.save(mainMock);

            createStatefulChildMocks(dto, mainMock, smockinUser);

        } else {

            restfulMockServiceUtils.handleCustomJsSyntax(dto, mainMock);
            restfulMockServiceUtils.populateEndpointDefinitionsAndRules(dto, mainMock);

            mainMock = restfulMockDAO.save(mainMock);

        }

        restfulMockServiceUtils.handleEndpointOrdering();

        return mainMock.getExtId();
    }

    @Override
    public void updateEndpoint(final String mockExtId, final RestfulMockDTO dto, final String token) throws RecordNotFoundException, ValidationException {
        logger.debug("updateEndpoint called");

        restfulMockServiceUtils.amendPath(dto);

        final RestfulMock mock = loadRestMock(mockExtId);

        userTokenServiceUtils.validateRecordOwner(mock.getCreatedBy(), token);

        final boolean pathChanged = (!mock.getPath().equalsIgnoreCase(dto.getPath()));

        if (RestMockTypeEnum.STATEFUL.equals(mock.getMockType())) { // existing mock is STATEFUL...

            handleStatefulMockUpdate(dto, mock);

        } else if (RestMockTypeEnum.STATEFUL.equals(dto.getMockType())) { // existing mock IS NOT STATEFUL but being updated to a STATEFUL type...

            dto.setMethod(RestMethodEnum.GET);
            handleMockFieldsUpdate(dto, mock);

            createStatefulChildMocks(dto, mock, mock.getCreatedBy());

        } else {

            handleMockFieldsUpdate(dto, mock);

        }

        if (pathChanged) {
            restfulMockServiceUtils.handleEndpointOrdering();
        }

    }

    @Override
    public void deleteEndpoint(final String mockExtId, final String token) throws RecordNotFoundException, ValidationException {
        logger.debug("deleteEndpoint called");

        final RestfulMock mock = loadRestMock(mockExtId);

        userTokenServiceUtils.validateRecordOwner(mock.getCreatedBy(), token);

        if (RestMockTypeEnum.STATEFUL.equals(mock.getMockType())) {

            final RestfulMock parent = loadStatefulParent(mock);

            parent.getStatefulChildren().clear();
            restfulMockDAO.saveAndFlush(parent);

        }

        restfulMockDAO.delete(mock);
    }

    @Override
    public List<RestfulMockResponseDTO> loadAll(final String token) throws RecordNotFoundException {
        logger.debug("loadAll called");

        return restfulMockServiceUtils.buildRestfulMockDefinitionDTOs(restfulMockDAO.findAllByUser(userTokenServiceUtils.loadCurrentUser(token).getId()));
    }

    RestfulMock loadRestMock(final String mockExtId) throws RecordNotFoundException {
        logger.debug("loadRestMock called");

        final RestfulMock mock = restfulMockDAO.findByExtId(mockExtId);

        if (mock == null)
            throw new RecordNotFoundException();

        return mock;
    }

    RestfulMock buildRestfulMock(final RestfulMockDTO dto, final SmockinUser smockinUser) {

        return new RestfulMock(
                restfulMockServiceUtils.formatInboundPathVarArgs(dto.getPath()),
                dto.getMethod(),
                dto.getStatus(),
                dto.getMockType(),
                dto.getProxyTimeoutInMillis(),
                dto.getWebSocketTimeoutInMillis(),
                dto.getSseHeartBeatInMillis(),
                dto.isProxyPushIdOnConnect(),
                dto.isRandomiseDefinitions(),
                dto.isProxyForwardWhenNoRuleMatch(),
                smockinUser,
                dto.isRandomiseLatency(),
                dto.getRandomiseLatencyRangeMinMillis(),
                dto.getRandomiseLatencyRangeMaxMillis(),
                (dto.getProjectId() != null) ? projectService.loadByExtId(dto.getProjectId()) : null);

    }

    void handleMockFieldsUpdate(final RestfulMockDTO dto, final RestfulMock mock)
            throws ValidationException {

        mock.getDefinitions().clear();
        mock.getRules().clear();
        restfulMockDAO.saveAndFlush(mock);

        mock.setMockType(dto.getMockType());
        mock.setPath(restfulMockServiceUtils.formatInboundPathVarArgs(dto.getPath()));
        mock.setMethod(dto.getMethod());
        mock.setStatus(dto.getStatus());
        mock.setProxyTimeOutInMillis(dto.getProxyTimeoutInMillis());
        mock.setWebSocketTimeoutInMillis(dto.getWebSocketTimeoutInMillis());
        mock.setSseHeartBeatInMillis(dto.getSseHeartBeatInMillis());
        mock.setProxyPushIdOnConnect(dto.isProxyPushIdOnConnect());
        mock.setRandomiseDefinitions(dto.isRandomiseDefinitions());
        mock.setProxyForwardWhenNoRuleMatch(dto.isProxyForwardWhenNoRuleMatch());
        mock.setLastUpdated(GeneralUtils.getCurrentDate()); // force update to lastUpdated, as changes to child records do not otherwise change this
        mock.setRandomiseLatency(dto.isRandomiseLatency());
        mock.setRandomiseLatencyRangeMinMillis(dto.getRandomiseLatencyRangeMinMillis());
        mock.setRandomiseLatencyRangeMaxMillis(dto.getRandomiseLatencyRangeMaxMillis());
        mock.setStatefulDefaultResponseBody(dto.getStatefulDefaultResponseBody());

        if (dto.getProjectId() != null)
            mock.setProject(projectService.loadByExtId(dto.getProjectId()));

        restfulMockServiceUtils.handleCustomJsSyntax(dto, mock);

        restfulMockServiceUtils.populateEndpointDefinitionsAndRules(dto, mock);

        restfulMockDAO.save(mock);

        if (RestMockTypeEnum.SEQ.equals(mock.getMockType())) {
            mockOrderingCounterService.clearMockStateById(mock.getExtId());
        }

    }

    void updateStatefulMockTypeFields(final RestfulMockDTO dto, final RestfulMock mock) {

        final RestMethodEnum method = mock.getMethod();
        final String originalPath = dto.getPath();
        final String varPath = dto.getPath() + "/:id";
        final boolean isParent = (mock.getStatefulParent() == null);

        if (!isParent
                && (RestMethodEnum.GET.equals(method)
                || RestMethodEnum.PUT.equals(method)
                || RestMethodEnum.PATCH.equals(method)
                || RestMethodEnum.DELETE.equals(method))) {
            mock.setPath(restfulMockServiceUtils.formatInboundPathVarArgs(varPath));
        } else {
            mock.setPath(restfulMockServiceUtils.formatInboundPathVarArgs(originalPath));
        }

        mock.setStatus(dto.getStatus());

        if (isParent) {
            mock.setStatefulDefaultResponseBody(dto.getStatefulDefaultResponseBody());
        }

        restfulMockDAO.save(mock);
    }

    void createStatefulChildMocks(final RestfulMockDTO dto, final RestfulMock mainMock, final SmockinUser smockinUser) {

        final String originalPath = dto.getPath();
        final String varPath = dto.getPath() + "/:id";

        for (RestMethodEnum method : RestMethodEnum.values()) {

            if (RestMethodEnum.GET.equals(method)
                    || RestMethodEnum.PUT.equals(method)
                    || RestMethodEnum.PATCH.equals(method)
                    || RestMethodEnum.DELETE.equals(method)) {
                dto.setPath(varPath);
            } else {
                dto.setPath(originalPath);
            }

            final RestfulMock mock = buildRestfulMock(dto, smockinUser);
            mock.setMethod(method);
            mock.setStatefulParent(mainMock);
            mock.setStatefulDefaultResponseBody(null); // only set this in parent

            restfulMockDAO.save(mock);

        }

    }

    RestfulMock loadStatefulParent(final RestfulMock mock) {

        return (mock.getStatefulParent() != null)
                ? mock.getStatefulParent()
                : mock;
    }

    void handleStatefulMockUpdate(final RestfulMockDTO dto, final RestfulMock mock) throws ValidationException {

        final boolean mockTypeChanged = (!mock.getMockType().equals(dto.getMockType()));

        final RestfulMock parent = loadStatefulParent(mock);

        if (mockTypeChanged) {

            parent.getStatefulChildren().clear();
            restfulMockDAO.saveAndFlush(parent);

            handleMockFieldsUpdate(dto, parent);

        } else {

            updateStatefulMockTypeFields(dto, parent);

            parent.getStatefulChildren()
                    .stream()
                    .forEach(c ->
                            updateStatefulMockTypeFields(dto, c));

        }

    }

}
