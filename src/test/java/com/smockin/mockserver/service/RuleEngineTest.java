package com.smockin.mockserver.service;

import com.smockin.admin.persistence.entity.RestfulMockDefinitionRule;
import com.smockin.admin.persistence.entity.RestfulMockDefinitionRuleGroup;
import com.smockin.admin.persistence.entity.RestfulMockDefinitionRuleGroupCondition;
import com.smockin.admin.persistence.enums.RuleComparatorEnum;
import com.smockin.admin.persistence.enums.RuleDataTypeEnum;
import com.smockin.admin.persistence.enums.RuleMatchingTypeEnum;
import com.smockin.mockserver.service.dto.RestfulResponseDTO;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.MediaType;
import spark.Request;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mgallina.
 */
@RunWith(MockitoJUnitRunner.class)
public class RuleEngineTest {

    @Mock
    private RuleResolver ruleResolver;

    @Mock
    private Request req;

    @Mock
    private List<RestfulMockDefinitionRule> rules;

    @Spy
    @InjectMocks
    private RuleEngineImpl ruleEngine = new RuleEngineImpl();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void process_nullRules_Test() {

        // Assertions
        thrown.expect(NullPointerException.class);

        // Setup
        rules = null;

        // Test
        ruleEngine.process(req, rules);

    }

    @Test
    public void process_emptyRules_Test() {

        // Setup
        rules = new ArrayList<RestfulMockDefinitionRule>();

        // Test
        final RestfulResponseDTO result = ruleEngine.process(req, rules);

        // Assertions
        Assert.assertNull(result);

    }

    @Test
    public void process_Test() {

        // Setup
        rules = new ArrayList<RestfulMockDefinitionRule>();

        final RestfulMockDefinitionRule rule = new RestfulMockDefinitionRule(null, 1, 200, MediaType.APPLICATION_JSON_VALUE, "{ \"msg\" : \"foobar\" }", 0, false);
        final RestfulMockDefinitionRuleGroup group = new RestfulMockDefinitionRuleGroup(rule, 1);
        final RestfulMockDefinitionRuleGroupCondition condition = new RestfulMockDefinitionRuleGroupCondition(group, "name", RuleDataTypeEnum.TEXT, RuleComparatorEnum.EQUALS, "joe", RuleMatchingTypeEnum.REQUEST_BODY, false);

        group.getConditions().add(condition);
        rule.getConditionGroups().add(group);
        rules.add(rule);

        Mockito.when(req.body()).thenReturn("{ \"name\" : \"joe\" }");
        Mockito.when(ruleResolver.processRuleComparison(Matchers.any(RestfulMockDefinitionRuleGroupCondition.class), Matchers.anyString())).thenReturn(true);

        // Test
        final RestfulResponseDTO result = ruleEngine.process(req, rules);

        // Assertions
        Assert.assertNotNull(result);
        Assert.assertEquals(rule.getHttpStatusCode(), result.getHttpStatusCode());
        Assert.assertEquals(rule.getResponseContentType(), result.getResponseContentType());
        Assert.assertEquals(rule.getResponseBody(), result.getResponseBody());
    }

    @Test
    public void extractInboundValue_nullRuleMatchingType_Test() {

        // Assertions
        thrown.expect(NullPointerException.class);

        // Test
        ruleEngine.extractInboundValue(null, "", req);

    }

    @Test
    public void extractInboundValue_reqHeader_Test() {

        // Setup
        final String fieldName = "name";
        final String reqResponse = "Hey Joe";
        Mockito.when(req.headers(fieldName)).thenReturn(reqResponse);

        // Test
        final String result = ruleEngine.extractInboundValue(RuleMatchingTypeEnum.REQUEST_HEADER, fieldName, req);

        // Assertions
        Assert.assertNotNull(result);
        Assert.assertEquals(reqResponse, result);

    }

    @Test
    public void extractInboundValue_reqParam_Test() {

        // Setup
        final String fieldName = "name";
        final String reqResponse = "Hey Joe";
        Mockito.when(req.queryParams(fieldName)).thenReturn(reqResponse);

        // Test
        final String result = ruleEngine.extractInboundValue(RuleMatchingTypeEnum.REQUEST_PARAM, "name", req);

        // Assertions
        Assert.assertNotNull(result);
        Assert.assertEquals(reqResponse, result);

    }

    @Test
    public void extractInboundValue_reqBody_Test() {

        // Setup
        final String reqResponse = "Hey Joe";
        Mockito.when(req.body()).thenReturn(reqResponse);

        // Test
        final String result = ruleEngine.extractInboundValue(RuleMatchingTypeEnum.REQUEST_BODY, "", req);

        // Assertions
        Assert.assertNotNull(result);
        Assert.assertEquals(reqResponse, result);

    }

    @Test
    public void extractInboundValue_pathVariable_Test() {

        // Setup
        final String fieldName = "name";
        final String reqResponse = "Hey Joe";
        Mockito.when(req.params(fieldName)).thenReturn(reqResponse);

        // Test
        final String result = ruleEngine.extractInboundValue(RuleMatchingTypeEnum.PATH_VARIABLE, fieldName, req);

        // Assertions
        Assert.assertNotNull(result);
        Assert.assertEquals(reqResponse, result);

    }

}
