package com.smockin.mockserver.service;

import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import spark.Request;
import javax.script.ScriptException;
import java.util.*;

public class JavaScriptResponseHandlerTest {

    private JavaScriptResponseHandlerImpl javaScriptResponseHandler;
    private Request req;

    @Before
    public void setUp() {
        req = Mockito.mock(Request.class);
        javaScriptResponseHandler = new JavaScriptResponseHandlerImpl();
    }

    @Test
    public void executeJS_print_Test() throws ScriptException {

        final Object response = javaScriptResponseHandler.executeJS("print('Hello, World')");

        Assert.assertNull(response);
    }

    @Test
    public void executeJS_string_func_Test() throws ScriptException {

        final String helloFunction = "function helloMrSmith(name) {"
                + "return 'Hello Mr ' + name + ' Smith' "
                + "}";

        final Object response = javaScriptResponseHandler.executeJS(
                helloFunction
                + " helloMrSmith('James')");

        Assert.assertNotNull(response);
        Assert.assertTrue(response instanceof String);
        Assert.assertEquals("Hello Mr James Smith", response);
    }

    @Test
    public void executeJS_numeric_random_func_Test() throws ScriptException {

        final String randomFunction = "function getNumber() {"
                + "return Math.floor(Math.random() * 10) + 1;"
                + "}";

        final Object response = javaScriptResponseHandler.executeJS(
                randomFunction
                    + " getNumber()");

        Assert.assertNotNull(response);
        Assert.assertTrue(response instanceof Number);
    }

    @Test
    public void executeJS_mock_user_func_Test() throws ScriptException {

        final String userFunction = "function handleResponse(req, res) { "
                + " req.pathVars; "
                + " req.body; "
                + " req.headers; "
                + " req.parameters; "
                + " "
                + " res.body = 'coming soon'; "
                + " res.status = 202; "
                + " res.headers.a = 'aa'; "
                + " return res; "
                + "}";

        final Object response = javaScriptResponseHandler.executeJS(
                userFunction
                        + JavaScriptResponseHandler.defaultRequestObject
                        + JavaScriptResponseHandler.defaultResponseObject
                        + JavaScriptResponseHandler.userResponseFunctionInvoker);

        Assert.assertNotNull(response);
        Assert.assertTrue(response instanceof ScriptObjectMirror);

        Assert.assertEquals("coming soon", ((ScriptObjectMirror)response).get("body"));
        Assert.assertEquals(202, ((ScriptObjectMirror)response).get("status"));
        Assert.assertEquals("text/plain", ((ScriptObjectMirror)response).get("contentType"));

        Assert.assertEquals("aa", ((ScriptObjectMirror)((ScriptObjectMirror)response).get("headers")).get("a"));
        Assert.assertNull(((ScriptObjectMirror)((ScriptObjectMirror)response).get("headers")).get("b") );
    }

    @Test
    public void executeJS_empty_user_func_Test() throws ScriptException {

        final String userFunction = "function handleResponse(req, res) { "
                + " return res; "
                + "}";

        final Object response = javaScriptResponseHandler.executeJS(
                userFunction
                        + JavaScriptResponseHandler.defaultRequestObject
                        + JavaScriptResponseHandler.defaultResponseObject
                        + JavaScriptResponseHandler.userResponseFunctionInvoker);

        Assert.assertNotNull(response);
        Assert.assertTrue(response instanceof ScriptObjectMirror);

        Assert.assertNull(((ScriptObjectMirror)response).get("body"));
        Assert.assertEquals(404, ((ScriptObjectMirror)response).get("status"));
        Assert.assertEquals("text/plain", ((ScriptObjectMirror)response).get("contentType"));
    }

    @Test
    public void executeJS_missing_mock_user_func_Test() throws ScriptException {

        final Object response = javaScriptResponseHandler.executeJS(
                        JavaScriptResponseHandler.defaultRequestObject
                        + JavaScriptResponseHandler.defaultResponseObject
                        + JavaScriptResponseHandler.userResponseFunctionInvoker);

        Assert.assertNotNull(response);
        Assert.assertTrue(response instanceof ScriptObjectMirror);

        Assert.assertEquals("Expected handleResponse(request, response) function is undefined!", ((ScriptObjectMirror)response).get("body"));
        Assert.assertEquals(404, ((ScriptObjectMirror)response).get("status"));
    }

    @Test
    public void extractAllRequestParamsTest() {

        // Setup
        Mockito.when(req.contentType()).thenReturn(MediaType.APPLICATION_JSON_VALUE);
        Mockito.when(req.queryParams()).thenReturn(new HashSet<>(Arrays.asList("name", "age")));
        Mockito.when(req.queryParams("name")).thenReturn("joe");
        Mockito.when(req.queryParams("age")).thenReturn("35");

        // Test
        final Map<String, String> params = javaScriptResponseHandler.extractAllRequestParams(req);

        // Assertions
        Assert.assertNotNull(params);
        Assert.assertEquals(2, params.size());
        Assert.assertEquals("joe", params.get("name"));
        Assert.assertEquals("35", params.get("age"));
    }

    @Test
    public void extractAllRequestParams_formPost_Test() {

        // Setup
        Mockito.when(req.contentType()).thenReturn(MediaType.APPLICATION_FORM_URLENCODED_VALUE);
        Mockito.when(req.body()).thenReturn("name=jane;age=28;");

        // Test
        final Map<String, String> params = javaScriptResponseHandler.extractAllRequestParams(req);

        // Assertions
        Assert.assertNotNull(params);
        Assert.assertEquals(2, params.size());
        Assert.assertEquals("jane", params.get("name"));
        Assert.assertEquals("28", params.get("age"));
    }

    @Test
    public void extractAllRequestParams_formWithRandomReqBody_Test() {

        // Setup
        Mockito.when(req.contentType()).thenReturn(MediaType.APPLICATION_FORM_URLENCODED_VALUE);
        Mockito.when(req.body()).thenReturn("asdasdasdasd");

        // Test
        final Map<String, String> params = javaScriptResponseHandler.extractAllRequestParams(req);

        // Assertions
        Assert.assertNotNull(params);
        Assert.assertEquals(1, params.size());
        Assert.assertNull(params.get("asdasdasdasd"));
    }

    @Test
    public void extractAllRequestParams_formWithRandomReqBody2_Test() {

        // Setup
        Mockito.when(req.contentType()).thenReturn(MediaType.APPLICATION_FORM_URLENCODED_VALUE);
        Mockito.when(req.body()).thenReturn("a;b=;c;;");

        // Test
        final Map<String, String> params = javaScriptResponseHandler.extractAllRequestParams(req);

        // Assertions
        Assert.assertNotNull(params);
        Assert.assertEquals(3, params.size());
        Assert.assertNull(params.get("a"));
        Assert.assertEquals("", params.get("b"));
        Assert.assertNull(params.get("c"));
    }

    @Test
    public void extractAllRequestParams_formWithBlankReqBody_Test() {

        // Setup
        Mockito.when(req.contentType()).thenReturn(MediaType.APPLICATION_FORM_URLENCODED_VALUE);
        Mockito.when(req.body()).thenReturn(" ");

        // Test
        final Map<String, String> params = javaScriptResponseHandler.extractAllRequestParams(req);

        // Assertions
        Assert.assertNotNull(params);
        Assert.assertTrue(params.isEmpty());
    }

    @Test
    public void applyMapValuesToStringBuilderTest() {

        // Setup
        final Map<String, String> values = new HashMap<>();
        values.put("firstname", "Bob");
        values.put("lastname", "Bloggs");
        final StringBuilder reqObject = new StringBuilder();

        // Test
        javaScriptResponseHandler.applyMapValuesToStringBuilder("request.parameters", values, reqObject);

        // Assertions
        Assert.assertNotNull(reqObject);
        Assert.assertNotNull(reqObject.toString());
        Assert.assertEquals("request.parameters['firstname']='Bob'; request.parameters['lastname']='Bloggs';", reqObject.toString().trim());
    }

    @Test
    public void populateRequestObjectWithInboundTest() {

        // Setup
        Mockito.when(req.headers()).thenReturn(new HashSet<>(Arrays.asList("one", "two")));
        Mockito.when(req.headers("one")).thenReturn("1");
        Mockito.when(req.headers("two")).thenReturn("2");
        Mockito.when(req.pathInfo()).thenReturn("/hello/james");
        Mockito.when(req.body()).thenReturn("xxx");
        Mockito.when(req.queryParams()).thenReturn(new HashSet<>(Arrays.asList("name", "age")));
        Mockito.when(req.queryParams("name")).thenReturn("joe");
        Mockito.when(req.queryParams("age")).thenReturn("35");

        // Test
        final String result = javaScriptResponseHandler.populateRequestObjectWithInbound(req, "/hello/{name}");

        // Assertions
        Assert.assertNotNull(result);
        Assert.assertEquals("request.path='/hello/james'; request.body='xxx'; request.pathVars['name']='james'; request.parameters['name']='joe'; request.parameters['age']='35'; request.headers['one']='1'; request.headers['two']='2';", result.trim());
    }

}
