package com.smockin.mockserver.service;

import com.smockin.admin.persistence.entity.RestfulMockDefinitionRule;
import com.smockin.admin.persistence.entity.RestfulMockDefinitionRuleGroup;
import com.smockin.admin.persistence.entity.RestfulMockDefinitionRuleGroupCondition;
import com.smockin.admin.persistence.enums.RuleMatchingTypeEnum;
import com.smockin.admin.service.SmockinUserService;
import com.smockin.mockserver.service.dto.RestfulResponseDTO;
import com.smockin.utils.GeneralUtils;
import com.smockin.utils.RuleEngineUtils;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Created by gallina.
 */
@Service
@Transactional
public class RuleEngineImpl implements RuleEngine {

    private final Logger logger = LoggerFactory.getLogger(RuleEngineImpl.class);

    @Autowired
    private RuleResolver ruleResolver;

    @Autowired
    private SmockinUserService smockinUserService;


    public RestfulResponseDTO process(final Context ctx, final List<RestfulMockDefinitionRule> rules) {
        logger.debug("process called");

        // Guard against null context (e.g., from WebSocket rules)
        if (ctx == null) {
            logger.debug("Context is null, skipping rule processing");
            return null;
        }

        for (RestfulMockDefinitionRule rule : rules) {

            for (RestfulMockDefinitionRuleGroup group : rule.getConditionGroups()) {

                int groupMatchCount = 0;

                for (RestfulMockDefinitionRuleGroupCondition condition : group.getConditions()) {

                    final String inboundValue = extractInboundValue(condition.getRuleMatchingType(),
                            condition.getField(),
                            ctx,
                            rule.getRestfulMock().getPath(),
                            rule.getRestfulMock().getCreatedBy().getCtxPath());

                    if (logger.isDebugEnabled()) {
                        logger.debug("Rule Matching Type: " + condition.getRuleMatchingType());
                        logger.debug("Inbound Value: " + inboundValue);
                    }

                    if (ruleResolver.processRuleComparison(condition, inboundValue)) {
                        groupMatchCount++;
                    }

                }

                // If a group of conditions is met then return straight out of this iteration.
                if (groupMatchCount == group.getConditions().size()) {

                    GeneralUtils.checkForAndHandleSleep(rule.getSleepInMillis());

                    return new RestfulResponseDTO(rule.getHttpStatusCode(), rule.getResponseContentType(), rule.getResponseBody(), rule.getResponseHeaders().entrySet());
                }

            }

        }

        return null;
    }

    String extractInboundValue(final RuleMatchingTypeEnum matchingType, final String fieldName, final Context ctx, final String mockPath, final String userCtxPath) {
        final var req = ctx.req();
        switch (matchingType) {
            case REQUEST_HEADER:
                return req.getHeader(fieldName);
            case REQUEST_PARAM:
                return GeneralUtils.extractRequestParamByName(ctx, fieldName);
            case REQUEST_BODY:
                return ctx.body();
            case PATH_VARIABLE:
                final String sanitizedInboundPath = GeneralUtils.sanitizeMultiUserPath(smockinUserService.getUserMode(), req.getPathInfo(), userCtxPath);
                return GeneralUtils.findPathVarIgnoreCase(sanitizedInboundPath, mockPath, fieldName);
            case PATH_VARIABLE_WILD:
                return RuleEngineUtils.matchOnPathVariable(fieldName, ctx);
            case REQUEST_BODY_JSON_ANY:
                return RuleEngineUtils.matchOnJsonField(fieldName, ctx.body(), req.getPathInfo());
            default:
                throw new IllegalArgumentException("Unsupported Rule Matching Type : " + matchingType);
        }
    }

}
