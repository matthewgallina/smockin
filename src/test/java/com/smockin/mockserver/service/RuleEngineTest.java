package com.smockin.mockserver.service;

import com.smockin.admin.enums.UserModeEnum;
import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.admin.persistence.entity.RestfulMockDefinitionRule;
import com.smockin.admin.persistence.entity.RestfulMockDefinitionRuleGroup;
import com.smockin.admin.persistence.entity.RestfulMockDefinitionRuleGroupCondition;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.persistence.enums.RuleComparatorEnum;
import com.smockin.admin.persistence.enums.RuleDataTypeEnum;
import com.smockin.admin.persistence.enums.RuleMatchingTypeEnum;
import com.smockin.admin.service.SmockinUserService;
import com.smockin.mockserver.service.dto.RestfulResponseDTO;
import io.javalin.http.Context;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by mgallina.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class RuleEngineTest {

    @Mock
    private RuleResolver ruleResolver;

    @Mock
    private Context ctx;

    @Mock
    private HttpServletRequest request;

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
        Mockito.when(ctx.req()).thenReturn(request);
        Mockito.when(smockinUserService.getUserMode()).thenReturn(UserModeEnum.INACTIVE);
    }

    @Test
    public void process_nullRules_Test()  {
        thrown.expect(NullPointerException.class);

        rules = null;

        ruleEngine.process(ctx, rules);
    }

    @Test
    public void process_emptyRules_Test()  {
        rules = new ArrayList<RestfulMockDefinitionRule>();

        final RestfulResponseDTO result = ruleEngine.process(ctx, rules);

        Assert.assertNull(result);
    }

    @Test
    public void process_Test()  {
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

        final String body = "{ \"name\" : \"joe\" }";
        Mockito.when(ctx.body()).thenReturn(body);
        Mockito.when(ruleResolver.processRuleComparison(Mockito.any(RestfulMockDefinitionRuleGroupCondition.class), Mockito.anyString())).thenReturn(true);

        final RestfulResponseDTO result = ruleEngine.process(ctx, rules);

        Assert.assertNotNull(result);
        Assert.assertEquals(rule.getHttpStatusCode(), result.getHttpStatusCode());
        Assert.assertEquals(rule.getResponseContentType(), result.getResponseContentType());
        Assert.assertEquals(rule.getResponseBody(), result.getResponseBody());
    }

    @Test
    public void extractInboundValue_nullRuleMatchingType_Test()  {
        thrown.expect(NullPointerException.class);

        ruleEngine.extractInboundValue(null, "", ctx, "/person/{name}", userCtxPath);
    }

    @Test
    public void extractInboundValue_reqHeader_Test()  {
        final String fieldName = "name";
        final String reqResponse = "Hey Joe";
        Mockito.when(request.getHeader(fieldName)).thenReturn(reqResponse);

        final String result = ruleEngine.extractInboundValue(RuleMatchingTypeEnum.REQUEST_HEADER, fieldName, ctx, "/person/{name}", userCtxPath);

        Assert.assertNotNull(result);
        Assert.assertEquals(reqResponse, result);
    }

    @Test
    public void extractInboundValue_reqParam_Test()  {
        final String fieldName = "name";
        final String reqResponse = "Hey Joe";
        Map<String, String[]> params = new HashMap<>();
        params.put(fieldName, new String[] { reqResponse });
        Mockito.when(request.getMethod()).thenReturn(HttpMethod.POST.name());
        Mockito.when(ctx.queryParamMap()).thenReturn(new HashMap<>());
        Mockito.when(ctx.contentType()).thenReturn(MediaType.APPLICATION_FORM_URLENCODED_VALUE);
        Mockito.when(ctx.body()).thenReturn(fieldName + "=" + reqResponse);

        final String result = ruleEngine.extractInboundValue(RuleMatchingTypeEnum.REQUEST_PARAM, "name", ctx, "/person/{name}", userCtxPath);

        Assert.assertNotNull(result);
        Assert.assertEquals(reqResponse, result);
    }

    @Test
    public void extractInboundValue_reqBody_Test()  {
        final String reqResponse = "Hey Joe";
        Mockito.when(ctx.body()).thenReturn(reqResponse);

        final String result = ruleEngine.extractInboundValue(RuleMatchingTypeEnum.REQUEST_BODY, "", ctx, "/person/{name}", userCtxPath);

        Assert.assertNotNull(result);
        Assert.assertEquals(reqResponse, result);
    }

    @Test
    public void extractInboundValue_pathVariable_Test()  {
        final String fieldName = "name";
        Mockito.when(request.getPathInfo()).thenReturn("/person/Joe");

        final String result = ruleEngine.extractInboundValue(RuleMatchingTypeEnum.PATH_VARIABLE, fieldName, ctx, "/person/{name}", userCtxPath);

        Assert.assertNotNull(result);
        Assert.assertEquals("Joe", result);
    }

    @Test
    public void extractInboundValue_jsonReqBody_Test()  {
        final String fieldName = "username";
        final String fieldValue = "admin";
        final String jsonBody = "{\"username\":\"" + fieldValue + "\"}";
        Mockito.when(ctx.body()).thenReturn(jsonBody);
        Mockito.when(request.getPathInfo()).thenReturn("/person/test");

        final String result = ruleEngine.extractInboundValue(RuleMatchingTypeEnum.REQUEST_BODY_JSON_ANY, fieldName, ctx, "/person/{name}", userCtxPath);

        Assert.assertNotNull(result);
        Assert.assertEquals(fieldValue, result);
    }

    @Test
    public void extractInboundValue_jsonReqBody_NotFound_Test()  {
        final String fieldName = "username";
        Mockito.when(ctx.body()).thenReturn("{\"foo\":\"bar\"}");
        Mockito.when(request.getPathInfo()).thenReturn("/person/test");

        final String result = ruleEngine.extractInboundValue(RuleMatchingTypeEnum.REQUEST_BODY_JSON_ANY, fieldName, ctx, "/person/{name}", userCtxPath);

        Assert.assertNull(result);
    }

    @Test
    public void extractInboundValue_jsonReqBody_invalidJson_Test()  {
        final String fieldName = "username";
        Mockito.when(ctx.body()).thenReturn("username = admin");
        Mockito.when(request.getPathInfo()).thenReturn("/person/test");

        final String result = ruleEngine.extractInboundValue(RuleMatchingTypeEnum.REQUEST_BODY_JSON_ANY, fieldName, ctx, "/person/{name}", userCtxPath);

        Assert.assertNull(result);
    }

    @Test
    public void extractInboundValue_jsonReqBody_null_Test() throws IOException {
        final String fieldName = "username";
        Mockito.when(request.getReader()).thenReturn(new java.io.BufferedReader(new java.io.StringReader("")));
        Mockito.when(request.getPathInfo()).thenReturn("/person/test");

        final String result = ruleEngine.extractInboundValue(RuleMatchingTypeEnum.REQUEST_BODY_JSON_ANY, fieldName, ctx, "/person/{name}", userCtxPath);

        Assert.assertNull(result);
    }

}
