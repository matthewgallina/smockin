package com.smockin.mockserver.service;

import com.smockin.admin.persistence.entity.RestfulMockDefinitionRuleGroupCondition;
import com.smockin.admin.persistence.enums.RuleDataTypeEnum;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.stereotype.Service;

/**
 * Created by mgallina.
 */
@Service
public class RuleResolverImpl implements RuleResolver {

    @Override
    public boolean processRuleComparison(final RestfulMockDefinitionRuleGroupCondition condition, final String inboundValue) {

        if (condition.getComparator() == null) {
            throw new IllegalArgumentException("Invalid rule comparator. Cannot be null");
        }

        switch (condition.getComparator()) {

            case EQUALS:
                return handleEquals(condition, inboundValue);
            case CONTAINS:
                return handleContains(condition, inboundValue);
            case IS_MISSING:
                return handleIsMissing(condition, inboundValue);
            default:
                throw new IllegalArgumentException("Unsupported rule comparator: " + condition.getComparator());
        }

    }

    // Supports TEXT and NUMERIC data types
    boolean handleEquals(RestfulMockDefinitionRuleGroupCondition condition, final String inboundValue) {

        if (inboundValue == null) {
            return false;
        }

        final RuleDataTypeEnum ruleMatchDataType = condition.getDataType();
        final String ruleMatchValue = condition.getMatchValue();

        if (RuleDataTypeEnum.TEXT.equals(ruleMatchDataType)) {

            if (condition.isCaseSensitive() != null && condition.isCaseSensitive()) {
                return ruleMatchValue.equals(inboundValue);
            }

            return ruleMatchValue.equalsIgnoreCase(inboundValue);
        } else if (RuleDataTypeEnum.NUMERIC.equals(ruleMatchDataType)
                && NumberUtils.isCreatable(inboundValue)
                && NumberUtils.toDouble(inboundValue) == NumberUtils.toDouble(ruleMatchValue)) {
            return true;
        }

        return false;
    }

    // Always handled as TEXT!
    boolean handleIsMissing(RestfulMockDefinitionRuleGroupCondition condition, final String inboundValue) {

        if (StringUtils.isNotBlank(inboundValue)) {

            if (condition.isCaseSensitive() != null
                    && condition.isCaseSensitive()
                    && StringUtils.isNotBlank(condition.getMatchValue())
                    && !condition.getMatchValue().equals(inboundValue)) {
                return true;
            }

            if ( ( condition.isCaseSensitive() == null || !condition.isCaseSensitive() )
                    && ( StringUtils.isNotBlank(condition.getMatchValue()) && !condition.getMatchValue().equalsIgnoreCase(inboundValue) ) ) {
                return true;
            }

            return false;
        }

        return true;
    }

    // Always handled as TEXT!
    boolean handleContains(RestfulMockDefinitionRuleGroupCondition condition, final String inboundValue) {

        if (inboundValue == null) {
            return false;
        }

        if (condition.isCaseSensitive() != null && condition.isCaseSensitive()) {
            return (inboundValue.indexOf(condition.getMatchValue()) > -1);
        }

        return (inboundValue.toLowerCase()
                .indexOf(condition.getMatchValue().toLowerCase()) > -1);
    }

}
