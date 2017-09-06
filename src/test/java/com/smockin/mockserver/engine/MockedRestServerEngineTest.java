package com.smockin.mockserver.engine;

import com.smockin.admin.persistence.dao.RestfulMockDAO;
import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.admin.persistence.entity.RestfulMockDefinitionOrder;
import com.smockin.admin.persistence.enums.MockTypeEnum;
import com.smockin.mockserver.service.MockOrderingCounterService;
import com.smockin.mockserver.service.ProxyService;
import com.smockin.mockserver.service.RuleEngine;
import com.smockin.mockserver.service.dto.RestfulResponseDTO;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

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
    private ProxyService proxyService;

    @Mock
    private MockOrderingCounterService mockOrderingCounterService;

    @Spy
    @InjectMocks
    private MockedRestServerEngine engine = new MockedRestServerEngine();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private RestfulMock restfulMock;
    private RestfulMockDefinitionOrder order1, order2, order3;

    @Before
    public void setUp() {

        restfulMock = new RestfulMock();
        restfulMock.getDefinitions().add(order1 = new RestfulMockDefinitionOrder(restfulMock, 200, "text/html", "HelloWorld 1", 1, 0, false, 0, 0));
        restfulMock.getDefinitions().add(order2 = new RestfulMockDefinitionOrder(restfulMock, 201, "text/html", "HelloWorld 2", 2, 0, false, 0, 0));
        restfulMock.getDefinitions().add(order3 = new RestfulMockDefinitionOrder(restfulMock, 204, "text/html", "HelloWorld 3", 3, 0, false, 0, 0));
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
        final RestfulResponseDTO result1 = engine.getDefault(restfulMock);

        // Assertions
        Assert.assertNotNull(result1);
        Assert.assertEquals(order1.getHttpStatusCode(), result1.getHttpStatusCode());
        Assert.assertEquals(order1.getResponseContentType(), result1.getResponseContentType());
        Assert.assertEquals(order1.getResponseBody(), result1.getResponseBody());

        // Test (run 2)
        // ... and just to double check...
        final RestfulResponseDTO result2 = engine.getDefault(restfulMock);

        // Assertions
        Assert.assertNotNull(result2);
        Assert.assertEquals(order1.getHttpStatusCode(), result2.getHttpStatusCode());
        Assert.assertEquals(order1.getResponseContentType(), result2.getResponseContentType());
        Assert.assertEquals(order1.getResponseBody(), result2.getResponseBody());

    }

    @Test
    public void getDefault_Proxy_Test() {

        // Setup
        restfulMock.setMockType(MockTypeEnum.PROXY_HTTP);

        // Test
        final RestfulResponseDTO result = engine.getDefault(restfulMock);

        // Assertions
        Assert.assertNotNull(result);
        Assert.assertEquals(404, result.getHttpStatusCode());
        Assert.assertNull(result.getResponseContentType());
        Assert.assertNull(result.getResponseBody());
        Assert.assertTrue(result.getHeaders().isEmpty());
    }



        @Test
    public void processRequest__Test() {

        // TODO
//        engine.processRequest();

    }

}
