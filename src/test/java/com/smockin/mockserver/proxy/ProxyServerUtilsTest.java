package com.smockin.mockserver.proxy;

import com.smockin.admin.persistence.enums.RestMethodEnum;
import com.smockin.mockserver.dto.ProxyActiveMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public class ProxyServerUtilsTest {

    private ProxyServerUtils proxyServerUtils;

    @Before
    public void setUp() {

        proxyServerUtils = new ProxyServerUtils();

    }

    @Test
    public void buildMockUrlPass() throws MalformedURLException {

        // Setup
        final URL inboundUrl = new URL(proxyServerUtils.fixProtocolWithDummyPrefix("/hello"));

        // Test
        final String url = proxyServerUtils.buildMockUrl(inboundUrl, 9005, "bob");

        // Assertions
        Assert.assertNotNull(url);
        Assert.assertEquals("http://localhost:9005/bob/hello", url);
    }

    @Test
    public void findMockMethodMatchPass() {

        // Setup
        final List<ProxyActiveMock> activeMocks = new ArrayList<>();
        activeMocks.add(new ProxyActiveMock("/helloworld", null, RestMethodEnum.GET));
        activeMocks.add(new ProxyActiveMock("/helloworld", null, RestMethodEnum.POST));
        activeMocks.add(new ProxyActiveMock("/helloworld", null, RestMethodEnum.PATCH));
        activeMocks.add(new ProxyActiveMock("/helloworld", null, RestMethodEnum.DELETE));

        // Test
        final Optional<ProxyActiveMock> proxyActiveMockOpt = proxyServerUtils.findMockMethodMatch("PATCH", activeMocks);

        // Assertions
        Assert.assertNotNull(proxyActiveMockOpt);
        Assert.assertTrue(proxyActiveMockOpt.isPresent());
        Assert.assertEquals(RestMethodEnum.PATCH, proxyActiveMockOpt.get().getMethod());

    }

}
