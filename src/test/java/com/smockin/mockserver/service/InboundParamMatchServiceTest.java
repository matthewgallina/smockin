package com.smockin.mockserver.service;

import com.smockin.admin.dto.UserKeyValueDataDTO;
import com.smockin.admin.enums.UserModeEnum;
import com.smockin.admin.service.SmockinUserService;
import com.smockin.admin.service.UserKeyValueDataService;
import com.smockin.mockserver.exception.InboundParamMatchException;
import com.smockin.mockserver.service.enums.ParamMatchTypeEnum;
import com.smockin.utils.GeneralUtils;
import io.javalin.http.Context;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.math.NumberUtils;
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
import org.springframework.http.HttpMethod;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

/**
 * Created by mgallina.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class InboundParamMatchServiceTest {

    private Context ctx;
    private HttpServletRequest request;
    private String sanitizedUserCtxInboundPath;
    private long userId;

    @Mock
    private SmockinUserService smockinUserService;

    @Mock
    private UserKeyValueDataService userKeyValueDataService;

    @Spy
    @InjectMocks
    private InboundParamMatchServiceImpl inboundParamMatchServiceImpl = new InboundParamMatchServiceImpl();


    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        sanitizedUserCtxInboundPath = "";
        userId = 1;
        ctx = Mockito.mock(Context.class);
        request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(ctx.req()).thenReturn(request);
    }

    @Test
    public void processParamMatch_NoToken_Test()  {
        Assert.assertNull(inboundParamMatchServiceImpl.processParamMatch(ctx, "/person/{name}","Hello World", sanitizedUserCtxInboundPath, userId));
    }

    @Test
    public void processParamMatch_InvalidToken_Test()  {

        final String responseBody = "Hello " + ParamMatchTypeEnum.PARAM_PREFIX + "Foo";
        Assert.assertNull(inboundParamMatchServiceImpl.processParamMatch(ctx, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId));
    }

    @Test
    public void processParamMatch_InvalidTokenWithBrackets_Test()  {

        final String responseBody = "Hello " + ParamMatchTypeEnum.PARAM_PREFIX + "Foo()";
        Assert.assertNull(inboundParamMatchServiceImpl.processParamMatch(ctx, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId));
    }

    @Test
    public void processParamMatch_Empty_Test()  {

        final String responseBody = "Hello " + ParamMatchTypeEnum.PARAM_PREFIX + "(  )";
        Assert.assertNull(inboundParamMatchServiceImpl.processParamMatch(ctx, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId));
    }

    @Test
    public void processParamMatch_Blank_Test()  {

        final String responseBody = "Hello " + ParamMatchTypeEnum.PARAM_PREFIX + "()";
        Assert.assertNull(inboundParamMatchServiceImpl.processParamMatch(ctx, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId));
    }

    @Test
    public void processParamMatch_header_Test()  {

        final String responseBody = "Hello " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.requestHeader.name() +"(name)";

        Mockito.when(request.getHeader("name")).thenReturn("Roger");
        Mockito.when(request.getHeaderNames()).thenReturn(Collections.enumeration(Arrays.asList("name")));

        final String result = inboundParamMatchServiceImpl.processParamMatch(ctx, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId);

        Assert.assertEquals("Hello Roger", result);
    }

    @Test
    public void processParamMatch_headerCase_Test()  {

        final String responseBody = "Hello " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.requestHeader.name() +"(NAME)";

        Mockito.when(request.getHeader("name")).thenReturn("Roger");
        Mockito.when(request.getHeaderNames()).thenReturn(Collections.enumeration(Arrays.asList("name")));

        final String result = inboundParamMatchServiceImpl.processParamMatch(ctx, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId);

        Assert.assertEquals("Hello Roger", result);
    }

    @Test
    public void processParamMatch_headerNoMatch_Test()  {

        final String responseBody = "Hello " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.requestHeader.name() +"(name)";
        Mockito.when(request.getHeaderNames()).thenReturn(Collections.enumeration(Collections.emptyList()));
        final String result = inboundParamMatchServiceImpl.processParamMatch(ctx, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId);

        Assert.assertEquals("Hello ", result);
    }

    @Test
    public void processParamMatch_reqParam_Test()  {

        final String responseBody = "Hello " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.requestParameter.name() +"(name)";

        Mockito.when(request.getMethod()).thenReturn(HttpMethod.GET.name());
        Map<String, String[]> params = new HashMap<>();
        params.put("name", new String[]{"Roger"});
        Mockito.when(request.getParameterMap()).thenReturn(params);
        // Mock queryParamMap to be non-empty so the GET path is taken
        Map<String, List<String>> queryParams = new HashMap<>();
        queryParams.put("name", Arrays.asList("Roger"));
        Mockito.when(ctx.queryParamMap()).thenReturn(queryParams);

        final String result = inboundParamMatchServiceImpl.processParamMatch(ctx, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId);

        Assert.assertEquals("Hello Roger", result);
    }

    @Test
    public void processParamMatch_reqParamCase_Test()  {

        final String responseBody = "Hello " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.requestParameter.name() +"(NAME)";

        Mockito.when(request.getMethod()).thenReturn(HttpMethod.GET.name());
        Map<String, String[]> params = new HashMap<>();
        params.put("name", new String[]{"Roger"});
        Mockito.when(request.getParameterMap()).thenReturn(params);
        // Mock queryParamMap to be non-empty so the GET path is taken
        Map<String, List<String>> queryParams = new HashMap<>();
        queryParams.put("name", Arrays.asList("Roger"));
        Mockito.when(ctx.queryParamMap()).thenReturn(queryParams);

        final String result = inboundParamMatchServiceImpl.processParamMatch(ctx, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId);

        Assert.assertEquals("Hello Roger", result);
    }

    @Test
    public void processParamMatch_reqParamNoMatch_Test()  {

        final String responseBody = "Hello " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.requestParameter.name() +"(name)";
        Mockito.when(request.getMethod()).thenReturn(HttpMethod.GET.name());
        Mockito.when(request.getParameterMap()).thenReturn(new HashMap<>());

        final String result = inboundParamMatchServiceImpl.processParamMatch(ctx, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId);

        Assert.assertEquals("Hello ", result);
    }

    @Test
    public void processParamMatch_pathVar_Test()  {

        final String responseBody = "Hello " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.pathVar.name() +"(name)";
        sanitizedUserCtxInboundPath = "/person/Roger";

        final String result = inboundParamMatchServiceImpl.processParamMatch(ctx, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId);

        Assert.assertEquals("Hello Roger", result);
    }

    @Test
    public void processParamMatch_pathVarCase_Test()  {

        final String responseBody = "Hello " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.pathVar.name() +"(NAME)";
        sanitizedUserCtxInboundPath = "/person/Roger";

        final String result = inboundParamMatchServiceImpl.processParamMatch(ctx, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId);

        Assert.assertEquals("Hello Roger", result);
    }

    @Test
    public void processParamMatch_pathVarNoMatch_Test()  {

        final String responseBody = "Hello " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.pathVar.name() +"(name)";
        final String result = inboundParamMatchServiceImpl.processParamMatch(ctx, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId);

        Assert.assertEquals("Hello ", result);
    }

    @Test
    public void enrichWithInboundParamMatches_multiMatchesAndSpaces_Test() throws InboundParamMatchException {
        final String responseBody = "Hello " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.requestHeader.name() +"('name'), you are " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.requestHeader.name() +"(GenDer) and are "  + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.requestHeader.name() +"(\"age\") years old";

        Mockito.when(request.getPathInfo()).thenReturn("/person/test");
        Mockito.when(request.getHeaderNames()).thenAnswer(inv -> Collections.enumeration(Arrays.asList("name", "age", "gender")));
        Mockito.when(request.getHeader("name")).thenReturn("Roger");
        Mockito.when(request.getHeader("age")).thenReturn("21");
        Mockito.when(request.getHeader("gender")).thenReturn("Male");
        Mockito.when(smockinUserService.getUserMode()).thenReturn(UserModeEnum.INACTIVE);

        final String result = inboundParamMatchServiceImpl.enrichWithInboundParamMatches(ctx, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId);

        Assert.assertEquals("Hello Roger, you are Male and are 21 years old", result);
    }

    @Test
    public void enrichWithInboundParamMatches_partialMatch_Test() throws InboundParamMatchException {
        final String responseBody = "Hello " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.requestHeader.name() +"(name), you are " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.requestHeader.name() +"(age) years old";

        Mockito.when(request.getPathInfo()).thenReturn("/person/test");
        Mockito.when(request.getHeaderNames()).thenAnswer(inv -> Collections.enumeration(Arrays.asList("name")));
        Mockito.when(request.getHeader("name")).thenReturn("Roger");

        final String result = inboundParamMatchServiceImpl.enrichWithInboundParamMatches(ctx, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId);

        Assert.assertEquals("Hello Roger, you are  years old", result);
    }

    @Test
    public void enrichWithInboundParamMatches_withNoMadeUpToken_Test() throws InboundParamMatchException {
        final String responseBody = "Hello " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.requestHeader.name() +"(name), you are " + ParamMatchTypeEnum.PARAM_PREFIX + "FOO(age) years old";

        Mockito.when(request.getPathInfo()).thenReturn("/person/test");
        Mockito.when(request.getHeaderNames()).thenAnswer(inv -> Collections.enumeration(Arrays.asList("name")));
        Mockito.when(request.getHeader("name")).thenReturn("Roger");

        final String result = inboundParamMatchServiceImpl.enrichWithInboundParamMatches(ctx, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId);

        Assert.assertNotNull(result);
        Assert.assertEquals("Hello Roger, you are " + ParamMatchTypeEnum.PARAM_PREFIX + "FOO(age) years old", result);
    }

    @Test
    public void processParamMatch_isoDate_Test()  {

        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        final String responseBody = "The date is " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.isoDate.name();

        final String result = inboundParamMatchServiceImpl.processParamMatch(ctx, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId);

        final String remainder = result.replaceAll("The date is ", "");

        try {
            Assert.assertNotNull(new SimpleDateFormat(GeneralUtils.ISO_DATE_FORMAT).parse(remainder));
        } catch (Throwable ex) {
            Assert.fail();
        }
    }

    @Test
    public void processParamMatch_isoDateTime_Test()  {

        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        final String responseBody = "The date and time is " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.isoDatetime.name();

        final String result = inboundParamMatchServiceImpl.processParamMatch(ctx, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId);

        final String remainder = result.replaceAll("The date and time is ", "");

        try {
            Assert.assertNotNull(new SimpleDateFormat(GeneralUtils.ISO_DATETIME_FORMAT).parse(remainder));
        } catch (Throwable ex) {
            Assert.fail();
        }
    }

    @Test
    public void processParamMatch_uuid_Test()  {

        final String responseBody = "Your ID is " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.uuid.name();

        final String result = inboundParamMatchServiceImpl.processParamMatch(ctx, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId);

        final String remainder = result.replaceAll("Your ID is ", "");

        try {
            Assert.assertNotNull(UUID.fromString(remainder));
        } catch (Throwable ex) {
            Assert.fail();
        }
    }

    @Test
    public void processParamMatch_randomNumber_Test()  {

        final String responseBody = "Your number is " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.randomNumber.name() + "(1,3)";

        final String result = inboundParamMatchServiceImpl.processParamMatch(ctx, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId);

        final String remainder = result.replaceAll("Your number is ", "");
        Assert.assertTrue(NumberUtils.isDigits(remainder));
        Assert.assertTrue((Integer.valueOf(remainder) == 1) || (Integer.valueOf(remainder) == 2) || (Integer.valueOf(remainder) == 3));
    }

    @Test
    public void processParamMatch_randomNumberZero_Test()  {

        final String responseBody = "Your number is " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.randomNumber.name() + "(0,0)";

        final String result = inboundParamMatchServiceImpl.processParamMatch(ctx, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId);

        final String remainder = result.replaceAll("Your number is ", "");
        Assert.assertTrue(NumberUtils.isDigits(remainder));
        Assert.assertEquals(Integer.valueOf(0), Integer.valueOf(remainder));
    }

    @Test
    public void processParamMatch_randomNumberNoParams_Test()  {

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("randomNumber is missing args");

        final String responseBody = "Your number is " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.randomNumber.name() + "()";

        inboundParamMatchServiceImpl.processParamMatch(ctx, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId);
    }

    @Test
    public void processParamMatch_kvpMatch_Test()  {

        final String responseBody = "I say " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.lookUpKvp +"(Hello)";

        Mockito.when(userKeyValueDataService.loadByKey(Mockito.anyString(), Mockito.anyLong()))
            .thenReturn(new UserKeyValueDataDTO(GeneralUtils.generateUUID(), "Hello", "Bonjour"));

        final String result = inboundParamMatchServiceImpl.processParamMatch(ctx, "/person", responseBody, sanitizedUserCtxInboundPath, userId);

        Assert.assertEquals("I say Bonjour", result);
    }

    @Test
    public void processParamMatch_kvpNoMatch_Test()  {

        final String responseBody = "I say "+ ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.lookUpKvp +"(Hello)";

        Mockito.when(userKeyValueDataService.loadByKey(Mockito.anyString(), Mockito.anyLong()))
            .thenReturn(null);

        final String result = inboundParamMatchServiceImpl.processParamMatch(ctx, "/person", responseBody, sanitizedUserCtxInboundPath, userId);

        Assert.assertEquals("I say ", result);
    }

    @Test
    public void processParamMatch_kvpNestedRequestBodyMatch_Test() {

        final String responseBody = "I say " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.lookUpKvp +"(" + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.requestBody + ")";

        Mockito.when(ctx.body()).thenReturn("greeting");
        Mockito.when(userKeyValueDataService.loadByKey(Mockito.anyString(), Mockito.anyLong()))
                .thenReturn(new UserKeyValueDataDTO(GeneralUtils.generateUUID(), "greeting", "Good day!"));

        final String result = inboundParamMatchServiceImpl.processParamMatch(ctx, "/person", responseBody, sanitizedUserCtxInboundPath, userId);

        Assert.assertEquals("I say Good day!", result);
    }

    @Test
    public void processParamMatch_kvpNestedRequestParamMatch_Test(){

        final String responseBody = "Watcha " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.lookUpKvp +"(" + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.requestParameter + "(name)" + ")";

        Mockito.when(request.getMethod()).thenReturn(HttpMethod.GET.name());
        Map<String, String[]> params = new HashMap<>();
        params.put("name", new String[]{"Max"});
        Mockito.when(request.getParameterMap()).thenReturn(params);
        // Mock queryParamMap to be non-empty so the GET path is taken
        Map<String, List<String>> queryParams = new HashMap<>();
        queryParams.put("name", Arrays.asList("Max"));
        Mockito.when(ctx.queryParamMap()).thenReturn(queryParams);
        Mockito.when(userKeyValueDataService.loadByKey(Mockito.anyString(), Mockito.anyLong()))
                .thenReturn(new UserKeyValueDataDTO(GeneralUtils.generateUUID(), "max", "Your name is Max"));

        final String result = inboundParamMatchServiceImpl.processParamMatch(ctx, "/person", responseBody, sanitizedUserCtxInboundPath, userId);

        Assert.assertEquals("Watcha Your name is Max", result);
    }

    @Test
    public void processParamMatch_kvpNestedPathVarMatch_Test() {

        final String responseBody = "Watcha " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.lookUpKvp +"(" + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.pathVar + "(name)" + ")";

        sanitizedUserCtxInboundPath = "/person/max";
        Mockito.when(userKeyValueDataService.loadByKey(Mockito.anyString(), Mockito.anyLong()))
                .thenReturn(new UserKeyValueDataDTO(GeneralUtils.generateUUID(), "max", "Your name is Max"));

        final String result = inboundParamMatchServiceImpl.processParamMatch(ctx, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId);

        Assert.assertEquals("Watcha Your name is Max", result);
    }

    @Test
    public void processParamMatch_kvpNestedRequestHeaderMatch_Test() {

        final String responseBody = "Watcha " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.lookUpKvp +"(" + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.requestHeader + "(name)" + ")";

        Mockito.when(request.getHeaderNames()).thenReturn(Collections.enumeration(Arrays.asList("name")));
        Mockito.when(request.getHeader("name")).thenReturn("Max");
        Mockito.when(userKeyValueDataService.loadByKey(Mockito.anyString(), Mockito.anyLong()))
                .thenReturn(new UserKeyValueDataDTO(GeneralUtils.generateUUID(), "max", "Your name is Max"));

        final String result = inboundParamMatchServiceImpl.processParamMatch(ctx, "/person", responseBody, sanitizedUserCtxInboundPath, userId);

        Assert.assertEquals("Watcha Your name is Max", result);
    }

    @Test
    public void processParamMatch_kvpNestedInvalidParam_Test()  {

        final String responseBody = "Watcha " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.lookUpKvp +"(" + ParamMatchTypeEnum.PARAM_PREFIX + "XXX(name)" + ")";

        final String result = inboundParamMatchServiceImpl.processParamMatch(ctx, "/person", responseBody, sanitizedUserCtxInboundPath, userId);

        Assert.assertEquals("Watcha ", result);
    }

}
