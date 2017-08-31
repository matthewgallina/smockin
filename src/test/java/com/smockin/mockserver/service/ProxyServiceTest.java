package com.smockin.mockserver.service;

import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.admin.persistence.enums.MockTypeEnum;
import com.smockin.admin.persistence.enums.RecordStatusEnum;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import com.smockin.mockserver.service.bean.ProxiedKey;
import com.smockin.mockserver.service.dto.ProxiedDTO;
import com.smockin.mockserver.service.dto.RestfulResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;

import java.util.concurrent.*;

/**
 * Created by mgallina on 11/08/17.
 */
public class ProxyServiceTest {

    private ProxyService proxyService;

    private ProxiedKey pxKey;
    private RestfulMock mockReq;
    private ProxiedDTO pxDto;

    private ExecutorService executor;
    private Runnable producer1;
    private Callable consumer1;

    @Before
    public void setUp() {

        executor = Executors.newFixedThreadPool(5);

        proxyService = new ProxyServiceImpl();

        pxKey = new ProxiedKey("/helloworld", RestMethodEnum.GET);
        mockReq = new RestfulMock(pxKey.getPath(), pxKey.getMethod(), RecordStatusEnum.ACTIVE, MockTypeEnum.PROXY_HTTP, 0, 0, false);
        pxDto = new ProxiedDTO(pxKey.getPath(), pxKey.getMethod(), 200, MediaType.APPLICATION_JSON_VALUE, "{ \"msg\" : \"helloworld\" }");

        producer1 = new Runnable() {
            @Override
            public void run() {
                proxyService.addResponse(pxDto);
            }
        };

        consumer1 = new Callable() {
            @Override
            public Object call() {
                return proxyService.waitForResponse(mockReq.getPath(), mockReq);
            }
        };

    }

    @Test
    public void proxyConcurrency_itemAlreadyInQueue_Test() throws InterruptedException, ExecutionException, TimeoutException {

        // Test
        executor.submit(producer1);

        final Future future = executor.submit(consumer1);

        while (!future.isDone()) {

            final Object response = future.get(Long.valueOf(3), TimeUnit.SECONDS);

            // Assertions
            Assert.assertTrue(response instanceof RestfulResponse);
            final RestfulResponse restfulResponse = (RestfulResponse)response;

            Assert.assertEquals(pxDto.getHttpStatusCode(), restfulResponse.getHttpStatusCode());
            Assert.assertEquals(pxDto.getResponseContentType(), restfulResponse.getResponseContentType());
            Assert.assertEquals(pxDto.getBody(), restfulResponse.getResponseBody());
            Assert.assertTrue(restfulResponse.getHeaders().isEmpty());
        }

    }

    @Test(expected = TimeoutException.class)
    public void proxyConcurrency_indefiniteWait_Test() throws InterruptedException, ExecutionException, TimeoutException {

        // The proxy mock has a 'proxyTimeOutInMillis' set to 'zero' and so the consumer thread will block until the internal service has reached the max timeout of 60 seconds (see ProxyService.MAX_TIMEOUT_MILLIS).
        // This test therefore deliberately times the Future out after only 3 seconds and so expects a TimeoutException.

        // Test
        final Future future = executor.submit(consumer1);

        while (!future.isDone()) {

            final Object response = future.get(Long.valueOf(3), TimeUnit.SECONDS);

            // Assertions
            Assert.assertTrue(response instanceof RestfulResponse);
            final RestfulResponse restfulResponse = (RestfulResponse)response;

            Assert.assertEquals(pxDto.getHttpStatusCode(), restfulResponse.getHttpStatusCode());
            Assert.assertEquals(pxDto.getResponseContentType(), restfulResponse.getResponseContentType());
            Assert.assertEquals(pxDto.getBody(), restfulResponse.getResponseBody());
            Assert.assertTrue(restfulResponse.getHeaders().isEmpty());
        }

    }

    @Test
    public void proxyConcurrency_timeoutWait_Test() throws InterruptedException, ExecutionException, TimeoutException {

        // The proxy mock is now set with a 'proxyTimeOutInMillis' of 3000 milliseconds which means the consumer thread should only have to wait for 3 seconds until this times out internally.
        // This test does not time out on the Future, in order to prove that.

        // Setup
        mockReq.setProxyTimeOutInMillis(3000);

        consumer1 = new Callable() {
            @Override
            public Object call() {
                return proxyService.waitForResponse(mockReq.getPath(), mockReq);
            }
        };

        // Test
        final Future future = executor.submit(consumer1);

        while (!future.isDone()) {

            // Assertions
            Assert.assertNull(future.get());
        }

    }

}
