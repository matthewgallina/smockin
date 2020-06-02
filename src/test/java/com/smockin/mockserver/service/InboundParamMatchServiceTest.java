package com.smockin.mockserver.service;

import com.smockin.admin.dto.UserKeyValueDataDTO;
import com.smockin.admin.enums.UserModeEnum;
import com.smockin.admin.service.SmockinUserService;
import com.smockin.admin.service.UserKeyValueDataService;
import com.smockin.mockserver.exception.InboundParamMatchException;
import com.smockin.mockserver.service.enums.ParamMatchTypeEnum;
import com.smockin.utils.GeneralUtils;
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
import spark.Request;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.TimeZone;
import java.util.UUID;

/**
 * Created by mgallina.
 */
@RunWith(MockitoJUnitRunner.class)
public class InboundParamMatchServiceTest {

    private Request request;
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
        request = Mockito.mock(Request.class);
    }

    @Test
    public void processParamMatch_NoToken_Test() {
        Assert.assertNull(inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}","Hello World", sanitizedUserCtxInboundPath, userId));
    }

    @Test
    public void processParamMatch_InvalidToken_Test() {

        // Test
        final String responseBody = "Hello " + ParamMatchTypeEnum.PARAM_PREFIX + "Foo";

        // Assertions
        Assert.assertNull(inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId));
    }

    @Test
    public void processParamMatch_InvalidTokenWithBrackets_Test() {

        // Setup
        final String responseBody = "Hello " + ParamMatchTypeEnum.PARAM_PREFIX + "Foo()";

        // Test & Assertions
        Assert.assertNull(inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId));
    }

    @Test
    public void processParamMatch_Empty_Test() {

        // Setup
        final String responseBody = "Hello " + ParamMatchTypeEnum.PARAM_PREFIX + "(  )";

        // Test & Assertions
        Assert.assertNull(inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId));
    }

    @Test
    public void processParamMatch_Blank_Test() {

        // Setup
        final String responseBody = "Hello " + ParamMatchTypeEnum.PARAM_PREFIX + "()";

        // Test & Assertions
        Assert.assertNull(inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId));

    }

    @Test
    public void processParamMatch_header_Test() {

        // Setup
        final String responseBody = "Hello " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.requestHeader.name() +"(name)";

        Mockito.when(request.headers("name")).thenReturn("Roger");
        Mockito.when(request.headers()).thenReturn(new HashSet<String>() {
            {
                add("name");
            }
        });

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId);

        // Assertions
        Assert.assertEquals("Hello Roger", result);
    }

    @Test
    public void processParamMatch_headerCase_Test() {

        // Setup
        final String responseBody = "Hello " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.requestHeader.name() +"(NAME)";

        Mockito.when(request.headers("name")).thenReturn("Roger");
        Mockito.when(request.headers()).thenReturn(new HashSet<String>() {
            {
                add("name");
            }
        });

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId);

        // Assertions
        Assert.assertEquals("Hello Roger", result);
    }

    @Test
    public void processParamMatch_headerNoMatch_Test() {

        // Test
        final String responseBody = "Hello " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.requestHeader.name() +"(name)";
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId);

        // Assertions
        Assert.assertEquals("Hello ", result);
    }

    @Test
    public void processParamMatch_reqParam_Test() {

        // Setup
        final String responseBody = "Hello " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.requestParameter.name() +"(name)";

        Mockito.when(request.queryParams("name")).thenReturn("Roger");
        Mockito.when(request.queryParams()).thenReturn(new HashSet<String>() {
            {
                add("name");
            }
        });

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId);

        // Assertions
        Assert.assertEquals("Hello Roger", result);
    }

    @Test
    public void processParamMatch_reqParamCase_Test() {

        // Setup
        final String responseBody = "Hello " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.requestParameter.name() +"(NAME)";

        Mockito.when(request.queryParams("name")).thenReturn("Roger");
        Mockito.when(request.queryParams()).thenReturn(new HashSet<String>() {
            {
                add("name");
            }
        });

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId);

        // Assertions
        Assert.assertEquals("Hello Roger", result);
    }

    @Test
    public void processParamMatch_reqParamNoMatch_Test() {

        // Test
        final String responseBody = "Hello " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.requestParameter.name() +"(name)";
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId);

        // Assertions
        Assert.assertEquals("Hello ", result);
    }

    @Test
    public void processParamMatch_pathVar_Test() {

        // Setup
        final String responseBody = "Hello " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.pathVar.name() +"(name)";

        sanitizedUserCtxInboundPath = "/person/Roger";

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId);

        // Assertions
        Assert.assertEquals("Hello Roger", result);
    }

    @Test
    public void processParamMatch_pathVarCase_Test() {

        // Setup
        final String responseBody = "Hello " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.pathVar.name() +"(NAME)";

        sanitizedUserCtxInboundPath = "/person/Roger";

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId);

        // Assertions
        Assert.assertEquals("Hello Roger", result);
    }

    @Test
    public void processParamMatch_pathVarNoMatch_Test() {

        // Test
        final String responseBody = "Hello " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.pathVar.name() +"(name)";
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId);

        // Assertions
        Assert.assertEquals("Hello ", result);
    }

    @Test
    public void enrichWithInboundParamMatches_multiMatchesAndSpaces_Test() throws InboundParamMatchException {

        // Setup
        final String responseBody = "Hello " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.requestHeader.name() +"('name'), you are " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.requestHeader.name() +"(GenDer) and are "  + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.requestHeader.name() +"(\"age\") years old";

        Mockito.when(request.headers("name")).thenReturn("Roger");
        Mockito.when(request.headers("age")).thenReturn("21");
        Mockito.when(request.headers("gender")).thenReturn("Male");
        Mockito.when(request.headers()).thenReturn(new HashSet<String>() {
            {
                add("name");
                add("age");
                add("gender");
            }
        });

        Mockito.when(smockinUserService.getUserMode()).thenReturn(UserModeEnum.INACTIVE);

        // Test
        final String result = inboundParamMatchServiceImpl.enrichWithInboundParamMatches(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId);

        // Assertions
        Assert.assertEquals("Hello Roger, you are Male and are 21 years old", result);
    }

    @Test
    public void enrichWithInboundParamMatches_partialMatch_Test() throws InboundParamMatchException {

        // Setup
        final String responseBody = "Hello " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.requestHeader.name() +"(name), you are " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.requestHeader.name() +"(age) years old";

        Mockito.when(request.headers("name")).thenReturn("Roger");
        Mockito.when(request.headers()).thenReturn(new HashSet<String>() {
            {
                add("name");
            }
        });

        // Test
        final String result = inboundParamMatchServiceImpl.enrichWithInboundParamMatches(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId);

        // Assertions
        Assert.assertEquals("Hello Roger, you are  years old", result);
    }

    @Test
    public void enrichWithInboundParamMatches_withNoMadeUpToken_Test() throws InboundParamMatchException {

        // Setup
        final String responseBody = "Hello " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.requestHeader.name() +"(name), you are " + ParamMatchTypeEnum.PARAM_PREFIX + "FOO(age) years old";

        Mockito.when(request.headers("name")).thenReturn("Roger");
        Mockito.when(request.headers()).thenReturn(new HashSet<String>() {
            {
                add("name");
            }
        });

        // Test
        final String result = inboundParamMatchServiceImpl.enrichWithInboundParamMatches(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId);

        // Assertions
        Assert.assertNotNull(result);
        Assert.assertEquals("Hello Roger, you are " + ParamMatchTypeEnum.PARAM_PREFIX + "FOO(age) years old", result);
    }

    @Test
    public void processParamMatch_isoDate_Test() {

        // Setup
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        final String responseBody = "The date is " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.isoDate.name();

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId);

        // Assertions
        final String remainder = result.replaceAll("The date is ", "");

        try {
            Assert.assertNotNull(new SimpleDateFormat(GeneralUtils.ISO_DATE_FORMAT).parse(remainder));
        } catch (Throwable ex) {
            Assert.fail();
        }
    }

    @Test
    public void processParamMatch_isoDateTime_Test() {

        // Setup
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        final String responseBody = "The date and time is " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.isoDatetime.name();

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId);

        // Assertions
        final String remainder = result.replaceAll("The date and time is ", "");

        try {
            Assert.assertNotNull(new SimpleDateFormat(GeneralUtils.ISO_DATETIME_FORMAT).parse(remainder));
        } catch (Throwable ex) {
            Assert.fail();
        }
    }

    @Test
    public void processParamMatch_uuid_Test() {

        // Setup
        final String responseBody = "Your ID is " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.uuid.name();

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId);

        // Assertions
        final String remainder = result.replaceAll("Your ID is ", "");

        try {
            Assert.assertNotNull(UUID.fromString(remainder));
        } catch (Throwable ex) {
            Assert.fail();
        }
    }

    @Test
    public void processParamMatch_randomNumber_Test() {

        // Setup
        final String responseBody = "Your number is " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.randomNumber.name() + "(1,3)";

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId);

        // Assertions
        final String remainder = result.replaceAll("Your number is ", "");
        Assert.assertTrue(NumberUtils.isDigits(remainder));
        Assert.assertTrue((Integer.valueOf(remainder) == 1) || (Integer.valueOf(remainder) == 2) || (Integer.valueOf(remainder) == 3));
    }

    @Test
    public void processParamMatch_randomNumberZero_Test() {

        // Setup
        final String responseBody = "Your number is " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.randomNumber.name() + "(0,0)";

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId);

        // Assertions
        final String remainder = result.replaceAll("Your number is ", "");
        Assert.assertTrue(NumberUtils.isDigits(remainder));
        Assert.assertEquals(Integer.valueOf(0), Integer.valueOf(remainder));
    }

    @Test
    public void processParamMatch_randomNumberNoParams_Test() {

        // Assertions
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("randomNumber is missing args");

        // Setup
        final String responseBody = "Your number is " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.randomNumber.name() + "()";

        // Test
        inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId);

    }

    @Test
    public void processParamMatch_kvpMatch_Test() {

        // Setup
        final String responseBody = "I say " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.lookUpKvp +"(Hello)";

        // Mock
        Mockito.when(userKeyValueDataService.loadByKey(Mockito.anyString(), Mockito.anyLong()))
            .thenReturn(new UserKeyValueDataDTO(GeneralUtils.generateUUID(), "Hello", "Bonjour"));

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person", responseBody, sanitizedUserCtxInboundPath, userId);

        // Assertions
        Assert.assertEquals("I say Bonjour", result);
    }

    @Test
    public void processParamMatch_kvpNoMatch_Test() {

        // Setup
        final String responseBody = "I say "+ ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.lookUpKvp +"(Hello)";

        // Mock
        Mockito.when(userKeyValueDataService.loadByKey(Mockito.anyString(), Mockito.anyLong()))
            .thenReturn(null);

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person", responseBody, sanitizedUserCtxInboundPath, userId);

        // Assertions
        Assert.assertEquals("I say ", result);
    }

    @Test
    public void processParamMatch_kvpNestedRequestBodyMatch_Test() {

        // Setup
        final String responseBody = "I say " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.lookUpKvp +"(" + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.requestBody + ")";

        // Mock
        Mockito.when(request.body()).thenReturn("greeting");
        Mockito.when(userKeyValueDataService.loadByKey(Mockito.anyString(), Mockito.anyLong()))
                .thenReturn(new UserKeyValueDataDTO(GeneralUtils.generateUUID(), "greeting", "Good day!"));

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person", responseBody, sanitizedUserCtxInboundPath, userId);

        // Assertions
        Assert.assertEquals("I say Good day!", result);
    }

    @Test
    public void processParamMatch_kvpNestedRequestParamMatch_Test() {

        // Setup
        final String responseBody = "Watcha " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.lookUpKvp +"(" + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.requestParameter + "(name)" + ")";

        // Mock
        Mockito.when(request.queryParams())
                .thenReturn(new HashSet<>(Arrays.asList("name")));
        Mockito.when(request.queryParams(Mockito.anyString()))
                .thenReturn("Max");
        Mockito.when(userKeyValueDataService.loadByKey(Mockito.anyString(), Mockito.anyLong()))
                .thenReturn(new UserKeyValueDataDTO(GeneralUtils.generateUUID(), "max", "Your name is Max"));

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person", responseBody, sanitizedUserCtxInboundPath, userId);

        // Assertions
        Assert.assertEquals("Watcha Your name is Max", result);
    }

    @Test
    public void processParamMatch_kvpNestedPathVarMatch_Test() {

        // Setup
        final String responseBody = "Watcha " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.lookUpKvp +"(" + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.pathVar + "(name)" + ")";

        // Mock
        sanitizedUserCtxInboundPath = "/person/max";
        Mockito.when(userKeyValueDataService.loadByKey(Mockito.anyString(), Mockito.anyLong()))
                .thenReturn(new UserKeyValueDataDTO(GeneralUtils.generateUUID(), "max", "Your name is Max"));

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath, userId);

        // Assertions
        Assert.assertEquals("Watcha Your name is Max", result);
    }

    @Test
    public void processParamMatch_kvpNestedRequestHeaderMatch_Test() {

        // Setup
        final String responseBody = "Watcha " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.lookUpKvp +"(" + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.requestHeader + "(name)" + ")";

        // Mock
        Mockito.when(request.headers())
                .thenReturn(new HashSet<>(Arrays.asList("name")));
        Mockito.when(request.headers(Mockito.anyString()))
                .thenReturn("Max");
        Mockito.when(userKeyValueDataService.loadByKey(Mockito.anyString(), Mockito.anyLong()))
                .thenReturn(new UserKeyValueDataDTO(GeneralUtils.generateUUID(), "max", "Your name is Max"));

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person", responseBody, sanitizedUserCtxInboundPath, userId);

        // Assertions
        Assert.assertEquals("Watcha Your name is Max", result);
    }

    @Test
    public void processParamMatch_kvpNestedInvalidParam_Test() {

        // Setup
        final String responseBody = "Watcha " + ParamMatchTypeEnum.PARAM_PREFIX + ParamMatchTypeEnum.lookUpKvp +"(" + ParamMatchTypeEnum.PARAM_PREFIX + "XXX(name)" + ")";

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person", responseBody, sanitizedUserCtxInboundPath, userId);

        // Assertions
        Assert.assertEquals("Watcha ", result);
    }

}
