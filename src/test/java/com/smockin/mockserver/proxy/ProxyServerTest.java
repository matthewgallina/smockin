package com.smockin.mockserver.proxy;

import com.smockin.admin.persistence.enums.RestMethodEnum;
import org.apache.commons.lang3.tuple.Pair;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

public class ProxyServerTest {

    private ProxyServer proxyServer;
    private Map<String, List<RestMethodEnum>> activeMocks;

    @Before
    public void setUp() {

        proxyServer = new ProxyServer();

        activeMocks = new HashMap<>();
        activeMocks.put("/helloworld", Arrays.asList(RestMethodEnum.GET));
        activeMocks.put("/hellomama", Arrays.asList(RestMethodEnum.POST));
        activeMocks.put("/hellopapa", Arrays.asList(RestMethodEnum.PATCH));
        activeMocks.put("/wiremock", Arrays.asList(RestMethodEnum.DELETE));

    }

    @Test
    public void checkForMockPathMatch_pathVar1_Match() {

        // Setup
        final String mockPath = "/hello";
        activeMocks.put(mockPath, Arrays.asList(RestMethodEnum.GET));

        // Test
        checkForMockPathMatch(activeMocks, "/hello", mockPath);
    }

    @Test
    public void checkForMockPathMatch_pathVar2_Match() {

        // Setup
        final String mockPath = "/hello/:name";
        activeMocks.put(mockPath, Arrays.asList(RestMethodEnum.GET));

        // Test
        checkForMockPathMatch(activeMocks, "/hello/bob", mockPath);
    }

    @Test
    public void checkForMockPathMatch_pathVar3_Match() {

        // Setup
        final String mockPath = "/hello/:name/age/:age";
        activeMocks.put(mockPath, Arrays.asList(RestMethodEnum.GET));

        // Test
        checkForMockPathMatch(activeMocks, "/hello/bob/age/28", mockPath);
    }

    @Test
    public void checkForMockPathMatch_pathVar4_Match() {

        // Setup
        final String mockPath = "/hello/:name/hello/:name2/hello/:name3";
        activeMocks.put(mockPath, Arrays.asList(RestMethodEnum.GET));

        // Test
        checkForMockPathMatch(activeMocks, "/hello/bob/hello/peter/hello/paul", mockPath);
    }

    private void checkForMockPathMatch(final Map<String, List<RestMethodEnum>> activeMocks, final String inboundPath, final String expectedMockPath) {

        // Test
        final Optional<Map.Entry<String, List<RestMethodEnum>>> matchedPath = proxyServer.checkForMockPathMatch(inboundPath, activeMocks);

        // Assertions
        Assert.assertNotNull(matchedPath);
        Assert.assertTrue(matchedPath.isPresent());
        Assert.assertThat(matchedPath.get().getKey(), CoreMatchers.is(expectedMockPath));
    }

    @Test
    public void pathVariableRegexTest() {

        final String regex = "^ABC/" + ProxyServer.PATH_VAR_REGEX + "+/DEF$";

        Assert.assertTrue("ABC/ABC/DEF".matches(regex));
        Assert.assertTrue("ABC/123/DEF".matches(regex));
        Assert.assertTrue("ABC/A-B+C/DEF".matches(regex));
        Assert.assertTrue("ABC/1-2+3/DEF".matches(regex));
        Assert.assertTrue("ABC/1:2!3/DEF".matches(regex));
        Assert.assertTrue("ABC/1~2$3/DEF".matches(regex));
        Assert.assertTrue("ABC/1&2(3)/DEF".matches(regex));
        Assert.assertTrue("ABC/1*2,3/DEF".matches(regex));
        Assert.assertTrue("ABC/1=2@3/DEF".matches(regex));

        Assert.assertFalse("ABC/A/B/DEF".matches(regex));
        Assert.assertFalse("ABC/1/2/DEF".matches(regex));
        Assert.assertFalse("ABC/=/+/DEF".matches(regex));
    }

}
