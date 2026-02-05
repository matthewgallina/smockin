package com.smockin.admin.interceptor;

import com.smockin.admin.service.AuthService;
import com.smockin.admin.service.SmockinUserService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AuthInterceptorTest {

    @Mock
    private SmockinUserService smockinUserService;

    @Mock
    private AuthService authService;

    @Spy
    @InjectMocks
    private AuthInterceptor authInterceptor;


    @Test
    public void matchExclusionUrl_exactMatchPass() {

        final String exclusionKey = "/smockin/test/mock";
        final String inboundUrl = "/smockin/test/mock";

        Assert.assertTrue(authInterceptor.matchExclusionUrl(exclusionKey, inboundUrl));
    }

    @Test
    public void matchExclusionUrl_exactMatchFail() {

        final String exclusionKey = "/smockin/test/mock";
        final String inboundUrl = "/smockin/test/mock2";

        Assert.assertFalse(authInterceptor.matchExclusionUrl(exclusionKey, inboundUrl));
    }

    @Test
    public void matchExclusionUrl_wildcardPathMatchPass() {

        final String exclusionKey = "/smockin/test/mock/*";
        final String inboundUrl = "/smockin/test/mock/123";

        Assert.assertTrue(authInterceptor.matchExclusionUrl(exclusionKey, inboundUrl));
    }

    @Test
    public void matchExclusionUrl_wildcardPathMatchFail() {

        final String exclusionKey = "/smockin/test/mock/*";
        final String inboundUrl = "/smockin/test/mock2/123";

        Assert.assertFalse(authInterceptor.matchExclusionUrl(exclusionKey, inboundUrl));
    }

    @Test
    public void matchExclusionUrl_wildcardSubPathMatchFail() {

        final String exclusionKey = "/smockin/test/mock/*";
        final String inboundUrl = "/test/mock/123";

        Assert.assertFalse(authInterceptor.matchExclusionUrl(exclusionKey, inboundUrl));
    }

    @Test
    public void matchExclusionUrl_wildcardSubPathMatchFail2() {

        final String exclusionKey = "/test/mock/*";
        final String inboundUrl = "/smockin/test/mock/123";

        Assert.assertFalse(authInterceptor.matchExclusionUrl(exclusionKey, inboundUrl));
    }

    @Test
    public void matchExclusionUrl_wildcardFileMatchPass() {

        final String exclusionKey = "*.html";
        final String inboundUrl = "/smockin/test/mock/file.html";

        Assert.assertTrue(authInterceptor.matchExclusionUrl(exclusionKey, inboundUrl));
    }

    @Test
    public void matchExclusionUrl_wildcardFileMatchFail() {

        final String exclusionKey = "*.html";
        final String inboundUrl = "/smockin/test/mock/file.htmlx";

        Assert.assertFalse(authInterceptor.matchExclusionUrl(exclusionKey, inboundUrl));
    }

}
