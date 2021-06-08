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


    @Override
    public RestfulMockResponseDTO loadEndpoint(final String mockExtId, final String token) throws RecordNotFoundException, ValidationException {
        logger.debug("loadEndpoint called");

        final RestfulMock mock = loadRestMock(mockExtId);

        userTokenServiceUtils.validateRecordOwner(mock.getCreatedBy(), token);

        return restfulMockServiceUtils.buildRestfulMockDefinitionDTO(mock);
    }

    @Override
    public String createEndpoint(final RestfulMockDTO dto,
                                 final String token) throws RecordNotFoundException, ValidationException {
        logger.debug("createEndpoint called");

        restfulMockServiceUtils.amendPath(dto);

        restfulMockServiceUtils.validateMockPathDoesNotStartWithUsername(dto.getPath());

        final SmockinUser smockinUser = userTokenServiceUtils.loadCurrentActiveUser(token);

        RestfulMock mainMock = restfulMockServiceUtils.buildRestfulMock(dto, smockinUser);

        mainMock = restfulMockServiceUtils.handleCreateStatefulMockType(dto, mainMock, smockinUser);
        restfulMockServiceUtils.handleCustomJsSyntax(dto, mainMock);
        restfulMockServiceUtils.populateEndpointDefinitionsAndRules(dto, mainMock);

        mainMock = restfulMockDAO.save(mainMock);

        restfulMockServiceUtils.handleEndpointOrdering();

        return mainMock.getExtId();
    }

    @Override
    public void updateEndpoint(final String mockExtId, final RestfulMockDTO dto, final String token) throws RecordNotFoundException, ValidationException {
        logger.debug("updateEndpoint called");

        restfulMockServiceUtils.amendPath(dto);

        restfulMockServiceUtils.validateMockPathDoesNotStartWithUsername(dto.getPath());

        final RestfulMock mock = loadRestMock(mockExtId);

        userTokenServiceUtils.validateRecordOwner(mock.getCreatedBy(), token);

        final boolean pathChanged = (!mock.getPath().equalsIgnoreCase(dto.getPath()));

        if (RestMockTypeEnum.STATEFUL.equals(mock.getMockType())) { // existing mock is STATEFUL...

            restfulMockServiceUtils.handleExistingStatefulMockUpdate(dto, mock);

        } else if (RestMockTypeEnum.STATEFUL.equals(dto.getMockType())) { // existing mock IS NOT STATEFUL but being updated to a STATEFUL type...

            dto.setMethod(RestMethodEnum.GET);
            restfulMockServiceUtils.handleMockFieldsUpdate(dto, mock);

            restfulMockServiceUtils.createStatefulChildMocks(dto, mock, mock.getCreatedBy());

        } else {

            restfulMockServiceUtils.handleMockFieldsUpdate(dto, mock);

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

        restfulMockServiceUtils.handleDeleteStatefulMock(mock);

        restfulMockDAO.delete(mock);
    }

    @Override
    public List<RestfulMockResponseDTO> loadAll(final String token) throws RecordNotFoundException {
        logger.debug("loadAll called");

        return restfulMockServiceUtils.buildRestfulMockDefinitionDTOs(restfulMockDAO.findAllByUser(userTokenServiceUtils.loadCurrentActiveUser(token).getId()));
    }

    RestfulMock loadRestMock(final String mockExtId) throws RecordNotFoundException {
        logger.debug("loadRestMock called");

        final RestfulMock mock = restfulMockDAO.findByExtId(mockExtId);

        if (mock == null)
            throw new RecordNotFoundException();

        return mock;
    }

}
