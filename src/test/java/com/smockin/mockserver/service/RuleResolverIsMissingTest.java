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
public class RuleResolverIsMissingTest {

    private RuleResolver ruleResolver;

    private final String ruleFieldName = "FirstName";


    @Before
    public void setup() {
        ruleResolver = new RuleResolverImpl();
    }

    @Test
    public void processRuleComparison_Text_IsMissing_Pass() {

        // Setup
        final RestfulMockDefinitionRuleGroupCondition condition =
                new RestfulMockDefinitionRuleGroupCondition(null, ruleFieldName, RuleDataTypeEnum.TEXT, RuleComparatorEnum.IS_MISSING, "", RuleMatchingTypeEnum.REQUEST_PARAM, false);

        // Test
        final boolean result = ruleResolver.processRuleComparison(condition, null);

        // Assertions
        Assert.assertTrue(result);
    }

    @Test
    public void processRuleComparison_Text_IsMissing_Fail() {

        // Setup
        final RestfulMockDefinitionRuleGroupCondition condition =
                new RestfulMockDefinitionRuleGroupCondition(null, ruleFieldName, RuleDataTypeEnum.TEXT, RuleComparatorEnum.IS_MISSING, "Joe", RuleMatchingTypeEnum.REQUEST_PARAM, false);

        // Test
        final boolean result = ruleResolver.processRuleComparison(condition, "Joe");

        // Assertions
        Assert.assertFalse(result);
    }

    @Test
    public void processRuleComparison_CaseSensitiveText_IsMissing_Pass() {

        // Setup
        final RestfulMockDefinitionRuleGroupCondition condition =
                new RestfulMockDefinitionRuleGroupCondition(null, ruleFieldName, RuleDataTypeEnum.TEXT, RuleComparatorEnum.IS_MISSING, "Joe", RuleMatchingTypeEnum.REQUEST_PARAM, true);

        // Test
        final boolean result = ruleResolver.processRuleComparison(condition, "joe");

        // Assertions
        Assert.assertTrue(result);
    }

    @Test
    public void processRuleComparison_Text_IsMissing_DifferentInputValue_Pass() {

        // Setup
        final RestfulMockDefinitionRuleGroupCondition condition =
                new RestfulMockDefinitionRuleGroupCondition(null, ruleFieldName, RuleDataTypeEnum.TEXT, RuleComparatorEnum.IS_MISSING, "Joe", RuleMatchingTypeEnum.REQUEST_PARAM, false);

        // Test
        final boolean result = ruleResolver.processRuleComparison(condition, "jane");

        // Assertions
        Assert.assertTrue(result);
    }

    @Test
    public void processRuleComparison_Text_IsMissing_NullInput_Pass() {

        // Setup
        final RestfulMockDefinitionRuleGroupCondition condition =
                new RestfulMockDefinitionRuleGroupCondition(null, ruleFieldName, RuleDataTypeEnum.TEXT, RuleComparatorEnum.IS_MISSING, "Joe", RuleMatchingTypeEnum.REQUEST_PARAM, false);

        // Test
        final boolean result = ruleResolver.processRuleComparison(condition, null);

        // Assertions
        Assert.assertTrue(result);
    }

    @Test
    public void processRuleComparison_Text_IsMissing_BlankInput_Pass() {

        // Setup
        final RestfulMockDefinitionRuleGroupCondition condition =
                new RestfulMockDefinitionRuleGroupCondition(null, ruleFieldName, RuleDataTypeEnum.TEXT, RuleComparatorEnum.IS_MISSING, "Joe", RuleMatchingTypeEnum.REQUEST_PARAM, false);

        // Test
        final boolean result = ruleResolver.processRuleComparison(condition, "");

        // Assertions
        Assert.assertTrue(result);
    }

}
