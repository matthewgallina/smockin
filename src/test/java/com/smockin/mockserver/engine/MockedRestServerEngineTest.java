package com.smockin.mockserver.engine;

import com.smockin.admin.persistence.dao.RestfulMockDAO;
import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.admin.persistence.entity.RestfulMockDefinitionOrder;
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
@RunWith(MockitoJUnitRunner.class)
public class MockedRestServerEngineTest {

    @Mock
    private RestfulMockDAO restfulMockDAO;

    @Mock
    private RuleEngine ruleEngine;

    @Mock
    private MockOrderingCounterService mockOrderingCounterService;

    @Spy
    @InjectMocks
    private MockedRestServerEngine engine = new MockedRestServerEngine();

    @Mock
    private Request request;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private RestfulMock restfulMock;
    private RestfulMockDefinitionOrder order1, order2, order3;

    @Before
    public void setUp() {

        restfulMock = new RestfulMock();
        restfulMock.getDefinitions().add(order1 = new RestfulMockDefinitionOrder(restfulMock, 200, "text/html", "HelloWorld 1", 1));
        restfulMock.getDefinitions().add(order2 = new RestfulMockDefinitionOrder(restfulMock, 201, "text/html", "HelloWorld 2", 2));
        restfulMock.getDefinitions().add(order3 = new RestfulMockDefinitionOrder(restfulMock, 204, "text/html", "HelloWorld 3", 3));
    }

    @Test
    public void getDefault_Null_Test() {

        // Assertions
        thrown.expect(NullPointerException.class);

        // Test
        engine.getDefault(null);
    }

    @Test
    public void getDefault_NoDefinitionsDefined_Test() {

        // Assertions
        thrown.expect(IndexOutOfBoundsException.class);

        // Setup
        restfulMock.getDefinitions().clear();

        // Test
        engine.getDefault(restfulMock);
    }

    @Test
    public void getDefaultTest() {

        // Test (run 1)
        // Should always be response with 'order No 1'
        final RestfulResponse result1 = engine.getDefault(restfulMock);

        // Assertions
        Assert.assertNotNull(result1);
        Assert.assertEquals(order1.getHttpStatusCode(), result1.getHttpStatusCode());
        Assert.assertEquals(order1.getResponseContentType(), result1.getResponseContentType());
        Assert.assertEquals(order1.getResponseBody(), result1.getResponseBody());

        // Test (run 2)
        // ... and just to double check...
        final RestfulResponse result2 = engine.getDefault(restfulMock);

        // Assertions
        Assert.assertNotNull(result2);
        Assert.assertEquals(order1.getHttpStatusCode(), result2.getHttpStatusCode());
        Assert.assertEquals(order1.getResponseContentType(), result2.getResponseContentType());
        Assert.assertEquals(order1.getResponseBody(), result2.getResponseBody());

    }

    @Test
    public void processRequest__Test() {

        // TODO
//        engine.processRequest();

    }

    @Test
    public void processParamMatch_NoToken_Test() {
        Assert.assertNull(engine.processParamMatch(request, "Hello World"));
    }

    @Test
    public void processParamMatch_InvalidToken_Test() {

        // Assertions
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Unsupported token : FOO");

        // Test
        final String responseBody = "Hello ${FOO name}";

        Assert.assertNull(engine.processParamMatch(request, responseBody));
    }

    @Test
    public void processParamMatch_header_Test() {

        // Setup
        final String responseBody = "Hello ${"+ InboundParamTypeEnum.REQ_HEAD.name() +" name}";

        Mockito.when(request.headers("name")).thenReturn("Roger");
        Mockito.when(request.headers()).thenReturn(new HashSet<String>() {
            {
                add("name");
            }
        });

        // Test
        final String result = engine.processParamMatch(request, responseBody);

        // Assertions
        Assert.assertEquals("Hello Roger", result);
    }

    @Test
    public void processParamMatch_headerCase_Test() {

        // Setup
        final String responseBody = "Hello ${"+ InboundParamTypeEnum.REQ_HEAD.name() +" NAME}";

        Mockito.when(request.headers("name")).thenReturn("Roger");
        Mockito.when(request.headers()).thenReturn(new HashSet<String>() {
            {
                add("name");
            }
        });

        // Test
        final String result = engine.processParamMatch(request, responseBody);

        // Assertions
        Assert.assertEquals("Hello Roger", result);
    }

    @Test
    public void processParamMatch_headerNoMatch_Test() {

        // Test
        final String responseBody = "Hello ${"+ InboundParamTypeEnum.REQ_HEAD.name() +" name}";
        final String result = engine.processParamMatch(request, responseBody);

        // Assertions
        Assert.assertEquals("Hello ", result);
    }

    @Test
    public void processParamMatch_reqParam_Test() {

        // Setup
        final String responseBody = "Hello ${"+ InboundParamTypeEnum.REQ_PARAM.name() +" name}";

        Mockito.when(request.queryParams("name")).thenReturn("Roger");
        Mockito.when(request.queryParams()).thenReturn(new HashSet<String>() {
            {
                add("name");
            }
        });

        // Test
        final String result = engine.processParamMatch(request, responseBody);

        // Assertions
        Assert.assertEquals("Hello Roger", result);
    }

    @Test
    public void processParamMatch_reqParamCase_Test() {

        // Setup
        final String responseBody = "Hello ${"+ InboundParamTypeEnum.REQ_PARAM.name() +" NAME}";

        Mockito.when(request.queryParams("name")).thenReturn("Roger");
        Mockito.when(request.queryParams()).thenReturn(new HashSet<String>() {
            {
                add("name");
            }
        });

        // Test
        final String result = engine.processParamMatch(request, responseBody);

        // Assertions
        Assert.assertEquals("Hello Roger", result);
    }

    @Test
    public void processParamMatch_reqParamNoMatch_Test() {

        // Test
        final String responseBody = "Hello ${"+ InboundParamTypeEnum.REQ_PARAM.name() +" name}";
        final String result = engine.processParamMatch(request, responseBody);

        // Assertions
        Assert.assertEquals("Hello ", result);
    }

    @Test
    public void processParamMatch_pathVar_Test() {

        // Setup
        final String responseBody = "Hello ${"+ InboundParamTypeEnum.PATH_VAR.name() +" name}";

        Mockito.when(request.params()).thenReturn(new HashMap<String, String>() {
            {
                put(":name", "Roger");
            }
        });

        // Test
        final String result = engine.processParamMatch(request, responseBody);

        // Assertions
        Assert.assertEquals("Hello Roger", result);
    }

    @Test
    public void processParamMatch_pathVarCase_Test() {

        // Setup
        final String responseBody = "Hello ${"+ InboundParamTypeEnum.PATH_VAR.name() +" NAME}";

        Mockito.when(request.params()).thenReturn(new HashMap<String, String>() {
            {
                put(":name", "Roger");
            }
        });

        // Test
        final String result = engine.processParamMatch(request, responseBody);

        // Assertions
        Assert.assertEquals("Hello Roger", result);
    }

    @Test
    public void processParamMatch_pathVarNoMatch_Test() {

        // Test
        final String responseBody = "Hello ${"+ InboundParamTypeEnum.PATH_VAR.name() +" name}";
        final String result = engine.processParamMatch(request, responseBody);

        // Assertions
        Assert.assertEquals("Hello ", result);
    }

    @Test
    public void enrichWithInboundParamMatchesTest() {

        // Setup
        final String responseBody = "Hello ${"+ InboundParamTypeEnum.REQ_HEAD.name() +" name}, you are ${"+ InboundParamTypeEnum.REQ_HEAD.name() +" age} years old";

        Mockito.when(request.headers("name")).thenReturn("Roger");
        Mockito.when(request.headers("age")).thenReturn("21");
        Mockito.when(request.headers()).thenReturn(new HashSet<String>() {
            {
                add("name");
                add("age");
            }
        });

        // Test
        final String result = engine.enrichWithInboundParamMatches(request, responseBody);

        // Assertions
        Assert.assertEquals("Hello Roger, you are 21 years old", result);
    }

    @Test
    public void enrichWithInboundParamMatches_partialMatch_Test() {

        // Setup
        final String responseBody = "Hello ${"+ InboundParamTypeEnum.REQ_HEAD.name() +" name}, you are ${"+ InboundParamTypeEnum.REQ_HEAD.name() +" age} years old";

        Mockito.when(request.headers("name")).thenReturn("Roger");
        Mockito.when(request.headers()).thenReturn(new HashSet<String>() {
            {
                add("name");
            }
        });

        // Test
        final String result = engine.enrichWithInboundParamMatches(request, responseBody);

        // Assertions
        Assert.assertEquals("Hello Roger, you are  years old", result);
    }

    @Test
    public void enrichWithInboundParamMatches_withIllegalToken_Test() {

        // Assertions
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Unsupported token : FOO");

        // Setup
        final String responseBody = "Hello ${"+ InboundParamTypeEnum.REQ_HEAD.name() +" name}, you are ${FOO age} years old";

        Mockito.when(request.headers("name")).thenReturn("Roger");
        Mockito.when(request.headers()).thenReturn(new HashSet<String>() {
            {
                add("name");
            }
        });

        engine.enrichWithInboundParamMatches(request, responseBody);
    }

}
