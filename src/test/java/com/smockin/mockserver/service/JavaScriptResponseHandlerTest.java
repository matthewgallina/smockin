package com.smockin.mockserver.service;

import com.smockin.admin.dto.UserKeyValueDataDTO;
import com.smockin.admin.enums.UserModeEnum;
import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.admin.persistence.entity.RestfulMockJavaScriptHandler;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.service.SmockinUserService;
import com.smockin.admin.service.UserKeyValueDataService;
import io.javalin.http.Context;
import jakarta.servlet.http.HttpServletRequest;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.openjdk.nashorn.api.scripting.ScriptObjectMirror;
import org.springframework.http.MediaType;

import javax.script.ScriptException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.Silent.class)
public class JavaScriptResponseHandlerTest {

    @Mock
    private SmockinUserService smockinUserService;

    @Mock
    private UserKeyValueDataService userKeyValueDataService;

    @Spy
    @InjectMocks
    private JavaScriptResponseHandlerImpl javaScriptResponseHandler = new JavaScriptResponseHandlerImpl();

    @Rule
    public ExpectedException expect = ExpectedException.none();

    @Mock
    private Context ctx;

    @Mock
    private HttpServletRequest req;

    @Before
    public void setUp() {
        Mockito.when(ctx.req()).thenReturn(req);
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
        Mockito.when(ctx.contentType()).thenReturn(MediaType.APPLICATION_JSON_VALUE);
        final Map<String, List<String>> queryParams = new HashMap<>();
        queryParams.put("name", Arrays.asList("joe"));
        queryParams.put("age", Arrays.asList("35"));
        Mockito.when(ctx.queryParamMap()).thenReturn(queryParams);

        // Test
        final Map<String, String> params = javaScriptResponseHandler.extractAllRequestParams(ctx);

        // Assertions
        Assert.assertNotNull(params);
        Assert.assertEquals(2, params.size());
        Assert.assertEquals("joe", params.get("name"));
        Assert.assertEquals("35", params.get("age"));
    }

    @Test
    public void extractAllRequestParams_formPost_Test() {

        // Setup
        Mockito.when(ctx.contentType()).thenReturn(MediaType.APPLICATION_FORM_URLENCODED_VALUE);
        Mockito.when(ctx.body()).thenReturn("name=jane&age=28");

        // Test
        final Map<String, String> params = javaScriptResponseHandler.extractAllRequestParams(ctx);

        // Assertions
        Assert.assertNotNull(params);
        Assert.assertEquals(2, params.size());
        Assert.assertEquals("jane", params.get("name"));
        Assert.assertEquals("28", params.get("age"));
    }

    @Test
    public void extractAllRequestParams_formWithRandomReqBody_Test() {

        // Setup
        Mockito.when(ctx.contentType()).thenReturn(MediaType.APPLICATION_FORM_URLENCODED_VALUE);
        Mockito.when(ctx.body()).thenReturn("asdasdasdasd");

        // Test
        final Map<String, String> params = javaScriptResponseHandler.extractAllRequestParams(ctx);

        // Assertions
        Assert.assertNotNull(params);
        Assert.assertEquals(1, params.size());
        Assert.assertNull(params.get("asdasdasdasd"));
    }

    @Test
    public void extractAllRequestParams_formWithRandomReqBody2_Test() {

        // Setup
        Mockito.when(ctx.contentType()).thenReturn(MediaType.APPLICATION_FORM_URLENCODED_VALUE);
        Mockito.when(ctx.body()).thenReturn("a&b=&c&&");

        // Test
        final Map<String, String> params = javaScriptResponseHandler.extractAllRequestParams(ctx);

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
        Mockito.when(ctx.contentType()).thenReturn(MediaType.APPLICATION_FORM_URLENCODED_VALUE);
        Mockito.when(ctx.body()).thenReturn(" ");

        // Test
        final Map<String, String> params = javaScriptResponseHandler.extractAllRequestParams(ctx);

        // Assertions
        Assert.assertNotNull(params);
        Assert.assertTrue(params.isEmpty());
    }

    @Test
    public void applyMapValuesToStringBuilderTest() {

        // Setup
        final Map<String, String> values = new LinkedHashMap<>();
        values.put("firstname", "Bob");
        values.put("lastname", "Bloggs");
        final StringBuilder reqObject = new StringBuilder();

        // Test
        javaScriptResponseHandler.applyMapValuesToStringBuilder("request.parameters", values, reqObject);

        // Assertions
        Assert.assertNotNull(reqObject);
        Assert.assertNotNull(reqObject.toString());
        Assert.assertEquals(" request.parameters['firstname']='Bob'; request.parameters['lastname']='Bloggs';", reqObject.toString());
    }

    @Test
    public void populateRequestObjectWithInboundTest() {

        // Setup
        Mockito.when(req.getHeaderNames()).thenAnswer(inv -> Collections.enumeration(Arrays.asList("one", "two")));
        Mockito.when(req.getHeader("one")).thenReturn("1");
        Mockito.when(req.getHeader("two")).thenReturn("2");
        Mockito.when(ctx.path()).thenReturn("/hello/james");
        Mockito.when(ctx.body()).thenReturn("xxx");
        final Map<String, List<String>> queryParams = new HashMap<>();
        queryParams.put("name", Arrays.asList("joe"));
        queryParams.put("age", Arrays.asList("35"));
        Mockito.when(ctx.queryParamMap()).thenReturn(queryParams);
        Mockito.when(ctx.contentType()).thenReturn(MediaType.APPLICATION_JSON_VALUE);

        Mockito.when(smockinUserService.getUserMode()).thenReturn(UserModeEnum.INACTIVE);

        // Test
        final String result = javaScriptResponseHandler.populateRequestObjectWithInbound(ctx, "/hello/{name}", "");

        // Assertions
        Assert.assertNotNull(result);
        Assert.assertTrue(result.contains("request.path='/hello/james';"));
        Assert.assertTrue(result.contains("request.body='xxx';"));
        Assert.assertTrue(result.contains("request.pathVars['name']='james';"));
    }

    @Test
    public void populateRequestObjectWithInbound_multiUserCtx_Test() {

        // Setup
        Mockito.when(req.getHeaderNames()).thenAnswer(inv -> Collections.enumeration(Arrays.asList("one", "two")));
        Mockito.when(req.getHeader("one")).thenReturn("1");
        Mockito.when(req.getHeader("two")).thenReturn("2");
        Mockito.when(ctx.path()).thenReturn("/bob/hello/james");
        Mockito.when(ctx.body()).thenReturn("xxx");
        final Map<String, List<String>> queryParams = new HashMap<>();
        queryParams.put("name", Arrays.asList("joe"));
        queryParams.put("age", Arrays.asList("35"));
        Mockito.when(ctx.queryParamMap()).thenReturn(queryParams);
        Mockito.when(ctx.contentType()).thenReturn(MediaType.APPLICATION_JSON_VALUE);

        Mockito.when(smockinUserService.getUserMode()).thenReturn(UserModeEnum.ACTIVE);

        // Test
        final String result = javaScriptResponseHandler.populateRequestObjectWithInbound(ctx, "/hello/{name}", "/bob");

        // Assertions
        Assert.assertNotNull(result);
        Assert.assertTrue(result.contains("request.path='/bob/hello/james';"));
        Assert.assertTrue(result.contains("request.body='xxx';"));
        Assert.assertTrue(result.contains("request.pathVars['name']='james';"));
    }

    @Test
    public void executeJS_runJavaSecurityAttempt1_Test() throws ScriptException {

        expect.expect(ScriptException.class);
        expect.expectMessage(Matchers.containsString("java"));

        javaScriptResponseHandler.executeJS("java.lang.System.nanoTime();");
    }

    @Test
    public void executeJS_runJavaSecurityAttempt2_Test() throws ScriptException {

        expect.expect(ScriptException.class);
        expect.expectMessage(Matchers.containsString("java"));

        javaScriptResponseHandler.executeJS("java.lang.Runtime.getRuntime().exec(\"java.lang.System.nanoTime();\");");
    }

    @Test
    public void executeJS_runJavaSecurityAttempt3_Exit_Test() throws ScriptException {

        expect.expect(ScriptException.class);
        expect.expectMessage(Matchers.containsString("exit"));

        javaScriptResponseHandler.executeJS("exit(1);");
    }

    @Test
    public void executeJS_runJavaSecurityAttempt4_Test() throws ScriptException {

        expect.expect(ScriptException.class);

        javaScriptResponseHandler.executeJS("java.lang.System.setProperty(\"a\", \"b\");");
    }

    @Test
    public void executeJS_runJavaSecurityAttempt5_Test() throws ScriptException {

        expect.expect(ScriptException.class);

        javaScriptResponseHandler.executeJS("new java.lang.ProcessBuilder().command(\"java.lang.System.nanoTime();\")");
    }

    @Test
    public void executeJS_runJavaSecurityAttempt6_Test() throws ScriptException {

        expect.expect(ScriptException.class);

        javaScriptResponseHandler.executeJS("java.io.File.createTempFile(\"hack\", \".sh\")");
    }

    @Test
    public void populateKVPs_noKvpsPresent_Test() throws ScriptException {

        // Setup
        final String userFunc = "function handleResponse(request, response) { "
                + ""
                + "response.body = \"Hello Bob. How are you on this sunny day?\";"
                + "}";

        final RestfulMock mock = new RestfulMock();
        final SmockinUser smockinUser = new SmockinUser();
        smockinUser.setCtxPath("");
        smockinUser.setId(1);
        mock.setCreatedBy(smockinUser);
        final RestfulMockJavaScriptHandler javaScriptHandler = new RestfulMockJavaScriptHandler();
        javaScriptHandler.setRestfulMock(mock);
        javaScriptHandler.setSyntax(userFunc);
        mock.setJavaScriptHandler(javaScriptHandler);

        // Test
        final String result = javaScriptResponseHandler.populateKVPs(ctx, mock);

        // Assertions
        Assert.assertNotNull(result);
        Assert.assertEquals(javaScriptResponseHandler.defaultKeyValuePairStoreObject, result);
    }

    @Test
    public void populateKVPs_fixedVars_Test() throws ScriptException {

        // Setup
        final String userFunc = "function handleResponse(request, response) { "
                + ""
                + "response.body = \"Hello\" + lookUpKvp('foo') + \". How are you on this\" + lookUpKvp('weather') + \"day?\";"
                + "}";

        final RestfulMock mock = new RestfulMock();
        final SmockinUser smockinUser = new SmockinUser();
        smockinUser.setCtxPath("");
        smockinUser.setId(1);
        mock.setCreatedBy(smockinUser);
        final RestfulMockJavaScriptHandler javaScriptHandler = new RestfulMockJavaScriptHandler();
        javaScriptHandler.setRestfulMock(mock);
        javaScriptHandler.setSyntax(userFunc);
        mock.setJavaScriptHandler(javaScriptHandler);

        // Mock
        final UserKeyValueDataDTO userKeyValueDataDTO = new UserKeyValueDataDTO();
        userKeyValueDataDTO.setValue("XXX");
        Mockito.when(userKeyValueDataService.loadByKey(Mockito.anyString(), Mockito.anyLong())).thenReturn(userKeyValueDataDTO);

        // Test
        final String result = javaScriptResponseHandler.populateKVPs(ctx, mock);

        // Assertions
        Assert.assertNotNull(result);
        Assert.assertTrue(result.contains("\"foo\":\"XXX\""));
        Assert.assertTrue(result.contains("\"weather\":\"XXX\""));
    }

    @Test
    public void populateKVPs_requestBodyInput_Test() throws ScriptException {

        // Setup
        final String userFunc = "function handleResponse(request, response) { "
                + "var name = lookUpKvp(request.body);"
                + "var weather = lookUpKvp('weather');"
                + "response.body = \"Hello\" + name + \". How are you on this\" + weather + \"day?\";"
                + "}";

        final RestfulMock mock = new RestfulMock();
        final SmockinUser smockinUser = new SmockinUser();
        smockinUser.setCtxPath("");
        smockinUser.setId(1);
        mock.setCreatedBy(smockinUser);
        final RestfulMockJavaScriptHandler javaScriptHandler = new RestfulMockJavaScriptHandler();
        javaScriptHandler.setRestfulMock(mock);
        javaScriptHandler.setSyntax(userFunc);
        mock.setJavaScriptHandler(javaScriptHandler);

        // Mock
        final UserKeyValueDataDTO userKeyValueDataDTO = new UserKeyValueDataDTO();
        userKeyValueDataDTO.setValue("XXX");
        Mockito.when(userKeyValueDataService.loadByKey(Mockito.anyString(), Mockito.anyLong())).thenReturn(userKeyValueDataDTO);
        Mockito.when(ctx.body()).thenReturn("hello");

        // Test
        final String result = javaScriptResponseHandler.populateKVPs(ctx, mock);

        // Assertions
        Assert.assertNotNull(result);
        Assert.assertTrue(result.contains("\"hello\":\"XXX\""));
        Assert.assertTrue(result.contains("\"weather\":\"XXX\""));
    }

    @Test
    public void populateKVPs_requestPathVarsInput_Test() throws ScriptException {

        // Setup
        final String userFunc = "function handleResponse(request, response) { "
                + "var kvpArray = { name : lookUpKvp(request.pathVars.firstName), weather : lookUpKvp('weather') };"
                + "response.body = \"Hello\" + kvpArray.name + \". How are you on this\" + kvpArray.weather + \"day?\";"
                + "}";

        final RestfulMock mock = new RestfulMock();
        final SmockinUser smockinUser = new SmockinUser();
        smockinUser.setCtxPath("");
        smockinUser.setId(1);
        mock.setCreatedBy(smockinUser);
        final RestfulMockJavaScriptHandler javaScriptHandler = new RestfulMockJavaScriptHandler();
        javaScriptHandler.setRestfulMock(mock);
        javaScriptHandler.setSyntax(userFunc);
        mock.setJavaScriptHandler(javaScriptHandler);
        mock.setPath("/hello/{firstName}");

        // Mock
        final UserKeyValueDataDTO userKeyValueDataDTO = new UserKeyValueDataDTO();
        userKeyValueDataDTO.setValue("XXX");
        Mockito.when(userKeyValueDataService.loadByKey(Mockito.anyString(), Mockito.anyLong())).thenReturn(userKeyValueDataDTO);
        Mockito.when(ctx.path()).thenReturn("/hello/bob");

        // Test
        final String result = javaScriptResponseHandler.populateKVPs(ctx, mock);

        // Assertions
        Assert.assertNotNull(result);
        Assert.assertTrue(result.contains("\"bob\":\"XXX\""));
        Assert.assertTrue(result.contains("\"weather\":\"XXX\""));
    }

    @Test
    public void populateKVPs_requestParametersInput_Test() throws ScriptException {

        // Setup
        final String userFunc = "function handleResponse(request, response) { "
                + "var getName = lookUpKvp(request.parameters['my-first-name']);"
                + "var getWeather = ( lookUpKvp('weather') );"
                + "var weather = KVP.weather;"
                + "response.body = \"Hello\" + getName + \". How are you on this\" + getWeather + \"day?\";"
                + "}";

        final RestfulMock mock = new RestfulMock();
        final SmockinUser smockinUser = new SmockinUser();
        smockinUser.setCtxPath("");
        smockinUser.setId(1);
        mock.setCreatedBy(smockinUser);
        final RestfulMockJavaScriptHandler javaScriptHandler = new RestfulMockJavaScriptHandler();
        javaScriptHandler.setRestfulMock(mock);
        javaScriptHandler.setSyntax(userFunc);
        mock.setJavaScriptHandler(javaScriptHandler);

        // Mock
        final UserKeyValueDataDTO userKeyValueDataDTO = new UserKeyValueDataDTO();
        userKeyValueDataDTO.setValue("XXX");
        Mockito.when(userKeyValueDataService.loadByKey(Mockito.anyString(), Mockito.anyLong())).thenReturn(userKeyValueDataDTO);
        final Map<String, List<String>> queryParams = new HashMap<>();
        queryParams.put("my-first-name", Arrays.asList("Harry"));
        Mockito.when(ctx.queryParamMap()).thenReturn(queryParams);
        Mockito.when(ctx.contentType()).thenReturn(MediaType.APPLICATION_JSON_VALUE);

        // Test
        final String result = javaScriptResponseHandler.populateKVPs(ctx, mock);

        // Assertions
        Assert.assertNotNull(result);
        Assert.assertTrue(result.contains("\"Harry\":\"XXX\""));
        Assert.assertTrue(result.contains("\"weather\":\"XXX\""));
    }

    @Test
    public void populateKVPs_requestHeadersInput_Test() throws ScriptException {

        // Setup
        final String userFunc = "function handleResponse(request, response) { "
                + "var getName = lookUpKvp(request.headers.myLastName);"
                + "var getWeather = ( lookUpKvp('weather') );"
                + "var weather = KVP.weather;"
                + "response.body = \"Hello\" + getName + \". How are you on this\" + getWeather + \"day?\";"
                + "}";

        final RestfulMock mock = new RestfulMock();
        final SmockinUser smockinUser = new SmockinUser();
        smockinUser.setCtxPath("");
        smockinUser.setId(1);
        mock.setCreatedBy(smockinUser);
        final RestfulMockJavaScriptHandler javaScriptHandler = new RestfulMockJavaScriptHandler();
        javaScriptHandler.setRestfulMock(mock);
        javaScriptHandler.setSyntax(userFunc);
        mock.setJavaScriptHandler(javaScriptHandler);

        // Mock
        final UserKeyValueDataDTO userKeyValueDataDTO = new UserKeyValueDataDTO();
        userKeyValueDataDTO.setValue("XXX");
        Mockito.when(userKeyValueDataService.loadByKey(Mockito.anyString(), Mockito.anyLong())).thenReturn(userKeyValueDataDTO);
        Mockito.when(req.getHeaderNames()).thenAnswer(inv -> Collections.enumeration(Arrays.asList("myLastName")));
        Mockito.when(req.getHeader(Mockito.anyString())).thenReturn("Potter");

        // Test
        final String result = javaScriptResponseHandler.populateKVPs(ctx, mock);

        // Assertions
        Assert.assertNotNull(result);
        Assert.assertTrue(result.contains("\"Potter\":\"XXX\"") || result.contains("\"weather\":\"XXX\""));
    }

    @Test
    public void populateKVPs_kvpNotFound_Test() throws ScriptException {

        // Setup
        final String userFunc = "function handleResponse(request, response) { "
                + ""
                + "response.body = \"Hello\" + lookUpKvp('foo') + \". How are you on this\" + lookUpKvp('weather') + \"day?\";"
                + "}";

        final RestfulMock mock = new RestfulMock();
        final SmockinUser smockinUser = new SmockinUser();
        smockinUser.setCtxPath("");
        smockinUser.setId(1);
        mock.setCreatedBy(smockinUser);
        final RestfulMockJavaScriptHandler javaScriptHandler = new RestfulMockJavaScriptHandler();
        javaScriptHandler.setRestfulMock(mock);
        javaScriptHandler.setSyntax(userFunc);
        mock.setJavaScriptHandler(javaScriptHandler);

        // Test
        final String result = javaScriptResponseHandler.populateKVPs(ctx, mock);

        // Assertions
        Assert.assertNotNull(result);
        Assert.assertTrue(result.contains("\"foo\":\"\""));
        Assert.assertTrue(result.contains("\"weather\":\"\""));

    }

    @Test
    public void populateKVPs_invalidKvpSyntax1_Test() throws ScriptException {

        // Assertions
        expect.expect(ScriptException.class);
        expect.expectMessage(Matchers.is("Invalid lookUpKvp(...) syntax. Unable to determine key lookup type"));

        // Setup
        final String userFunc = "function handleResponse(request, response) { "
                + ""
                + "response.body = \"Hello\" + lookUpKvp( + \". How are you on this\" + lookUpKvp() + \"day?\";"
                + "}";

        final RestfulMock mock = new RestfulMock();
        final SmockinUser smockinUser = new SmockinUser();
        smockinUser.setCtxPath("");
        smockinUser.setId(1);
        mock.setCreatedBy(smockinUser);
        final RestfulMockJavaScriptHandler javaScriptHandler = new RestfulMockJavaScriptHandler();
        javaScriptHandler.setRestfulMock(mock);
        javaScriptHandler.setSyntax(userFunc);
        mock.setJavaScriptHandler(javaScriptHandler);

        // Test
        javaScriptResponseHandler.populateKVPs(ctx, mock);
    }

    @Test
    public void populateKVPs_invalidKvpSyntax2_Test() throws ScriptException {

        // Assertions
        expect.expect(ScriptException.class);
        expect.expectMessage(Matchers.is("Invalid lookUpKvp(...) syntax. key within find parenthesis is undefined"));

        // Setup
        final String userFunc = "function handleResponse(request, response) { "
                + ""
                + "response.body = \"Hello\" + lookUpKvp() + \". How are you on this\" + lookUpKvp('foo') + \"day?\";"
                + "}";

        final RestfulMock mock = new RestfulMock();
        final SmockinUser smockinUser = new SmockinUser();
        smockinUser.setCtxPath("");
        smockinUser.setId(1);
        mock.setCreatedBy(smockinUser);
        final RestfulMockJavaScriptHandler javaScriptHandler = new RestfulMockJavaScriptHandler();
        javaScriptHandler.setRestfulMock(mock);
        javaScriptHandler.setSyntax(userFunc);
        mock.setJavaScriptHandler(javaScriptHandler);

        // Test
        javaScriptResponseHandler.populateKVPs(ctx, mock);

    }

    @Test
    public void removeLineBreaks_windows_Test() {

        // Setup
        final String input = "Hello\r\nWorld";

        // Test
        final String result = javaScriptResponseHandler.removeLineBreaks(input);

        // Assertions
        Assert.assertNotNull(result);
        Assert.assertEquals("HelloWorld", result);
    }

    @Test
    public void removeLineBreaks_linux_Test() {

        // Setup
        final String input = "Hello\nWorld";

        // Test
        final String result = javaScriptResponseHandler.removeLineBreaks(input);

        // Assertions
        Assert.assertNotNull(result);
        Assert.assertEquals("HelloWorld", result);
    }

}
