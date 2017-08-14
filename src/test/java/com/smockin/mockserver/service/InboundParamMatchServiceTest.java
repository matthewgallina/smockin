package com.smockin.mockserver.service;

import com.smockin.admin.persistence.dao.RestfulMockDAO;
import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.admin.persistence.entity.RestfulMockDefinitionOrder;
import com.smockin.mockserver.service.InboundParamMatchService;
import com.smockin.mockserver.service.InboundParamMatchServiceImpl;
import com.smockin.mockserver.service.MockOrderingCounterService;
import com.smockin.mockserver.service.RuleEngine;
import com.smockin.mockserver.service.dto.RestfulResponse;
import com.smockin.mockserver.service.enums.InboundParamTypeEnum;
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
import org.mockito.runners.MockitoJUnitRunner;
import spark.Request;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by mgallina.
 */
public class InboundParamMatchServiceTest {

    private Request request;
    private InboundParamMatchServiceImpl inboundParamMatchServiceImpl;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {

        inboundParamMatchServiceImpl = new InboundParamMatchServiceImpl();
        request = Mockito.mock(Request.class);
    }

    @Test
    public void processParamMatch_NoToken_Test() {
        Assert.assertNull(inboundParamMatchServiceImpl.processParamMatch(request, "Hello World"));
    }

    @Test
    public void processParamMatch_InvalidToken_Test() {

        // Assertions
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Unsupported token : FOO");

        // Test
        final String responseBody = "Hello ${FOO name}";

        Assert.assertNull(inboundParamMatchServiceImpl.processParamMatch(request, responseBody));
    }

    @Test
    public void processParamMatch_header_Test() {

        // Setup
        final String responseBody = "Hello ${"+ InboundParamTypeEnum.REQ_HEAD.name() +"=name}";

        Mockito.when(request.headers("name")).thenReturn("Roger");
        Mockito.when(request.headers()).thenReturn(new HashSet<String>() {
            {
                add("name");
            }
        });

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, responseBody);

        // Assertions
        Assert.assertEquals("Hello Roger", result);
    }

    @Test
    public void processParamMatch_headerCase_Test() {

        // Setup
        final String responseBody = "Hello ${"+ InboundParamTypeEnum.REQ_HEAD.name() +"=NAME}";

        Mockito.when(request.headers("name")).thenReturn("Roger");
        Mockito.when(request.headers()).thenReturn(new HashSet<String>() {
            {
                add("name");
            }
        });

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, responseBody);

        // Assertions
        Assert.assertEquals("Hello Roger", result);
    }

    @Test
    public void processParamMatch_headerNoMatch_Test() {

        // Test
        final String responseBody = "Hello ${"+ InboundParamTypeEnum.REQ_HEAD.name() +"=name}";
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, responseBody);

        // Assertions
        Assert.assertEquals("Hello ", result);
    }

    @Test
    public void processParamMatch_reqParam_Test() {

        // Setup
        final String responseBody = "Hello ${"+ InboundParamTypeEnum.REQ_PARAM.name() +"=name}";

        Mockito.when(request.queryParams("name")).thenReturn("Roger");
        Mockito.when(request.queryParams()).thenReturn(new HashSet<String>() {
            {
                add("name");
            }
        });

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, responseBody);

        // Assertions
        Assert.assertEquals("Hello Roger", result);
    }

    @Test
    public void processParamMatch_reqParamCase_Test() {

        // Setup
        final String responseBody = "Hello ${"+ InboundParamTypeEnum.REQ_PARAM.name() +"=NAME}";

        Mockito.when(request.queryParams("name")).thenReturn("Roger");
        Mockito.when(request.queryParams()).thenReturn(new HashSet<String>() {
            {
                add("name");
            }
        });

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, responseBody);

        // Assertions
        Assert.assertEquals("Hello Roger", result);
    }

    @Test
    public void processParamMatch_reqParamNoMatch_Test() {

        // Test
        final String responseBody = "Hello ${"+ InboundParamTypeEnum.REQ_PARAM.name() +"=name}";
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, responseBody);

        // Assertions
        Assert.assertEquals("Hello ", result);
    }

    @Test
    public void processParamMatch_pathVar_Test() {

        // Setup
        final String responseBody = "Hello ${"+ InboundParamTypeEnum.PATH_VAR.name() +"=name}";

        Mockito.when(request.params()).thenReturn(new HashMap<String, String>() {
            {
                put(":name", "Roger");
            }
        });

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, responseBody);

        // Assertions
        Assert.assertEquals("Hello Roger", result);
    }

    @Test
    public void processParamMatch_pathVarCase_Test() {

        // Setup
        final String responseBody = "Hello ${"+ InboundParamTypeEnum.PATH_VAR.name() +"=NAME}";

        Mockito.when(request.params()).thenReturn(new HashMap<String, String>() {
            {
                put(":name", "Roger");
            }
        });

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, responseBody);

        // Assertions
        Assert.assertEquals("Hello Roger", result);
    }

    @Test
    public void processParamMatch_pathVarNoMatch_Test() {

        // Test
        final String responseBody = "Hello ${"+ InboundParamTypeEnum.PATH_VAR.name() +"=name}";
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, responseBody);

        // Assertions
        Assert.assertEquals("Hello ", result);
    }

    @Test
    public void enrichWithInboundParamMatches_multiMatchesAndSpaces_Test() {

        // Setup
        final String responseBody = "Hello ${"+ InboundParamTypeEnum.REQ_HEAD.name() +"=name}, you are ${"+ InboundParamTypeEnum.REQ_HEAD.name() +"= gender  } and are ${"+ InboundParamTypeEnum.REQ_HEAD.name() +"=age} years old";

        Mockito.when(request.headers("name")).thenReturn("Roger");
        Mockito.when(request.headers("age")).thenReturn("21");
        Mockito.when(request.headers("gender")).thenReturn("Male");
        Mockito.when(request.headers()).thenReturn(new HashSet<String>() {
            {
                add("name");
                add("age");
                add("gender");
            }
        });

        // Test
        final String result = inboundParamMatchServiceImpl.enrichWithInboundParamMatches(request, responseBody);

        // Assertions
        Assert.assertEquals("Hello Roger, you are Male and are 21 years old", result);
    }

    @Test
    public void enrichWithInboundParamMatches_partialMatch_Test() {

        // Setup
        final String responseBody = "Hello ${"+ InboundParamTypeEnum.REQ_HEAD.name() +"=name}, you are ${"+ InboundParamTypeEnum.REQ_HEAD.name() +"=age} years old";

        Mockito.when(request.headers("name")).thenReturn("Roger");
        Mockito.when(request.headers()).thenReturn(new HashSet<String>() {
            {
                add("name");
            }
        });

        // Test
        final String result = inboundParamMatchServiceImpl.enrichWithInboundParamMatches(request, responseBody);

        // Assertions
        Assert.assertEquals("Hello Roger, you are  years old", result);
    }

    @Test
    public void enrichWithInboundParamMatches_withIllegalToken_Test() {

        // Assertions
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Unsupported token : FOO");

        // Setup
        final String responseBody = "Hello ${"+ InboundParamTypeEnum.REQ_HEAD.name() +"=name}, you are ${FOO=age} years old";

        Mockito.when(request.headers("name")).thenReturn("Roger");
        Mockito.when(request.headers()).thenReturn(new HashSet<String>() {
            {
                add("name");
            }
        });

        inboundParamMatchServiceImpl.enrichWithInboundParamMatches(request, responseBody);
    }

}
