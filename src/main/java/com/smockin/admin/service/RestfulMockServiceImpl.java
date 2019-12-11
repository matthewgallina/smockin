package com.smockin.admin.service;

import com.smockin.admin.dto.RestfulMockDTO;
import com.smockin.admin.dto.response.RestfulMockResponseDTO;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.dao.RestfulMockDAO;
import com.smockin.admin.persistence.dao.RestfulMockDefinitionRuleDAO;
import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.admin.persistence.entity.SmockinUser;
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
    public String createEndpoint(final RestfulMockDTO dto, final String token) throws RecordNotFoundException {
        logger.debug("createEndpoint called");

        restfulMockServiceUtils.amendPath(dto);

        final SmockinUser smockinUser = userTokenServiceUtils.loadCurrentUser(token);

        RestfulMock mock = new RestfulMock(
                dto.getPath(),
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

        restfulMockServiceUtils.populateEndpointDefinitionsAndRules(dto, mock);

        // Reassign entity variable, as spring data does not enrich the passed in entity instance with any generated ids.
        mock = restfulMockDAO.save(mock);

        restfulMockServiceUtils.handleEndpointOrdering();

        return mock.getExtId();
    }

    @Override
    public void updateEndpoint(final String mockExtId, final RestfulMockDTO dto, final String token) throws RecordNotFoundException, ValidationException {
        logger.debug("updateEndpoint called");

        restfulMockServiceUtils.amendPath(dto);

        final RestfulMock mock = loadRestMock(mockExtId);

        userTokenServiceUtils.validateRecordOwner(mock.getCreatedBy(), token);

        final boolean pathChanged = (!mock.getPath().equalsIgnoreCase(dto.getPath()));

        mock.getDefinitions().clear();
        mock.getRules().clear();
        restfulMockDAO.saveAndFlush(mock);

        mock.setMockType(dto.getMockType());
        mock.setPath(dto.getPath());
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

        if (dto.getProjectId() != null)
            mock.setProject(projectService.loadByExtId(dto.getProjectId()));

        restfulMockServiceUtils.populateEndpointDefinitionsAndRules(dto, mock);

        restfulMockDAO.save(mock);

        if (RestMockTypeEnum.SEQ.equals(mock.getMockType())) {
            mockOrderingCounterService.clearMockStateById(mock.getExtId());
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

}
