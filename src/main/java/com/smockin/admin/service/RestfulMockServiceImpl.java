package com.smockin.admin.service;

import com.smockin.admin.dto.RestfulMockDTO;
import com.smockin.admin.dto.response.RestfulMockResponseDTO;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.persistence.dao.RestfulMockDAO;
import com.smockin.admin.persistence.dao.RestfulMockDefinitionRuleDAO;
import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.admin.service.utils.RestfulMockServiceUtils;
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
    private RestfulMockDAO restfulMockDefinitionDAO;

    @Autowired
    private RestfulMockDefinitionRuleDAO restfulMockDefinitionRuleDAO;

    @Autowired
    private RestfulMockServiceUtils restfulMockServiceUtils;


    public String createEndpoint(final RestfulMockDTO dto) {

        restfulMockServiceUtils.amendPath(dto);

        RestfulMock mock = new RestfulMock(dto.getPath(), dto.getMethod(), dto.getStatus(), dto.getMockType(), dto.getProxyTimeoutInMillis(), dto.isRandomiseDefinitions());

        restfulMockServiceUtils.populateEndpointDefinitionsAndRules(dto, mock);

        // Reassign entity variable, as spring data does not enrich the passed in entity instance with any generated ids.
        mock = restfulMockDefinitionDAO.save(mock);

        restfulMockServiceUtils.handleEndpointOrdering();

        return mock.getExtId();
    }

    public void updateEndpoint(final String mockDefExtId, final RestfulMockDTO dto) throws RecordNotFoundException {

        restfulMockServiceUtils.amendPath(dto);

        final RestfulMock mock = restfulMockDefinitionDAO.findByExtId(mockDefExtId);

        if (mock == null)
            throw new RecordNotFoundException();

        final boolean pathChanged = (!mock.getPath().equalsIgnoreCase(dto.getPath()));

        mock.getDefinitions().clear();
        mock.getRules().clear();
        restfulMockDefinitionDAO.saveAndFlush(mock);

        mock.setMockType(dto.getMockType());
        mock.setPath(dto.getPath());
        mock.setMethod(dto.getMethod());
        mock.setStatus(dto.getStatus());
        mock.setProxyTimeOutInMillis(dto.getProxyTimeoutInMillis());
        mock.setRandomiseDefinitions(dto.isRandomiseDefinitions());

        restfulMockServiceUtils.populateEndpointDefinitionsAndRules(dto, mock);

        restfulMockDefinitionDAO.save(mock).getId();

        if (pathChanged) {
            restfulMockServiceUtils.handleEndpointOrdering();
        }

    }

    public void deleteEndpoint(final String mockDefExtId) throws RecordNotFoundException {

        final RestfulMock mock = restfulMockDefinitionDAO.findByExtId(mockDefExtId);

        if (mock == null)
            throw new RecordNotFoundException();

        restfulMockDefinitionDAO.delete(mock);
    }

    public List<RestfulMockResponseDTO> loadAll() {
        return restfulMockServiceUtils.buildRestfulMockDefinitionDTO(restfulMockDefinitionDAO.findAll());
    }

    /*
    @Override
    public List<RestfulMockResponseDTO> loadAllByStatus(final RecordStatusEnum status) {
        return restfulMockServiceUtils.buildRestfulMockDefinitionDTO(restfulMockDefinitionDAO.findAllByStatus(status));

    }

    @Override
    public String createRule(final String mockDefExtId, final RuleDTO dto) throws RecordNotFoundException {

        final RestfulMock mock = restfulMockDefinitionDAO.findByExtId(mockDefExtId);

        if (mock == null)
            throw new RecordNotFoundException();

        final RestfulMockDefinitionRule rule = new RestfulMockDefinitionRule(mock, dto.getOrderNo(), dto.getHttpStatusCode(), dto.getResponseContentType(), dto.getResponseBody());

        restfulMockServiceUtils.buildRuleGroups(dto, rule);

        return restfulMockDefinitionRuleDAO.save(rule).getExtId();
    }

    @Override
    public void updateRule(final String ruleExtId, final RuleDTO dto) throws RecordNotFoundException {

        final RestfulMockDefinitionRule rule = restfulMockDefinitionRuleDAO.findByExtId(ruleExtId);

        if (rule == null)
            throw new RecordNotFoundException();

        rule.getConditionGroups().clear();
        restfulMockDefinitionRuleDAO.saveAndFlush(rule);

        rule.setHttpStatusCode(dto.getHttpStatusCode());
        rule.setResponseBody(dto.getResponseBody());

        restfulMockServiceUtils.buildRuleGroups(dto, rule);

        restfulMockDefinitionRuleDAO.save(rule);
    }
    */
}
