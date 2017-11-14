package com.smockin.mockserver.service;

import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.admin.persistence.enums.RecordStatusEnum;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import com.smockin.admin.persistence.enums.RestMockTypeEnum;
import com.smockin.mockserver.service.bean.ProxiedKey;
import com.smockin.mockserver.service.dto.HttpProxiedDTO;
import com.smockin.mockserver.service.dto.RestfulResponseDTO;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;

import java.util.concurrent.*;

/**
 * Created by mgallina on 11/08/17.
 */
public class HttpProxyServiceQueueTest {

    private HttpProxyService proxyService;

    private RestfulMock mockReqHelloGet, mockReqHelloPost, mockReqHelloDelete, mockReqFooGet;
    private ProxiedKey helloKeyGet, helloKeyPost, helloKeyDelete, fooKeyGet;
    private HttpProxiedDTO helloGetDTO, helloPostDTO, helloDeleteDTO, fooGetDTO;


    @Before
    public void setUp() {

        proxyService = new HttpProxyServiceImpl();

        helloKeyGet = new ProxiedKey("/helloworld", RestMethodEnum.GET);
        helloKeyPost = new ProxiedKey("/helloworld", RestMethodEnum.POST);
        helloKeyDelete = new ProxiedKey("/helloworld", RestMethodEnum.DELETE);
        fooKeyGet = new ProxiedKey("/foo", RestMethodEnum.GET);

        mockReqHelloGet = new RestfulMock(helloKeyGet.getPath(), helloKeyGet.getMethod(), RecordStatusEnum.ACTIVE, RestMockTypeEnum.PROXY_HTTP, 500, 0, 0, false, false);
        mockReqHelloPost = new RestfulMock(helloKeyPost.getPath(), helloKeyPost.getMethod(), RecordStatusEnum.ACTIVE, RestMockTypeEnum.PROXY_HTTP, 500, 0, 0, false, false);
        mockReqHelloDelete = new RestfulMock(helloKeyDelete.getPath(), helloKeyDelete.getMethod(), RecordStatusEnum.ACTIVE, RestMockTypeEnum.PROXY_HTTP, 500, 0, 0, false, false);
        mockReqFooGet = new RestfulMock(fooKeyGet.getPath(), fooKeyGet.getMethod(), RecordStatusEnum.ACTIVE, RestMockTypeEnum.PROXY_HTTP, 500, 0, 0, false, false);

        helloGetDTO = new HttpProxiedDTO(helloKeyGet.getPath(), helloKeyGet.getMethod(), 200, MediaType.APPLICATION_JSON_VALUE, "{ \"msg\" : \"helloworld 1\" }");
        helloPostDTO = new HttpProxiedDTO(helloKeyPost.getPath(), helloKeyPost.getMethod(), 200, MediaType.APPLICATION_JSON_VALUE, "{ \"msg\" : \"helloworld 2\" }");
        helloDeleteDTO = new HttpProxiedDTO(helloKeyDelete.getPath(), helloKeyDelete.getMethod(), 200, MediaType.APPLICATION_JSON_VALUE, "{ \"msg\" : \"helloworld 3\" }");
        fooGetDTO = new HttpProxiedDTO(fooKeyGet.getPath(), fooKeyGet.getMethod(), 200, MediaType.APPLICATION_JSON_VALUE, "{ \"msg\" : \"foo 1\" }");

        proxyService.addResponse(helloGetDTO);
        proxyService.addResponse(helloPostDTO);
        proxyService.addResponse(helloDeleteDTO);
        proxyService.addResponse(fooGetDTO);
    }

    @Test
    public void waitForResponse_ConsumeAll_Test() throws InterruptedException, ExecutionException, TimeoutException {

        final RestfulResponseDTO dto1 = proxyService.waitForResponse(helloKeyGet.getPath(), mockReqHelloGet);
        Assert.assertNotNull(dto1);
        Assert.assertEquals(helloGetDTO.getBody(), dto1.getResponseBody());

        final RestfulResponseDTO dto2 = proxyService.waitForResponse(helloKeyPost.getPath(), mockReqHelloPost);
        Assert.assertNotNull(dto2);
        Assert.assertEquals(helloPostDTO.getBody(), dto2.getResponseBody());

        final RestfulResponseDTO dto3 = proxyService.waitForResponse(helloKeyDelete.getPath(), mockReqHelloDelete);
        Assert.assertNotNull(dto3);
        Assert.assertEquals(helloDeleteDTO.getBody(), dto3.getResponseBody());

        final RestfulResponseDTO dto4 = proxyService.waitForResponse(fooKeyGet.getPath(), mockReqFooGet);
        Assert.assertNotNull(dto4);
        Assert.assertEquals(fooGetDTO.getBody(), dto4.getResponseBody());

    }

    @Test
    public void waitForResponse_ConsumeAndWaitTimeout_Test() throws InterruptedException, ExecutionException, TimeoutException {

        final RestfulResponseDTO dto1 = proxyService.waitForResponse(helloKeyGet.getPath(), mockReqHelloGet);
        Assert.assertNotNull(dto1);
        Assert.assertEquals(helloGetDTO.getBody(), dto1.getResponseBody());

        Assert.assertNull(proxyService.waitForResponse(helloKeyGet.getPath(), mockReqHelloGet));
    }

    @Test
    public void clearSession_Test() throws InterruptedException, ExecutionException, TimeoutException {

        // Test
        proxyService.clearSession();

        // Assertions
        Assert.assertNull(proxyService.waitForResponse(helloKeyGet.getPath(), mockReqHelloGet));
    }

    @Test
    public void clearSession_ByPath_Hello_Test() throws InterruptedException, ExecutionException, TimeoutException {

        // Test
        proxyService.clearSession(helloKeyPost.getPath());

        // Assertions
        Assert.assertNull(proxyService.waitForResponse(helloKeyGet.getPath(), mockReqHelloGet));
        Assert.assertNull(proxyService.waitForResponse(helloKeyPost.getPath(), mockReqHelloPost));
        Assert.assertNull(proxyService.waitForResponse(helloKeyDelete.getPath(), mockReqHelloDelete));

        final RestfulResponseDTO dto4 = proxyService.waitForResponse(fooKeyGet.getPath(), mockReqFooGet);
        Assert.assertNotNull(dto4);
        Assert.assertEquals(fooGetDTO.getBody(), dto4.getResponseBody());
    }

    @Test
    public void clearSession_ByPath_Foo_Test() throws InterruptedException, ExecutionException, TimeoutException {

        // Test
        proxyService.clearSession(fooKeyGet.getPath());

        // Assertions
        Assert.assertNull(proxyService.waitForResponse(fooKeyGet.getPath(), mockReqFooGet));

        final RestfulResponseDTO dto1 = proxyService.waitForResponse(helloKeyGet.getPath(), mockReqHelloGet);
        Assert.assertNotNull(dto1);
        Assert.assertEquals(helloGetDTO.getBody(), dto1.getResponseBody());

        final RestfulResponseDTO dto2 = proxyService.waitForResponse(helloKeyPost.getPath(), mockReqHelloPost);
        Assert.assertNotNull(dto2);
        Assert.assertEquals(helloPostDTO.getBody(), dto2.getResponseBody());

        final RestfulResponseDTO dto3 = proxyService.waitForResponse(helloKeyDelete.getPath(), mockReqHelloDelete);
        Assert.assertNotNull(dto3);
        Assert.assertEquals(helloDeleteDTO.getBody(), dto3.getResponseBody());
    }

}
