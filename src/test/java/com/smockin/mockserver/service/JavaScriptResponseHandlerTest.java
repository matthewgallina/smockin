package com.smockin.mockserver.service;

import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import javax.script.ScriptException;

public class JavaScriptResponseHandlerTest {

    private JavaScriptResponseHandlerImpl javaScriptResponseHandler;

    @Before
    public void setUp() {

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

}
