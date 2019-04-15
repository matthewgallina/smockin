package com.smockin.admin.service.utils;

import com.smockin.admin.dto.*;
import com.smockin.admin.dto.response.RestfulMockResponseDTO;
import com.smockin.admin.persistence.dao.RestfulMockDAO;
import com.smockin.admin.persistence.entity.*;
import com.smockin.mockserver.engine.MockedRestServerEngine;
import com.smockin.utils.GeneralUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by mgallina.
 */
@Component
public class RestfulMockServiceUtils {

    @Autowired
    private RestfulMockDAO restfulMockDAO;

    @Autowired
    private RestfulMockSortingUtils restfulMockSortingUtils;

    @Autowired
    private MockedRestServerEngine mockedRestServerEngine;

    @Transactional
    public List<RestfulMockResponseDTO> buildRestfulMockDefinitionDTO(final List<RestfulMock> restfulMockDefinitions) {

        final List<RestfulMockResponseDTO> restMockDTOs = new ArrayList<>();

        for (RestfulMock rmd : restfulMockDefinitions) {

            final RestfulMockResponseDTO dto = new RestfulMockResponseDTO(rmd.getExtId(), rmd.getPath(), rmd.getCreatedBy().getCtxPath(), mockedRestServerEngine.getDeploymentStatus(rmd, rmd.getStatus()), rmd.getMethod(), rmd.getStatus(),
                    rmd.getMockType(), rmd.getDateCreated(), rmd.getCreatedBy().getUsername(), rmd.getProxyTimeOutInMillis(), rmd.getWebSocketTimeoutInMillis(), rmd.getSseHeartBeatInMillis(), rmd.isProxyPushIdOnConnect(),
                    rmd.isRandomiseDefinitions(), rmd.isProxyForwardWhenNoRuleMatch(), rmd.isRandomiseLatency(), rmd.getRandomiseLatencyRangeMinMillis(), rmd.getRandomiseLatencyRangeMaxMillis());

            // Definitions
            for (RestfulMockDefinitionOrder order : rmd.getDefinitions()) {
                final RestfulMockDefinitionDTO restfulMockDefinitionDTO = new RestfulMockDefinitionDTO(order.getExtId(), order.getOrderNo(), order.getHttpStatusCode(), order.getResponseContentType(), order.getResponseBody(), order.getSleepInMillis(), order.isSuspend(), order.getFrequencyCount(), order.getFrequencyPercentage());

                for (Map.Entry<String, String> responseHeader : order.getResponseHeaders().entrySet()) {
                    restfulMockDefinitionDTO.getResponseHeaders().put(responseHeader.getKey(), responseHeader.getValue());
                }

                dto.getDefinitions().add(restfulMockDefinitionDTO);
            }

            // Rules
            for (RestfulMockDefinitionRule rule : rmd.getRules()) {

                final RuleDTO ruleDto = new RuleDTO(rule.getExtId(), rule.getOrderNo(), rule.getHttpStatusCode(), rule.getResponseContentType(), rule.getResponseBody(), rule.getSleepInMillis(), rule.isSuspend());

                for (Map.Entry<String, String> responseHeader : rule.getResponseHeaders().entrySet()) {
                    ruleDto.getResponseHeaders().put(responseHeader.getKey(), responseHeader.getValue());
                }

                for (RestfulMockDefinitionRuleGroup group : rule.getConditionGroups()) {

                    final RuleGroupDTO groupDto = new RuleGroupDTO(group.getExtId(), group.getOrderNo());

                    for (RestfulMockDefinitionRuleGroupCondition condition : group.getConditions()) {
                        groupDto.getConditions().add(new RuleConditionDTO(condition.getExtId(), condition.getField(), condition.getDataType(), condition.getComparator(), condition.getMatchValue(), condition.getRuleMatchingType(), condition.isCaseSensitive()));
                    }

                    ruleDto.getGroups().add(groupDto);
                }

                dto.getRules().add(ruleDto);
            }

            restMockDTOs.add(dto);
        }

        return restMockDTOs;
    }

    public void buildRuleGroups(final RuleDTO dto, final RestfulMockDefinitionRule rule) {

        for (RuleGroupDTO groupDTO : dto.getGroups()) {

            final RestfulMockDefinitionRuleGroup group = new RestfulMockDefinitionRuleGroup(rule, groupDTO.getOrderNo());

            for (RuleConditionDTO conditionDTO : groupDTO.getConditions()) {
                group.getConditions().add(new RestfulMockDefinitionRuleGroupCondition(group, conditionDTO.getField(), conditionDTO.getDataType(), conditionDTO.getComparator(), conditionDTO.getValue(), conditionDTO.getRuleMatchingType(), conditionDTO.isCaseSensitive()));
            }

            rule.getConditionGroups().add(group);
        }

    }

    public void populateEndpointDefinitionsAndRules(final RestfulMockDTO dtoSource, final RestfulMock mockDest) {

        // Endpoint Sequenced Definition
        for (RestfulMockDefinitionDTO restMockOrderDto : dtoSource.getDefinitions()) {

            final RestfulMockDefinitionOrder restfulMockDefinitionOrder =
                    new RestfulMockDefinitionOrder(mockDest, restMockOrderDto.getHttpStatusCode(), restMockOrderDto.getResponseContentType(), restMockOrderDto.getResponseBody(), restMockOrderDto.getOrderNo(), restMockOrderDto.getSleepInMillis(), restMockOrderDto.isSuspend(), restMockOrderDto.getFrequencyCount(), restMockOrderDto.getFrequencyPercentage());

            if (restMockOrderDto.getResponseHeaders() != null) {
                for (Map.Entry<String, String> responseHeader : restMockOrderDto.getResponseHeaders().entrySet()) {
                    restfulMockDefinitionOrder.getResponseHeaders().put(responseHeader.getKey(), responseHeader.getValue());
                }
            }

            mockDest.getDefinitions().add(restfulMockDefinitionOrder);
        }

        // Endpoint Rules
        for (RuleDTO ruleDto : dtoSource.getRules()) {

            final RestfulMockDefinitionRule rule = new RestfulMockDefinitionRule(mockDest, ruleDto.getOrderNo(), ruleDto.getHttpStatusCode(), ruleDto.getResponseContentType(), ruleDto.getResponseBody(), ruleDto.getSleepInMillis(), ruleDto.isSuspend());

            for (Map.Entry<String, String> responseHeader : ruleDto.getResponseHeaders().entrySet()) {
                rule.getResponseHeaders().put(responseHeader.getKey(), responseHeader.getValue());
            }

            buildRuleGroups(ruleDto, rule);

            mockDest.getRules().add(rule);
        }

    }

    @Transactional
    public void handleEndpointOrdering() {

        // Load all restful mocks
        final List<RestfulMock> allRestfulMocks = restfulMockDAO.findAll();

        // Alphanumerically order the mocks by endpoint path. This also updates the initializationOrder field of each record.
        restfulMockSortingUtils.autoOrderEndpointPaths(allRestfulMocks);

        // Save all
        restfulMockDAO.saveAll(allRestfulMocks);
    }

    public void amendPath(final RestfulMockDTO dto) {
        dto.setPath(GeneralUtils.prefixPath(dto.getPath()));
    }

    public void preHandleExistingEndpoints(final RestfulMockDTO dto, final MockImportConfigDTO apiImportConfig, final SmockinUser user, final String conflictCtxPath) {

        final RestfulMock existingRestFulMock = restfulMockDAO.findByPathAndMethodAndUser(dto.getPath(), dto.getMethod(), user);

        if (existingRestFulMock == null) {
            return;
        }

        if (!apiImportConfig.isKeepExisting()) {
            restfulMockDAO.delete(existingRestFulMock);
            restfulMockDAO.flush();
            return;
        }

        switch (apiImportConfig.getKeepStrategy()) {
            case RENAME_EXISTING:
                existingRestFulMock.setPath("/" + conflictCtxPath + existingRestFulMock.getPath());
                restfulMockDAO.save(existingRestFulMock);
                break;
            case RENAME_NEW:
                dto.setPath("/" + conflictCtxPath + dto.getPath());
                break;
        }

    }

}
