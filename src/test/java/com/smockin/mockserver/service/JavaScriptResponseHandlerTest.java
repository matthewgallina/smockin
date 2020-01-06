package com.smockin.mockserver.service;

import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.script.ScriptException;

public class JavaScriptResponseHandlerTest {

    private JavaScriptResponseHandler JavaScriptResponseHandler;

    @Before
    public void setUp() {

        JavaScriptResponseHandler = new JavaScriptResponseHandlerImpl();
    }

    @Test
    public void execute_print_Test() throws ScriptException {

        final Object response = JavaScriptResponseHandler.execute("print('Hello, World')");

        Assert.assertNull(response);
    }

    @Test
    public void execute_string_func_Test() throws ScriptException {

        final String helloFunction = "function helloMrSmith(name) {"
                + "return 'Hello Mr ' + name + ' Smith' "
                + "}";

        final Object response = JavaScriptResponseHandler.execute(
                helloFunction
                + " helloMrSmith('James')");

        Assert.assertNotNull(response);
        Assert.assertTrue(response instanceof String);
        Assert.assertEquals("Hello Mr James Smith", response);
    }

    @Test
    public void execute_numeric_random_func_Test() throws ScriptException {

        final String randomFunction = "function getNumber() {"
                + "return Math.floor(Math.random() * 10) + 1;"
                + "}";

        final Object response = JavaScriptResponseHandler.execute(
                randomFunction
                    + " getNumber()");

        Assert.assertNotNull(response);
        Assert.assertTrue(response instanceof Number);
    }

    @Test
    public void execute_mock_user_func_Test() throws ScriptException {

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

        final Object response = JavaScriptResponseHandler.execute(
                userFunction
                        + JavaScriptResponseHandler.handleResponseCaller);

        Assert.assertNotNull(response);
        Assert.assertTrue(response instanceof ScriptObjectMirror);

        Assert.assertEquals("coming soon", ((ScriptObjectMirror)response).get("body"));
        Assert.assertEquals(202, ((ScriptObjectMirror)response).get("status"));
        Assert.assertEquals("text/plain", ((ScriptObjectMirror)response).get("contentType"));
        Assert.assertEquals("aa", ((ScriptObjectMirror)((ScriptObjectMirror)response).get("headers")).get("a"));
        Assert.assertNull(((ScriptObjectMirror)((ScriptObjectMirror)response).get("headers")).get("b") );
    }

    @Test
    public void execute_missing_mock_user_func_Test() throws ScriptException {

        final Object response = JavaScriptResponseHandler
                .execute(JavaScriptResponseHandler.handleResponseCaller);

        Assert.assertNotNull(response);
        Assert.assertTrue(response instanceof ScriptObjectMirror);

        Assert.assertEquals("mock js logic is undefined!", ((ScriptObjectMirror)response).get("body"));
        Assert.assertEquals(404, ((ScriptObjectMirror)response).get("status"));
    }

}
