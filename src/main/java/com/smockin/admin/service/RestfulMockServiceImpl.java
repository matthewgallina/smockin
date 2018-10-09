package com.smockin.admin.service;

import com.smockin.admin.dto.ProxyRestDuplicatePriorityDTO;
import com.smockin.admin.dto.RestfulMockDTO;
import com.smockin.admin.dto.response.ProxyRestDuplicateDTO;
import com.smockin.admin.dto.response.RestfulMockResponseDTO;
import com.smockin.admin.enums.SearchFilterEnum;
import com.smockin.admin.exception.AuthException;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.dao.RestfulMockDAO;
import com.smockin.admin.persistence.dao.RestfulMockDefinitionRuleDAO;
import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.service.utils.RestfulMockServiceUtils;
import com.smockin.admin.service.utils.UserTokenServiceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

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

    @Override
    public String createEndpoint(final RestfulMockDTO dto, final String token) throws RecordNotFoundException {
        logger.debug("createEndpoint called");

        restfulMockServiceUtils.amendPath(dto);

        final SmockinUser smockinUser = userTokenServiceUtils.loadCurrentUser(token);

        RestfulMock mock = new RestfulMock(dto.getPath(),
                dto.getMethod(),
                dto.getStatus(),
                dto.getMockType(),
                dto.getProxyTimeoutInMillis(),
                dto.getWebSocketTimeoutInMillis(),
                dto.getSseHeartBeatInMillis(),
                dto.isProxyPushIdOnConnect(),
                dto.isRandomiseDefinitions(),
                dto.isProxyForwardWhenNoRuleMatch(),
                smockinUser);

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

        restfulMockServiceUtils.populateEndpointDefinitionsAndRules(dto, mock);

        restfulMockDAO.save(mock).getId();

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
    public List<RestfulMockResponseDTO> loadAll(final String searchFilter, final String token) throws RecordNotFoundException {
        logger.debug("loadAll called");

        if (SearchFilterEnum.ALL.name().equalsIgnoreCase(searchFilter)) {
            return restfulMockServiceUtils.buildRestfulMockDefinitionDTO(restfulMockDAO.findAll());
        }

        return restfulMockServiceUtils.buildRestfulMockDefinitionDTO(restfulMockDAO.findAllByUser(userTokenServiceUtils.loadCurrentUser(token).getId()));
    }

    @Override
    public List<ProxyRestDuplicateDTO> loadAllUserPathDuplicates(final String token) throws RecordNotFoundException, AuthException {
        logger.debug("loadAllUserPathDuplicates called");

        smockinUserService.assertCurrentUserIsAdmin(userTokenServiceUtils.loadCurrentUser(token));

        return restfulMockDAO.findAllActivePathDuplicates()
                .entrySet()
                .stream()
                .map(m -> new ProxyRestDuplicateDTO(m.getKey().getLeft(), m.getKey().getRight(), restfulMockServiceUtils.buildRestfulMockDefinitionDTO(m.getValue())))
                .collect(Collectors.toList());
    }

    @Override
    public void saveUserPathDuplicatePriorities(final ProxyRestDuplicatePriorityDTO priorityMocks,
                                                final String token) throws RecordNotFoundException, AuthException {
        logger.debug("saveUserPathDuplicatePriorities called");

        smockinUserService.assertCurrentUserIsAdmin(userTokenServiceUtils.loadCurrentUser(token));

        for (String extId : priorityMocks.getProxyPriorityMockIds()) {

            // Set priority
            final RestfulMock restMock = loadRestMock(extId);
            restMock.setProxyPriority(true);

            // Revert any existing priority flags for same path and method.
            // Note if inbound caller is silly enough to pass in 2 priorities for the same path, then this ensure only 1 is set in the end.
            restfulMockDAO.resetAllOtherProxyPriorities(restMock.getPath(), restMock.getMethod(), restMock.getExtId());

            restfulMockDAO.save(restMock);
        }

    }

    RestfulMock loadRestMock(final String mockExtId) throws RecordNotFoundException {
        logger.debug("loadRestMock called");

        final RestfulMock mock = restfulMockDAO.findByExtId(mockExtId);

        if (mock == null)
            throw new RecordNotFoundException();

        return mock;
    }

}
