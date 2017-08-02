package com.smockin.mockserver.service;

import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.admin.persistence.entity.RestfulMockDefinitionOrder;
import com.smockin.admin.service.utils.GeneralUtils;
import com.smockin.mockserver.service.dto.RestfulResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by mgallina.
 */
public class MockOrderingCounterServiceTest {

    private MockOrderingCounterService mockOrderingCounterService;
    private RestfulMock restfulMock1, restfulMock2;

    private RestfulMockDefinitionOrder order1, order2, order3, order4, order5, order6;

    @Before
    public void setUp() {

        mockOrderingCounterService = new MockOrderingCounterServiceImpl();

        //
        // Mock Definition 1
        restfulMock1 = new RestfulMock();
        restfulMock1.setExtId(GeneralUtils.generateUUID());

        order1 = new RestfulMockDefinitionOrder(restfulMock1, 200, "application/json", "{ \"number\" : \"one\" }", 1);
        order1.setId(1);
        order2 = new RestfulMockDefinitionOrder(restfulMock1, 201, "application/json", "{ \"number\" : \"two\" }", 2);
        order2.setId(2);
        order3 = new RestfulMockDefinitionOrder(restfulMock1, 202, "application/json", "{ \"number\" : \"three\" }", 3);
        order3.setId(3);
        order4 = new RestfulMockDefinitionOrder(restfulMock1, 204, "application/json", "{ \"number\" : \"four\" }", 4);
        order4.setId(4);

        restfulMock1.getDefinitions().add(order1);
        restfulMock1.getDefinitions().add(order2);
        restfulMock1.getDefinitions().add(order3);
        restfulMock1.getDefinitions().add(order4);

        //
        // Mock Definition 2
        restfulMock2 = new RestfulMock();
        restfulMock2.setExtId(GeneralUtils.generateUUID());

        order5 = new RestfulMockDefinitionOrder(restfulMock2, 400, "application/json", "{ \"number\" : \"five\" }", 1);
        order5.setId(5);
        order6 = new RestfulMockDefinitionOrder(restfulMock2, 500, "application/json", "{ \"number\" : \"six\" }", 2);
        order6.setId(6);

        restfulMock2.getDefinitions().add(order5);
        restfulMock2.getDefinitions().add(order6);

    }

    @Test
    public void getNextInSequenceTest() {

        // Test (run 1)
        // Start with calls to 'RestfulMockDefinition 1'...
        final RestfulResponse result1 = mockOrderingCounterService.getNextInSequence(restfulMock1);

        // Assertions
        Assert.assertNotNull(result1);
        Assert.assertEquals(order1.getHttpStatusCode(), result1.getHttpStatusCode());
        Assert.assertEquals(order1.getResponseContentType(), result1.getResponseContentType());
        Assert.assertEquals(order1.getResponseBody(), result1.getResponseBody());

        // Test (run 2)
        final RestfulResponse result2 = mockOrderingCounterService.getNextInSequence(restfulMock1);

        // Assertions
        Assert.assertNotNull(result2);
        Assert.assertEquals(order2.getHttpStatusCode(), result2.getHttpStatusCode());
        Assert.assertEquals(order2.getResponseContentType(), result2.getResponseContentType());
        Assert.assertEquals(order2.getResponseBody(), result2.getResponseBody());

        // Test (run 3)
        final RestfulResponse result3 = mockOrderingCounterService.getNextInSequence(restfulMock1);

        // Assertions
        Assert.assertNotNull(result3);
        Assert.assertEquals(order3.getHttpStatusCode(), result3.getHttpStatusCode());
        Assert.assertEquals(order3.getResponseContentType(), result3.getResponseContentType());
        Assert.assertEquals(order3.getResponseBody(), result3.getResponseBody());

        // Test (run 4)
        // Call 'RestfulMockDefinition 2' in-between calls to 'RestfulMockDefinition 1'
        final RestfulResponse result11 = mockOrderingCounterService.getNextInSequence(restfulMock2);

        // Assertions
        Assert.assertNotNull(result11);
        Assert.assertEquals(order5.getHttpStatusCode(), result11.getHttpStatusCode());
        Assert.assertEquals(order5.getResponseContentType(), result11.getResponseContentType());
        Assert.assertEquals(order5.getResponseBody(), result11.getResponseBody());

        // Test (run 5)
        final RestfulResponse result4 = mockOrderingCounterService.getNextInSequence(restfulMock1);

        // Assertions
        Assert.assertNotNull(result4);
        Assert.assertEquals(order4.getHttpStatusCode(), result4.getHttpStatusCode());
        Assert.assertEquals(order4.getResponseContentType(), result4.getResponseContentType());
        Assert.assertEquals(order4.getResponseBody(), result4.getResponseBody());

        // Test (run 6)
        // ... And again call 'RestfulMockDefinition 2' in-between calls to 'RestfulMockDefinition 1'
        final RestfulResponse result22 = mockOrderingCounterService.getNextInSequence(restfulMock2);

        // Assertions
        Assert.assertNotNull(result22);
        Assert.assertEquals(order6.getHttpStatusCode(), result22.getHttpStatusCode());
        Assert.assertEquals(order6.getResponseContentType(), result22.getResponseContentType());
        Assert.assertEquals(order6.getResponseBody(), result22.getResponseBody());

        // Test (run 7)
        // This call to 'RestfulMockDefinition 1' should now come around full circle returning the 1st response (with order no 1)
        final RestfulResponse result5 = mockOrderingCounterService.getNextInSequence(restfulMock1);

        // Assertions
        Assert.assertNotNull(result5);
        Assert.assertEquals(order1.getHttpStatusCode(), result5.getHttpStatusCode());
        Assert.assertEquals(order1.getResponseContentType(), result5.getResponseContentType());
        Assert.assertEquals(order1.getResponseBody(), result5.getResponseBody());

        // Test (run 8)
        // This call to 'RestfulMockDefinition 2' should now come around full circle returning the 1st response (with order no 1)
        final RestfulResponse result33 = mockOrderingCounterService.getNextInSequence(restfulMock2);

        // Assertions
        Assert.assertNotNull(result33);
        Assert.assertEquals(order5.getHttpStatusCode(), result33.getHttpStatusCode());
        Assert.assertEquals(order5.getResponseContentType(), result33.getResponseContentType());
        Assert.assertEquals(order5.getResponseBody(), result33.getResponseBody());

    }

}