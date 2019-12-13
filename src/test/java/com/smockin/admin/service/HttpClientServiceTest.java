package com.smockin.admin.service;

import com.smockin.admin.dto.HttpClientCallDTO;
import com.smockin.admin.dto.response.HttpClientResponseDTO;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.message.BasicHeader;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by mgallina on 15/08/17.
 */
@RunWith(MockitoJUnitRunner.class)
public class HttpClientServiceTest {

    @Mock
    private MockedServerEngineService mockedServerEngineService;

    @Mock
    private Request request;

    @Mock
    private Response response;

    @Mock
    private HttpResponse httpResponse;

    @Mock
    private HttpEntity httpEntity;

    @Mock
    private StatusLine statusLine;

    @Spy
    @InjectMocks
    private HttpClientServiceImpl httpClientServiceImpl = new HttpClientServiceImpl();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {

    }

    @Test
    public void validateRequest_urlMissing_Test() throws ValidationException {

        // Setup
        final HttpClientCallDTO httpClientCallDTO = new HttpClientCallDTO();
        httpClientCallDTO.setMethod(RestMethodEnum.GET);

        // Assertions
        thrown.expect(ValidationException.class);
        thrown.expectMessage("url is required");

        // Test
        httpClientServiceImpl.validateRequest(httpClientCallDTO);
    }

    @Test
    public void validateRequest_methodMissing_Test() throws ValidationException {

        // Setup
        final HttpClientCallDTO httpClientCallDTO = new HttpClientCallDTO();
        httpClientCallDTO.setUrl("/foo");

        // Assertions
        thrown.expect(ValidationException.class);
        thrown.expectMessage("method is required");

        // Test
        httpClientServiceImpl.validateRequest(httpClientCallDTO);
    }

    @Test
    public void validateRequestTest() throws ValidationException {

        // Setup
        final HttpClientCallDTO httpClientCallDTO = new HttpClientCallDTO();
        httpClientCallDTO.setUrl("/foo");
        httpClientCallDTO.setMethod(RestMethodEnum.GET);

        // Test
        httpClientServiceImpl.validateRequest(httpClientCallDTO);
    }

    @Test
    public void applyRequestHeadersTest() {

        // Setup
        final Map<String, String> requestHeaders = new HashMap<String, String>() {
            {
                put("auth", "X");
                put("one", "1");
            }
        };

        // Test
        httpClientServiceImpl.applyRequestHeaders(request, requestHeaders);

        // Assertions
        Mockito.verify(request, Mockito.times(1)).addHeader("auth", "X");
        Mockito.verify(request, Mockito.times(1)).addHeader("one", "1");
    }

    @Test
    public void applyRequestHeaders_Null_Test() {

        // Test
        httpClientServiceImpl.applyRequestHeaders(request, null);

        // Assertions
        Mockito.verify(request, Mockito.never()).addHeader(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void extractResponseHeadersTest() {

        // Setup
        Mockito.when(httpResponse.getAllHeaders()).thenReturn(new Header[] { new BasicHeader("one", "1"), new BasicHeader("two", "2") });

        // Test
        final Map<String, String> result = httpClientServiceImpl.extractResponseHeaders(httpResponse);

        // Assertions
        Assert.assertNotNull(result);
        Assert.assertEquals(2, result.size());
        Assert.assertTrue(result.containsKey("one"));
        Assert.assertTrue(result.containsKey("two"));
    }

    @Test
    public void extractResponseBodyTest() throws IOException {

        // Setup
        Mockito.when(httpResponse.getEntity()).thenReturn(httpEntity);
        Mockito.when(httpEntity.getContent()).thenReturn(IOUtils.toInputStream("Foo", Charset.defaultCharset()));

        // Test
        final String result = httpClientServiceImpl.extractResponseBody(httpResponse);

        // Assertions
        Assert.assertEquals("Foo", result);
    }

    @Test
    public void executeRequestTest() throws IOException {

        // Setup
        final Map<String, String> requestHeaders = new HashMap<String, String>() {
            {
                put("auth", "X");
            }
        };

        Mockito.when(request.execute()).thenReturn(response);
        Mockito.when(response.returnResponse()).thenReturn(httpResponse);
        Mockito.when(httpResponse.getAllHeaders()).thenReturn(new Header[] { new BasicHeader("one", "1") });

        Mockito.when(httpResponse.getStatusLine()).thenReturn(statusLine);
        Mockito.when(statusLine.getStatusCode()).thenReturn(200);

        Mockito.when(httpResponse.getEntity()).thenReturn(httpEntity);
        final Header contentTypeHeader = Mockito.mock(Header.class);
        Mockito.when(httpResponse.getEntity().getContentType()).thenReturn(contentTypeHeader);
        Mockito.when(contentTypeHeader.getValue()).thenReturn("text/plain");
        Mockito.when(httpEntity.getContent()).thenReturn(IOUtils.toInputStream("Foo", Charset.defaultCharset()));

        // Test
        final HttpClientResponseDTO result = httpClientServiceImpl.executeRequest(request, requestHeaders);

        // Assertions
        Assert.assertNotNull(result);
        Assert.assertEquals(200, result.getStatus());
        Assert.assertEquals("text/plain", result.getContentType());
        Assert.assertEquals("Foo", result.getBody());
        Assert.assertEquals(1, result.getHeaders().size());
        Assert.assertEquals("1", result.getHeaders().get("one"));
    }

}
