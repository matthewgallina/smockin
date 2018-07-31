package com.smockin.mockserver.service;

import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.admin.persistence.enums.RestMockTypeEnum;
import com.smockin.admin.persistence.enums.RecordStatusEnum;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import com.smockin.mockserver.service.bean.ProxiedKey;
import com.smockin.mockserver.service.dto.HttpProxiedDTO;
import com.smockin.mockserver.service.dto.RestfulResponseDTO;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Created by mgallina on 11/08/17.
 */
public class HttpProxyServiceVolumeTest {

    private HttpProxyService proxyService;
    private final int proxiedTestCount = 1000;

    private ExecutorService executor;
    private Runnable[] producers;
    private TestCallable[] consumers;
    private ProxiedKey[] keys;


    @Before
    public void setUp() {

        executor = Executors.newFixedThreadPool(100);

        proxyService = new HttpProxyServiceImpl();

        keys = generateKeys();

        producers = new Runnable[proxiedTestCount];

        for (int p=0; p < proxiedTestCount; p++) {

            final ProxiedKey pk = keys[p];

            producers[p] = new Runnable() {
                @Override
                public void run() {
                    proxyService.addResponse(new HttpProxiedDTO(pk.getPath(), pk.getMethod(), 200, MediaType.APPLICATION_JSON_VALUE, "{ \"path\" : \"" + pk.getPath() + "\" }"));
                }
            };

        }

        consumers = new TestCallable[proxiedTestCount];

        for (int c=0; c < proxiedTestCount; c++) {

            final ProxiedKey pk = keys[c];

            consumers[c] = new TestCallable() {

                public ProxiedKey getProxiedKey() {
                     return pk;
                }

                @Override
                public Object call() {
                    return proxyService.waitForResponse(pk.getPath(), new RestfulMock(pk.getPath(), pk.getMethod(), RecordStatusEnum.ACTIVE, RestMockTypeEnum.PROXY_HTTP, 3000, 0, 0, false, false, null));
                }
            };

        }

    }

    @Test
    public void proxyConcurrency_itemAlreadyInQueue_Test() throws InterruptedException, ExecutionException, TimeoutException {

        // Test
        List<FutureAssertionWrapper> futureAssertionsList = new ArrayList<FutureAssertionWrapper>();

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
