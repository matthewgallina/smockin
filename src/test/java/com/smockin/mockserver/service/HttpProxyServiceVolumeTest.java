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
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by mgallina on 11/08/17.
 */
public class HttpProxyServiceVolumeTest {

    private RestfulMockDAO restfulMockDAO;
    private UserTokenServiceUtils userTokenServiceUtils;
    private MockedRestServerEngine mockedRestServerEngine;
    private HttpProxyService proxyService;
    private SmockinUser user;

    private final int proxiedTestCount = 1000;

    private ExecutorService executor;
    private Runnable[] producers;
    private TestCallable[] consumers;
    private ProxiedKey[] keys;
    private RestfulMock[] mocks;

    @Before
    public void setUp() throws RecordNotFoundException, ValidationException {

        user = new SmockinUser();
        user.setRole(SmockinUserRoleEnum.REGULAR);
        user.setCtxPath("foo");
        user.setSessionToken(GeneralUtils.generateUUID());

        executor = Executors.newFixedThreadPool(100);

        proxyService = new HttpProxyServiceImpl();

        // Had to resort to manually mocking, as there is a problem using @RunWith(MockitoJUnitRunner.class) where the mocks do not seem to work within the separate threads.
        proxyService = new HttpProxyServiceImpl();
        restfulMockDAO = Mockito.mock(RestfulMockDAO.class);
        userTokenServiceUtils = Mockito.mock(UserTokenServiceUtils.class);
        mockedRestServerEngine = Mockito.mock(MockedRestServerEngine.class);

        ReflectionTestUtils.setField(proxyService, "restfulMockDAO", restfulMockDAO);
        ReflectionTestUtils.setField(proxyService, "userTokenServiceUtils", userTokenServiceUtils);
        ReflectionTestUtils.setField(proxyService, "mockedRestServerEngine", mockedRestServerEngine);

        Mockito.doNothing().when(userTokenServiceUtils).validateRecordOwner(Matchers.any(SmockinUser.class), Matchers.anyString());

        keys = generateKeys();
        mocks = buildRestfulMocks(keys);

        producers = new Runnable[proxiedTestCount];

        for (int p=0; p < proxiedTestCount; p++) {

            final ProxiedKey pk = keys[p];
            final RestfulMock rm = mocks[p];

            Mockito.when(restfulMockDAO.findByExtId(rm.getExtId())).thenReturn(rm);
            Mockito.when(mockedRestServerEngine.buildUserPath(rm)).thenReturn(File.separator + user.getCtxPath() + rm.getPath());

            producers[p] = () -> {
                try {
                    proxyService.addResponse(rm.getExtId(), new HttpProxiedDTO(pk.getMethod(), 200, MediaType.APPLICATION_JSON_VALUE, "{ \"path\" : \"" + File.separator + user.getCtxPath() + pk.getPath() + "\" }"), user.getSessionToken());
                } catch (RecordNotFoundException | ValidationException e) {
                    Assert.fail();
                }
            };

        }

        consumers = new TestCallable[proxiedTestCount];

        for (int c=0; c < proxiedTestCount; c++) {

            final ProxiedKey pk = keys[c];
            final RestfulMock rm = mocks[c];

            consumers[c] = new TestCallable() {

                public ProxiedKey getProxiedKey() {
                     return pk;
                }

                @Override
                public Object call() {
                    return proxyService.waitForResponse(File.separator + user.getCtxPath() + pk.getPath(), rm);
                }
            };

        }

    }

    @Test
    public void proxyConcurrency_itemAlreadyInQueue_Test() throws InterruptedException, ExecutionException {

        // Test
        List<FutureAssertionWrapper> futureAssertionsList = new ArrayList<>();

        for (int i=0; i < proxiedTestCount; i++) {

            final TestCallable callable = consumers[i];
            futureAssertionsList.add(new FutureAssertionWrapper(callable.getProxiedKey(), executor.submit(callable)));

            executor.submit(producers[i]);
        }

        // Assertions
        int assertionsCount = 0;

        for (FutureAssertionWrapper faw : futureAssertionsList) {

            final ProxiedKey pk = faw.getProxiedKey();
            final Future future = faw.getFuture();

            do {

                final Object response = future.get();

                Assert.assertTrue(response instanceof RestfulResponseDTO);
                final RestfulResponseDTO restfulResponse = (RestfulResponseDTO)response;

                Assert.assertEquals(200, restfulResponse.getHttpStatusCode());
                Assert.assertEquals(MediaType.APPLICATION_JSON_VALUE, restfulResponse.getResponseContentType());
                Assert.assertNotNull(restfulResponse.getResponseBody());
                Assert.assertTrue(restfulResponse.getResponseBody().contains(pk.getPath()));
                Assert.assertTrue(restfulResponse.getHeaders().isEmpty());

                assertionsCount++;
            } while (!future.isDone());

        }

        Assert.assertEquals(proxiedTestCount, assertionsCount);

    }

    private ProxiedKey[] generateKeys() {

        final ProxiedKey[] keys = new ProxiedKey[proxiedTestCount];

        for (int p=0; p < proxiedTestCount; p++) {

            final int distinctAppender;

            if (p % 3 == 1) {
                distinctAppender = 1;
            } else if (p % 2 == 1) {
                distinctAppender = 2;
            } else {
                distinctAppender = 3;
            }

            keys[p] = new ProxiedKey("/helloworld" + distinctAppender, RestMethodEnum.GET);
        }

        return keys;
    }

    private RestfulMock[] buildRestfulMocks(final ProxiedKey[] pks) {

        return Stream.of(pks).map(this::buildRestfulMock)
                .collect(Collectors.toList())
                .toArray(new RestfulMock[] {});
    }

    private RestfulMock buildRestfulMock(final ProxiedKey pk) {
        final RestfulMock mockReq = new RestfulMock(pk.getPath(), pk.getMethod(), RecordStatusEnum.ACTIVE, RestMockTypeEnum.PROXY_HTTP, 3000, 0, 0, false, false, false, user, false, 0,0);
        mockReq.setExtId(GeneralUtils.generateUUID());
        return mockReq;
    }

    interface TestCallable extends Callable {
        ProxiedKey getProxiedKey();
    }

    class FutureAssertionWrapper {

        private final ProxiedKey proxiedKey;
        private final Future future;

        public FutureAssertionWrapper(ProxiedKey proxiedKey, Future future) {
            this.proxiedKey = proxiedKey;
            this.future = future;
        }

        public ProxiedKey getProxiedKey() {
            return proxiedKey;
        }
        public Future getFuture() {
            return future;
        }

    }

}
