package com.smockin.mockserver.proxy;

import com.smockin.admin.persistence.enums.RestMethodEnum;
import com.smockin.mockserver.dto.ProxyActiveMock;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import java.util.*;

public class ProxyServerTest {

    private ProxyServerUtils proxyServerUtils;
    private List<ProxyActiveMock> activeMocks;

    @Before
    public void setUp() {

        proxyServerUtils = new ProxyServerUtils();

        activeMocks = new ArrayList<>();
        activeMocks.add(new ProxyActiveMock("/helloworld", null, RestMethodEnum.GET));
        activeMocks.add(new ProxyActiveMock("/hellomama", null, RestMethodEnum.POST));
        activeMocks.add(new ProxyActiveMock("/hellopapa", null, RestMethodEnum.PATCH));
        activeMocks.add(new ProxyActiveMock("/wiremock", null, RestMethodEnum.DELETE));
    }

    @Test
    public void checkForMockPathMatch_pathVar1_Match() {

        // Setup
        final String mockPath = "/hello";
        activeMocks.add(new ProxyActiveMock(mockPath, null, RestMethodEnum.GET));

        // Test
        checkForMockPathMatch(activeMocks, mockPath, mockPath);
    }

    @Test
    public void checkForMockPathMatch_pathVar2_Match() {

        // Setup
        final String mockPath = "/hello/:name";
        activeMocks.add(new ProxyActiveMock(mockPath, null, RestMethodEnum.GET));

        // Test
        checkForMockPathMatch(activeMocks, "/hello/bob", mockPath);
    }

    @Test
    public void checkForMockPathMatch_pathVar3_Match() {

        // Setup
        final String mockPath = "/hello/:name/age/:age";
        activeMocks.add(new ProxyActiveMock(mockPath, null, RestMethodEnum.GET));

        // Test
        checkForMockPathMatch(activeMocks, "/hello/bob/age/28", mockPath);
    }

    @Test
    public void checkForMockPathMatch_pathVar4_Match() {

        // Setup
        final String mockPath = "/hello/:name/hello/:name2/hello/:name3";
        activeMocks.add(new ProxyActiveMock(mockPath, null, RestMethodEnum.GET));

        // Test
        checkForMockPathMatch(activeMocks, "/hello/bob/hello/peter/hello/paul", mockPath);
    }

    @Test
    public void checkForMockPathMatch_multi_Match() {

        // Setup
        final String mockPath = "/hello";
        activeMocks.add(new ProxyActiveMock(mockPath, null, RestMethodEnum.GET));
        activeMocks.add(new ProxyActiveMock(mockPath, null, RestMethodEnum.POST));
        activeMocks.add(new ProxyActiveMock(mockPath, null, RestMethodEnum.DELETE));

        // Test
        final List<ProxyActiveMock> matchedPaths = proxyServerUtils.findMockPathMatches(mockPath, activeMocks);

        // Assertions
        Assert.assertNotNull(matchedPaths);
        Assert.assertEquals(3, matchedPaths.size());
        Assert.assertThat(matchedPaths.get(0).getPath(), CoreMatchers.is(mockPath));
        Assert.assertThat(matchedPaths.get(1).getPath(), CoreMatchers.is(mockPath));
        Assert.assertThat(matchedPaths.get(2).getPath(), CoreMatchers.is(mockPath));

        Assert.assertThat(matchedPaths.get(0).getMethod(), CoreMatchers.anyOf(CoreMatchers.is(RestMethodEnum.GET), CoreMatchers.is(RestMethodEnum.POST), CoreMatchers.is(RestMethodEnum.DELETE)));
        Assert.assertThat(matchedPaths.get(1).getMethod(), CoreMatchers.anyOf(CoreMatchers.is(RestMethodEnum.GET), CoreMatchers.is(RestMethodEnum.POST), CoreMatchers.is(RestMethodEnum.DELETE)));
        Assert.assertThat(matchedPaths.get(2).getMethod(), CoreMatchers.anyOf(CoreMatchers.is(RestMethodEnum.GET), CoreMatchers.is(RestMethodEnum.POST), CoreMatchers.is(RestMethodEnum.DELETE)));
    }

    private void checkForMockPathMatch(final List<ProxyActiveMock> activeMocks, final String inboundPath, final String expectedMockPath) {

        // Test
        final List<ProxyActiveMock> matchedPaths = proxyServerUtils.findMockPathMatches(inboundPath, activeMocks);

        // Assertions
        Assert.assertNotNull(matchedPaths);
        Assert.assertFalse(matchedPaths.isEmpty());
        Assert.assertThat(matchedPaths.get(0).getPath(), CoreMatchers.is(expectedMockPath));
    }

    @Test
    public void pathVariableRegexTest() {

        final String regex = "^ABC/" + ProxyServerUtils.PATH_VAR_REGEX + "+/DEF$";

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
