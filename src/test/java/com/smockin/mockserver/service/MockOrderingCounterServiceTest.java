package com.smockin.mockserver.service;

import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.admin.persistence.entity.RestfulMockDefinitionOrder;
import com.smockin.utils.GeneralUtils;
import com.smockin.mockserver.service.dto.RestfulResponseDTO;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;

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

        order1 = new RestfulMockDefinitionOrder(restfulMock1, 200, MediaType.APPLICATION_JSON_VALUE, "{ \"number\" : \"one\" }", 1, 0, false, 0 ,0);
        order1.setExtId(GeneralUtils.generateUUID());
        order2 = new RestfulMockDefinitionOrder(restfulMock1, 201, MediaType.APPLICATION_JSON_VALUE, "{ \"number\" : \"two\" }", 2, 0, false, 0 ,0);
        order2.setExtId(GeneralUtils.generateUUID());
        order3 = new RestfulMockDefinitionOrder(restfulMock1, 202, MediaType.APPLICATION_JSON_VALUE, "{ \"number\" : \"three\" }", 3, 0, false, 0 ,0);
        order3.setExtId(GeneralUtils.generateUUID());
        order4 = new RestfulMockDefinitionOrder(restfulMock1, 204, MediaType.APPLICATION_JSON_VALUE, "{ \"number\" : \"four\" }", 4, 0, false, 0 ,0);
        order4.setExtId(GeneralUtils.generateUUID());

        restfulMock1.getDefinitions().add(order1);
        restfulMock1.getDefinitions().add(order2);
        restfulMock1.getDefinitions().add(order3);
        restfulMock1.getDefinitions().add(order4);

        //
        // Mock Definition 2
        restfulMock2 = new RestfulMock();
        restfulMock2.setExtId(GeneralUtils.generateUUID());

        order5 = new RestfulMockDefinitionOrder(restfulMock2, 400, MediaType.APPLICATION_JSON_VALUE, "{ \"number\" : \"five\" }", 1, 0, false, 0 ,0);
        order5.setExtId(GeneralUtils.generateUUID());
        order6 = new RestfulMockDefinitionOrder(restfulMock2, 500, MediaType.APPLICATION_JSON_VALUE, "{ \"number\" : \"six\" }", 2, 0, false, 0 ,0);
        order6.setExtId(GeneralUtils.generateUUID());

        restfulMock2.getDefinitions().add(order5);
        restfulMock2.getDefinitions().add(order6);

    }

    @Test
    public void getNextInSequence_singleOccurence_Test() {

        // Test (run 1)
        // Start with calls to 'RestfulMockDefinition 1'...
        final RestfulResponseDTO result1 = mockOrderingCounterService.process(restfulMock1);

        // Assertions
        Assert.assertNotNull(result1);
        Assert.assertEquals(order1.getHttpStatusCode(), result1.getHttpStatusCode());
        Assert.assertEquals(order1.getResponseContentType(), result1.getResponseContentType());
        Assert.assertEquals(order1.getResponseBody(), result1.getResponseBody());


        // Test (run 2)
        final RestfulResponseDTO result2 = mockOrderingCounterService.process(restfulMock1);

        // Assertions
        Assert.assertNotNull(result2);
        Assert.assertEquals(order2.getHttpStatusCode(), result2.getHttpStatusCode());
        Assert.assertEquals(order2.getResponseContentType(), result2.getResponseContentType());
        Assert.assertEquals(order2.getResponseBody(), result2.getResponseBody());


        // Test (run 3)
        final RestfulResponseDTO result3 = mockOrderingCounterService.process(restfulMock1);

        // Assertions
        Assert.assertNotNull(result3);
        Assert.assertEquals(order3.getHttpStatusCode(), result3.getHttpStatusCode());
        Assert.assertEquals(order3.getResponseContentType(), result3.getResponseContentType());
        Assert.assertEquals(order3.getResponseBody(), result3.getResponseBody());


        // Test (run 4)
        // Call 'RestfulMockDefinition 2' in-between calls to 'RestfulMockDefinition 1'
        final RestfulResponseDTO result11 = mockOrderingCounterService.process(restfulMock2);

        // Assertions
        Assert.assertNotNull(result11);
        Assert.assertEquals(order5.getHttpStatusCode(), result11.getHttpStatusCode());
        Assert.assertEquals(order5.getResponseContentType(), result11.getResponseContentType());
        Assert.assertEquals(order5.getResponseBody(), result11.getResponseBody());


        // Test (run 5)
        final RestfulResponseDTO result4 = mockOrderingCounterService.process(restfulMock1);

        // Assertions
        Assert.assertNotNull(result4);
        Assert.assertEquals(order4.getHttpStatusCode(), result4.getHttpStatusCode());
        Assert.assertEquals(order4.getResponseContentType(), result4.getResponseContentType());
        Assert.assertEquals(order4.getResponseBody(), result4.getResponseBody());


        // Test (run 6)
        // ... And again call 'RestfulMockDefinition 2' in-between calls to 'RestfulMockDefinition 1'
        final RestfulResponseDTO result22 = mockOrderingCounterService.process(restfulMock2);

        // Assertions
        Assert.assertNotNull(result22);
        Assert.assertEquals(order6.getHttpStatusCode(), result22.getHttpStatusCode());
        Assert.assertEquals(order6.getResponseContentType(), result22.getResponseContentType());
        Assert.assertEquals(order6.getResponseBody(), result22.getResponseBody());


        // Test (run 7)
        // This call to 'RestfulMockDefinition 1' should now come around full circle returning the 1st response (with order no 1)
        final RestfulResponseDTO result5 = mockOrderingCounterService.process(restfulMock1);

        // Assertions
        Assert.assertNotNull(result5);
        Assert.assertEquals(order1.getHttpStatusCode(), result5.getHttpStatusCode());
        Assert.assertEquals(order1.getResponseContentType(), result5.getResponseContentType());
        Assert.assertEquals(order1.getResponseBody(), result5.getResponseBody());


        // Test (run 8)
        // This call to 'RestfulMockDefinition 2' should now come around full circle returning the 1st response (with order no 1)
        final RestfulResponseDTO result33 = mockOrderingCounterService.process(restfulMock2);

        // Assertions
        Assert.assertNotNull(result33);
        Assert.assertEquals(order5.getHttpStatusCode(), result33.getHttpStatusCode());
        Assert.assertEquals(order5.getResponseContentType(), result33.getResponseContentType());
        Assert.assertEquals(order5.getResponseBody(), result33.getResponseBody());

    }

    @Test
    public void getNextInSequence_multiOccurence_Test() {

        // Local Setup
        order1.setFrequencyCount(1);
        order2.setFrequencyCount(2);
        order3.setFrequencyCount(0);
        order4.setFrequencyCount(0);


        // Test (run 1)
        // Expect order 1 to be returned once
        final RestfulResponseDTO result1 = mockOrderingCounterService.process(restfulMock1);

        // Assertions
        Assert.assertNotNull(result1);
        Assert.assertEquals(order1.getHttpStatusCode(), result1.getHttpStatusCode());
        Assert.assertEquals(order1.getResponseContentType(), result1.getResponseContentType());
        Assert.assertEquals(order1.getResponseBody(), result1.getResponseBody());


        // Test (run 2)
        // Expect order 2 to be returned twice, once here...
        final RestfulResponseDTO result2 = mockOrderingCounterService.process(restfulMock1);

        // Assertions
        Assert.assertNotNull(result2);
        Assert.assertEquals(order2.getHttpStatusCode(), result2.getHttpStatusCode());
        Assert.assertEquals(order2.getResponseContentType(), result2.getResponseContentType());
        Assert.assertEquals(order2.getResponseBody(), result2.getResponseBody());


        // Test (run 3)
        // ... and again here
        final RestfulResponseDTO result3 = mockOrderingCounterService.process(restfulMock1);

        // Assertions
        Assert.assertNotNull(result3);
        Assert.assertEquals(order2.getHttpStatusCode(), result3.getHttpStatusCode());
        Assert.assertEquals(order2.getResponseContentType(), result3.getResponseContentType());
        Assert.assertEquals(order2.getResponseBody(), result3.getResponseBody());


        // Test (run 4)
        // Now expecting order 3
        final RestfulResponseDTO result4 = mockOrderingCounterService.process(restfulMock1);

        // Assertions
        Assert.assertNotNull(result4);
        Assert.assertEquals(order3.getHttpStatusCode(), result4.getHttpStatusCode());
        Assert.assertEquals(order3.getResponseContentType(), result4.getResponseContentType());
        Assert.assertEquals(order3.getResponseBody(), result4.getResponseBody());


        // Test (run 5)
        // Then order 4
        final RestfulResponseDTO result5 = mockOrderingCounterService.process(restfulMock1);

        // Assertions
        Assert.assertNotNull(result5);
        Assert.assertEquals(order4.getHttpStatusCode(), result5.getHttpStatusCode());
        Assert.assertEquals(order4.getResponseContentType(), result5.getResponseContentType());
        Assert.assertEquals(order4.getResponseBody(), result5.getResponseBody());


        // Test (run 6)
        // Finally back round to order 1
        final RestfulResponseDTO result6 = mockOrderingCounterService.process(restfulMock1);

        // Assertions
        Assert.assertNotNull(result6);
        Assert.assertEquals(order1.getHttpStatusCode(), result6.getHttpStatusCode());
        Assert.assertEquals(order1.getResponseContentType(), result6.getResponseContentType());
        Assert.assertEquals(order1.getResponseBody(), result6.getResponseBody());

    }

}
