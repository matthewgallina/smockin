package com.smockin.mockserver.service;

import com.smockin.admin.persistence.entity.RestfulMockDefinitionRule;
import com.smockin.admin.persistence.entity.RestfulMockDefinitionRuleGroup;
import com.smockin.admin.persistence.entity.RestfulMockDefinitionRuleGroupCondition;
import com.smockin.admin.persistence.enums.RuleMatchingTypeEnum;
import com.smockin.utils.GeneralUtils;
import com.smockin.mockserver.service.dto.RestfulResponseDTO;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import spark.Request;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

/**
 * Created by gallina.
 */
@Service
public class RuleEngineImpl implements RuleEngine {

    private final Logger logger = LoggerFactory.getLogger(RuleEngineImpl.class);

    @Autowired
    private RuleResolver ruleResolver;

    public RestfulResponseDTO process(final Request req, final List<RestfulMockDefinitionRule> rules) {

        for (RestfulMockDefinitionRule rule : rules) {

            for (RestfulMockDefinitionRuleGroup group : rule.getConditionGroups()) {

                int groupMatchCount = 0;

                for (RestfulMockDefinitionRuleGroupCondition condition : group.getConditions()) {

                    final String inboundValue = extractInboundValue(condition.getRuleMatchingType(), condition.getField(), req);

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

    String extractInboundValue(final RuleMatchingTypeEnum matchingType, final String fieldName, final Request req) {

        switch (matchingType) {
            case REQUEST_HEADER:
                return req.headers(fieldName);
            case REQUEST_PARAM:
                return extractRequestParam(req, fieldName);
            case REQUEST_BODY:
                return req.body();
            case PATH_VARIABLE:
                return req.params(fieldName);
            case PATH_VARIABLE_WILD:

                final int argPosition = NumberUtils.toInt(fieldName, -1);

                if (argPosition == -1
                        || req.splat().length < argPosition) {
                    throw new IllegalArgumentException("Unable to perform wildcard matching on the mocked endpoint '" + req.pathInfo() + "'. Path variable arg count does not align.");
                }

                return req.splat()[(argPosition - 1)];
            case REQUEST_BODY_JSON_ANY:

                final Map<String, ?> json = GeneralUtils.deserialiseJSONToMap(req.body());

                return (json != null)?(String)json.get(fieldName):null;
            default:
                throw new IllegalArgumentException("Unsupported Rule Matching Type : " + matchingType);
        }

    }

    String extractRequestParam(final Request req, final String fieldName) {

        // Java Spark does not provide a convenient way of extracting form based request parameters,
        // so have to parse these manually.
        if (req.contentType() != null
                && (req.contentType().contains("application/x-www-form-urlencoded")
                    ||  req.contentType().contains("multipart/form-data"))) {
            return URLEncodedUtils.parse(req.body(), Charset.defaultCharset())
                    .stream()
                    .filter(k -> k.getName().equals(fieldName))
                    .map(k -> k.getValue())
                    .findFirst()
                    .orElse(null);
        }

        return req.queryParams(fieldName);
    }

}
