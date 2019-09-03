package com.smockin.mockserver.service;

import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.dao.RestfulMockDAO;
import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.persistence.enums.RestMockTypeEnum;
import com.smockin.admin.persistence.enums.RecordStatusEnum;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import com.smockin.admin.persistence.enums.SmockinUserRoleEnum;
import com.smockin.admin.service.utils.UserTokenServiceUtils;
import com.smockin.mockserver.engine.MockedRestServerEngine;
import com.smockin.mockserver.service.bean.ProxiedKey;
import com.smockin.mockserver.service.dto.HttpProxiedDTO;
import com.smockin.mockserver.service.dto.RestfulResponseDTO;
import com.smockin.utils.GeneralUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.util.concurrent.*;

/**
 * Created by mgallina on 11/08/17.
 */
public class HttpProxyServiceTest {

    private ProxiedKey pxKey;
    private RestfulMock mockReq;
    private HttpProxiedDTO pxDto;

    private ExecutorService executor;
    private Runnable producer1;
    private Callable consumer1;
    private SmockinUser user;

    private RestfulMockDAO restfulMockDAO;
    private UserTokenServiceUtils userTokenServiceUtils;
    private MockedRestServerEngine mockedRestServerEngine;
    private HttpProxyService proxyService;

    @Before
    public void setUp() throws RecordNotFoundException, ValidationException {

        user = new SmockinUser();
        user.setRole(SmockinUserRoleEnum.REGULAR);
        user.setCtxPath("foo");
        user.setSessionToken(GeneralUtils.generateUUID());

        executor = Executors.newFixedThreadPool(5);

        // Had to resort to manually mocking, as there is a problem using @RunWith(MockitoJUnitRunner.class) where the mocks do not seem to work within the separate threads.
        proxyService = new HttpProxyServiceImpl();
        restfulMockDAO = Mockito.mock(RestfulMockDAO.class);
        userTokenServiceUtils = Mockito.mock(UserTokenServiceUtils.class);
        mockedRestServerEngine = Mockito.mock(MockedRestServerEngine.class);

        ReflectionTestUtils.setField(proxyService, "restfulMockDAO", restfulMockDAO);
        ReflectionTestUtils.setField(proxyService, "userTokenServiceUtils", userTokenServiceUtils);
        ReflectionTestUtils.setField(proxyService, "mockedRestServerEngine", mockedRestServerEngine);

        pxKey = new ProxiedKey("/helloworld", RestMethodEnum.GET);
        mockReq = new RestfulMock(pxKey.getPath(), pxKey.getMethod(), RecordStatusEnum.ACTIVE, RestMockTypeEnum.PROXY_HTTP, 0, 0, 0, false, false, false, user, false, 0,0, null);
        pxDto = new HttpProxiedDTO(pxKey.getMethod(), 200, MediaType.APPLICATION_JSON_VALUE, "{ \"msg\" : \"helloworld\" }");

        Mockito.when(restfulMockDAO.findByExtId(Matchers.anyString())).thenReturn(mockReq);
        Mockito.doNothing().when(userTokenServiceUtils).validateRecordOwner(Matchers.any(SmockinUser.class), Matchers.anyString());

        Mockito.when(mockedRestServerEngine.buildUserPath(mockReq)).thenReturn(File.separator + user.getCtxPath() + mockReq.getPath());

        producer1 = () -> {
            try {
                proxyService.addResponse(mockReq.getExtId(), pxDto, user.getSessionToken());
            } catch (RecordNotFoundException | ValidationException e) {
                Assert.fail();
            }
        };

        consumer1 = () -> proxyService.waitForResponse(File.separator + user.getCtxPath() + mockReq.getPath(), mockReq);

    }

    @Test
    public void proxyConcurrency_itemAlreadyInQueue_Test() throws InterruptedException, ExecutionException, TimeoutException {

        // Test
        executor.submit(producer1);

        final Future future = executor.submit(consumer1);

        while (!future.isDone()) {

            final Object response = future.get(Long.valueOf(3), TimeUnit.SECONDS);

            // Assertions
            Assert.assertTrue(response instanceof RestfulResponseDTO);
            final RestfulResponseDTO restfulResponse = (RestfulResponseDTO)response;

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
            Assert.assertTrue(response instanceof RestfulResponseDTO);
            final RestfulResponseDTO restfulResponse = (RestfulResponseDTO)response;

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

        consumer1 = () -> proxyService.waitForResponse(mockReq.getPath(), mockReq);

        // Test
        final Future future = executor.submit(consumer1);

        while (!future.isDone()) {

            // Assertions
            Assert.assertNull(future.get());
        }

    }

}
