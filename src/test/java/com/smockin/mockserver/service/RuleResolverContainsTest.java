package com.smockin.mockserver.service;

import com.smockin.admin.persistence.entity.RestfulMockDefinitionRuleGroupCondition;
import com.smockin.admin.persistence.enums.RuleComparatorEnum;
import com.smockin.admin.persistence.enums.RuleDataTypeEnum;
import com.smockin.admin.persistence.enums.RuleMatchingTypeEnum;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by mgallina.
 */
public class RuleResolverContainsTest {

    private RuleResolver ruleResolver;

    private final String ruleTextValue = "HiJkLmNoP";
    private final String inboundTextValue = "aBcDeFg" + ruleTextValue + "qRsTuVwXyZ";


    @Before
    public void setup() {
        ruleResolver = new RuleResolverImpl();
    }

    @Test
    public void processRuleComparison_NullValue_Text_Contains_Fail() {

        // Setup
        final RestfulMockDefinitionRuleGroupCondition condition =
                new RestfulMockDefinitionRuleGroupCondition(null, "NAME", RuleDataTypeEnum.TEXT, RuleComparatorEnum.CONTAINS, ruleTextValue, RuleMatchingTypeEnum.REQUEST_PARAM, false);

        // Test
        final boolean result = ruleResolver.processRuleComparison(condition, null);

        // Assertions
        Assert.assertFalse(result);
    }

    @Test
    public void processRuleComparison_Text_Contains_Pass() {

        // Setup
        final RestfulMockDefinitionRuleGroupCondition condition =
                new RestfulMockDefinitionRuleGroupCondition(null, "NAME", RuleDataTypeEnum.TEXT, RuleComparatorEnum.CONTAINS, ruleTextValue.toUpperCase(), RuleMatchingTypeEnum.REQUEST_PARAM, false);

        // Test
        final boolean result = ruleResolver.processRuleComparison(condition, inboundTextValue);

        // Assertions
        Assert.assertTrue(result);
    }

    @Test
    public void processRuleComparison_CaseSensitiveText_Contains_Pass() {

        // Setup
        final RestfulMockDefinitionRuleGroupCondition condition =
                new RestfulMockDefinitionRuleGroupCondition(null, "NAME", RuleDataTypeEnum.TEXT, RuleComparatorEnum.CONTAINS, ruleTextValue, RuleMatchingTypeEnum.REQUEST_PARAM, true);

        // Test
        final boolean result = ruleResolver.processRuleComparison(condition, inboundTextValue);

        // Assertions
        Assert.assertTrue(result);
    }

    @Test
    public void processRuleComparison_CaseSensitiveText_Contains_Fail() {

        // Setup
        final RestfulMockDefinitionRuleGroupCondition condition =
                new RestfulMockDefinitionRuleGroupCondition(null, "NAME", RuleDataTypeEnum.TEXT, RuleComparatorEnum.CONTAINS, ruleTextValue.toUpperCase(), RuleMatchingTypeEnum.REQUEST_PARAM, true);

        // Test
        final boolean result = ruleResolver.processRuleComparison(condition, inboundTextValue);

        // Assertions
        Assert.assertFalse(result);
    }

    @Test
    public void processRuleComparison_CaseSensitiveFieldIsNullText_Contains_Fail() {

        // Setup
        final RestfulMockDefinitionRuleGroupCondition condition =
                new RestfulMockDefinitionRuleGroupCondition(null, "NAME", RuleDataTypeEnum.TEXT, RuleComparatorEnum.CONTAINS, ruleTextValue.toUpperCase(), RuleMatchingTypeEnum.REQUEST_PARAM, null);

        // Test
        final boolean result = ruleResolver.processRuleComparison(condition, inboundTextValue);

        // Assertions
        Assert.assertTrue(result);
    }

}
