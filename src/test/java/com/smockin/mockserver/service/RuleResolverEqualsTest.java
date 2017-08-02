package com.smockin.mockserver.service;

import com.smockin.admin.persistence.entity.RestfulMockDefinitionRuleGroupCondition;
import com.smockin.admin.persistence.enums.RuleComparatorEnum;
import com.smockin.admin.persistence.enums.RuleDataTypeEnum;
import com.smockin.admin.persistence.enums.RuleMatchingTypeEnum;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Created by mgallina.
 */
public class RuleResolverEqualsTest {

    private RuleResolver ruleResolver;

    private final String inboundTextValue = "aBcDeF";
    private final String inboundNumericWholeValue = "201";
    private final String inboundNumericDecimalValue = "201.321";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setup() {
        ruleResolver = new RuleResolverImpl();
    }

    @Test
    public void processRuleComparison_NullComp_Fail() {

        // Assertions
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Invalid rule comparator. Cannot be null");

        // Setup
        final RestfulMockDefinitionRuleGroupCondition condition =
                new RestfulMockDefinitionRuleGroupCondition(null, "NAME", RuleDataTypeEnum.TEXT, null, inboundTextValue, RuleMatchingTypeEnum.REQUEST_PARAM, false);

        // Test
        ruleResolver.processRuleComparison(condition, null);
    }

    @Test
    public void processRuleComparison_NullValue_Text_Equals_Fail() {

        // Setup
        final RestfulMockDefinitionRuleGroupCondition condition =
                new RestfulMockDefinitionRuleGroupCondition(null, "NAME", RuleDataTypeEnum.TEXT, RuleComparatorEnum.EQUALS, inboundTextValue, RuleMatchingTypeEnum.REQUEST_PARAM, false);

        // Test
        final boolean result = ruleResolver.processRuleComparison(condition, null);

        // Assertions
        Assert.assertFalse(result);
    }

    @Test
    public void processRuleComparison_Text_Equals_Pass() {

        // Setup
        final RestfulMockDefinitionRuleGroupCondition condition =
                new RestfulMockDefinitionRuleGroupCondition(null, "NAME", RuleDataTypeEnum.TEXT, RuleComparatorEnum.EQUALS, inboundTextValue, RuleMatchingTypeEnum.REQUEST_PARAM, false);

        // Test
        final boolean result = ruleResolver.processRuleComparison(condition, inboundTextValue.toUpperCase());

        // Assertions
        Assert.assertTrue(result);
    }

    @Test
    public void processRuleComparison_Text_Equals_Fail() {

        // Setup
        final RestfulMockDefinitionRuleGroupCondition condition =
                new RestfulMockDefinitionRuleGroupCondition(null, "NAME", RuleDataTypeEnum.TEXT, RuleComparatorEnum.EQUALS, inboundTextValue, RuleMatchingTypeEnum.REQUEST_PARAM, false);

        // Test
        final boolean result = ruleResolver.processRuleComparison(condition, inboundTextValue + "GHI");

        // Assertions
        Assert.assertFalse(result);
    }

    @Test
    public void processRuleComparison_CaseSensitiveFieldIsNull_Text_Equals_Pass() {

        // Setup
        final RestfulMockDefinitionRuleGroupCondition condition =
                new RestfulMockDefinitionRuleGroupCondition(null, "NAME", RuleDataTypeEnum.TEXT, RuleComparatorEnum.EQUALS, inboundTextValue, RuleMatchingTypeEnum.REQUEST_PARAM, null);

        // Test
        final boolean result = ruleResolver.processRuleComparison(condition, inboundTextValue.toUpperCase());

        // Assertions
        Assert.assertTrue(result);
    }

    @Test
    public void processRuleComparison_CaseSensitive_Text_Equals_Pass() {

        // Setup
        final RestfulMockDefinitionRuleGroupCondition condition =
                new RestfulMockDefinitionRuleGroupCondition(null, "NAME", RuleDataTypeEnum.TEXT, RuleComparatorEnum.EQUALS, inboundTextValue, RuleMatchingTypeEnum.REQUEST_PARAM, true);

        // Test
        final boolean result = ruleResolver.processRuleComparison(condition, inboundTextValue);

        // Assertions
        Assert.assertTrue(result);
    }

    @Test
    public void processRuleComparison_CaseSensitive_Text_Equals_Fail() {

        // Setup
        final RestfulMockDefinitionRuleGroupCondition condition =
                new RestfulMockDefinitionRuleGroupCondition(null, "NAME", RuleDataTypeEnum.TEXT, RuleComparatorEnum.EQUALS, inboundTextValue, RuleMatchingTypeEnum.REQUEST_PARAM, true);

        // Test
        final boolean result = ruleResolver.processRuleComparison(condition, inboundTextValue.toUpperCase());

        // Assertions
        Assert.assertFalse(result);
    }

    @Test
    public void processRuleComparison_Whole_Numeric_Equals_Pass() {

        // Setup
        final RestfulMockDefinitionRuleGroupCondition condition =
                new RestfulMockDefinitionRuleGroupCondition(null, "NAME", RuleDataTypeEnum.NUMERIC, RuleComparatorEnum.EQUALS, inboundNumericWholeValue, RuleMatchingTypeEnum.REQUEST_PARAM, null);

        // Test
        final boolean result = ruleResolver.processRuleComparison(condition, inboundNumericWholeValue);

        // Assertions
        Assert.assertTrue(result);
    }

    @Test
    public void processRuleComparison_Whole_Numeric_Equals_Fail() {

        // Setup
        final RestfulMockDefinitionRuleGroupCondition condition =
                new RestfulMockDefinitionRuleGroupCondition(null, "NAME", RuleDataTypeEnum.NUMERIC, RuleComparatorEnum.EQUALS, inboundNumericWholeValue, RuleMatchingTypeEnum.REQUEST_PARAM, null);

        // Test
        final boolean result = ruleResolver.processRuleComparison(condition, "101");

        // Assertions
        Assert.assertFalse(result);
    }

    @Test
    public void processRuleComparison_Decimal_Numeric_Equals_Pass() {

        // Setup
        final RestfulMockDefinitionRuleGroupCondition condition =
                new RestfulMockDefinitionRuleGroupCondition(null, "NAME", RuleDataTypeEnum.NUMERIC, RuleComparatorEnum.EQUALS, inboundNumericDecimalValue, RuleMatchingTypeEnum.REQUEST_PARAM, null);

        // Test
        final boolean result = ruleResolver.processRuleComparison(condition, inboundNumericDecimalValue);

        // Assertions
        Assert.assertTrue(result);
    }

    @Test
    public void processRuleComparison_Decimal_Numeric_Equals_Fail() {

        // Setup
        final RestfulMockDefinitionRuleGroupCondition condition =
                new RestfulMockDefinitionRuleGroupCondition(null, "NAME", RuleDataTypeEnum.NUMERIC, RuleComparatorEnum.EQUALS, inboundNumericDecimalValue, RuleMatchingTypeEnum.REQUEST_PARAM, null);

        // Test
        final boolean result = ruleResolver.processRuleComparison(condition, "201.322");

        // Assertions
        Assert.assertFalse(result);
    }

    @Test
    public void processRuleComparison_Invalid_Numeric_Equals_Fail() {

        // Setup
        final RestfulMockDefinitionRuleGroupCondition condition =
                new RestfulMockDefinitionRuleGroupCondition(null, "NAME", RuleDataTypeEnum.NUMERIC, RuleComparatorEnum.EQUALS, inboundNumericDecimalValue, RuleMatchingTypeEnum.REQUEST_PARAM, null);

        // Test
        final boolean result = ruleResolver.processRuleComparison(condition, "One");

        // Assertions
        Assert.assertFalse(result);
    }

}
