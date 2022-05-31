package com.smockin.mockserver.service;

import com.smockin.admin.enums.UserModeEnum;
import com.smockin.admin.persistence.entity.*;
import com.smockin.admin.persistence.enums.RuleComparatorEnum;
import com.smockin.admin.persistence.enums.RuleDataTypeEnum;
import com.smockin.admin.persistence.enums.RuleMatchingTypeEnum;
import com.smockin.admin.service.SmockinUserService;
import com.smockin.mockserver.service.dto.RestfulResponseDTO;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import spark.QueryParamsMap;
import spark.Request;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private QueryParamsMap queryParamsMap;

    @Mock
    private List<RestfulMockDefinitionRule> rules;

    @Mock
    private SmockinUserService smockinUserService;

    @Spy
    @InjectMocks
    private RuleEngineImpl ruleEngine = new RuleEngineImpl();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private String userCtxPath;


    @Before
    public void setUp() {

        userCtxPath = "";
        Mockito.when(smockinUserService.getUserMode()).thenReturn(UserModeEnum.INACTIVE);

    }

    @Test
    public void process_nullRules_Test() {

        // Assertions
        thrown.expect(NullPointerException.class);

        // Setup
        rules = null;

        // Test
        ruleEngine.process(req.pathInfo(), req, rules);

    }

    @Test
    public void process_emptyRules_Test() {

        // Setup
        rules = new ArrayList<RestfulMockDefinitionRule>();

        // Test
        final RestfulResponseDTO result = ruleEngine.process(req.pathInfo(), req, rules);

        // Assertions
        Assert.assertNull(result);

    }

    @Test
    public void process_Test() {

        // Setup
        rules = new ArrayList<>();

        final RestfulMock mock = new RestfulMock();
        mock.setPath("/person/{name}");
        final SmockinUser user = new SmockinUser();
        user.setCtxPath(userCtxPath);
        mock.setCreatedBy(user);
        final RestfulMockDefinitionRule rule = new RestfulMockDefinitionRule(mock, 1, 200, MediaType.APPLICATION_JSON_VALUE, "{ \"msg\" : \"foobar\" }", 0, false);
        final RestfulMockDefinitionRuleGroup group = new RestfulMockDefinitionRuleGroup(rule, 1);
        final RestfulMockDefinitionRuleGroupCondition condition = new RestfulMockDefinitionRuleGroupCondition(group, "name", RuleDataTypeEnum.TEXT, RuleComparatorEnum.EQUALS, "joe", RuleMatchingTypeEnum.REQUEST_BODY, false);

        group.getConditions().add(condition);
        rule.getConditionGroups().add(group);
        rules.add(rule);

        Mockito.when(req.body()).thenReturn("{ \"name\" : \"joe\" }");
        Mockito.when(ruleResolver.processRuleComparison(Mockito.any(RestfulMockDefinitionRuleGroupCondition.class), Mockito.anyString())).thenReturn(true);

        // Test
        final RestfulResponseDTO result = ruleEngine.process(req.pathInfo(), req, rules);

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
        ruleEngine.extractInboundValue(null, "", req.pathInfo(), req, "/person/{name}", userCtxPath);

    }

    @Test
    public void extractInboundValue_reqHeader_Test() {

        // Setup
        final String fieldName = "name";
        final String reqResponse = "Hey Joe";
        Mockito.when(req.headers(fieldName)).thenReturn(reqResponse);

        // Test
        final String result = ruleEngine.extractInboundValue(RuleMatchingTypeEnum.REQUEST_HEADER, fieldName, req.pathInfo(), req, "/person/{name}", userCtxPath);

        // Assertions
        Assert.assertNotNull(result);
        Assert.assertEquals(reqResponse, result);

    }

    @Test
    public void extractInboundValue_reqParam_Test() {

        // Setup
        final String fieldName = "name";
        final String reqResponse = "Hey Joe";
        final Map<String, String[]> params = new HashMap<>();
        params.put(fieldName, new String[] { reqResponse });
        Mockito.when(req.requestMethod()).thenReturn(HttpMethod.POST.name());
        Mockito.when(queryParamsMap.toMap()).thenReturn(params);
        Mockito.when(req.queryMap()).thenReturn(queryParamsMap);

        // Test
        final String result = ruleEngine.extractInboundValue(RuleMatchingTypeEnum.REQUEST_PARAM, "name", req.pathInfo(), req, "/person/{name}", userCtxPath);

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
        final String result = ruleEngine.extractInboundValue(RuleMatchingTypeEnum.REQUEST_BODY, "", req.pathInfo(), req, "/person/{name}", userCtxPath);

        // Assertions
        Assert.assertNotNull(result);
        Assert.assertEquals(reqResponse, result);

    }

    @Test
    public void extractInboundValue_pathVariable_Test() {

        // Setup
        final String fieldName = "name";
        Mockito.when(req.pathInfo()).thenReturn("/person/Joe");

        // Test
        final String result = ruleEngine.extractInboundValue(RuleMatchingTypeEnum.PATH_VARIABLE, fieldName, req.pathInfo(), req, "/person/{name}", userCtxPath);

        // Assertions
        Assert.assertNotNull(result);
        Assert.assertEquals("Joe", result);

    }

    @Test
    public void extractInboundValue_jsonReqBody_Test() {

        // Setup
        final String fieldName = "username";
        final String fieldValue = "admin";
        Mockito.when(req.body()).thenReturn("{\"username\":\"" + fieldValue + "\"}");

        // Test
        final String result = ruleEngine.extractInboundValue(RuleMatchingTypeEnum.REQUEST_BODY_JSON_ANY, fieldName, req.pathInfo(), req, "/person/{name}", userCtxPath);

        // Assertions
        Assert.assertNotNull(result);
        Assert.assertEquals(fieldValue, result);

    }

    @Test
    public void extractInboundValue_jsonReqBody_NotFound_Test() {

        // Setup
        final String fieldName = "username";
        Mockito.when(req.body()).thenReturn("{\"foo\":\"bar\"}");

        // Test
        final String result = ruleEngine.extractInboundValue(RuleMatchingTypeEnum.REQUEST_BODY_JSON_ANY, fieldName, req.pathInfo(), req, "/person/{name}", userCtxPath);

        // Assertions
        Assert.assertNull(result);

    }

    @Test
    public void extractInboundValue_jsonReqBody_invalidJson_Test() {

        // Setup
        final String fieldName = "username";
        Mockito.when(req.body()).thenReturn("username = admin");

        // Test
        final String result = ruleEngine.extractInboundValue(RuleMatchingTypeEnum.REQUEST_BODY_JSON_ANY, fieldName, req.pathInfo(), req, "/person/{name}", userCtxPath);

        // Assertions
        Assert.assertNull(result);

    }

    @Test
    public void extractInboundValue_jsonReqBody_null_Test() {

        // Setup
        final String fieldName = "username";
        Mockito.when(req.body()).thenReturn(null);

        // Test
        final String result = ruleEngine.extractInboundValue(RuleMatchingTypeEnum.REQUEST_BODY_JSON_ANY, fieldName, req.pathInfo(), req, "/person/{name}", userCtxPath);

        // Assertions
        Assert.assertNull(result);

    }

}
