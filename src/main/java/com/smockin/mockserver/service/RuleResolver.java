package com.smockin.mockserver.service;

import com.smockin.admin.persistence.entity.RestfulMockDefinitionRuleGroupCondition;

/**
 * Created by mgallina.
 */
public interface RuleResolver {

    boolean processRuleComparison(final RestfulMockDefinitionRuleGroupCondition condition, final String inboundValue);

}
