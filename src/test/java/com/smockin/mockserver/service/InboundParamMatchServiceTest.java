package com.smockin.mockserver.service;

import com.smockin.admin.enums.UserModeEnum;
import com.smockin.admin.service.SmockinUserService;
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

    @Mock
    private SmockinUserService smockinUserService;

    @Spy
    @InjectMocks
    private InboundParamMatchServiceImpl inboundParamMatchServiceImpl = new InboundParamMatchServiceImpl();


    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {

        sanitizedUserCtxInboundPath = "";
        request = Mockito.mock(Request.class);
    }

    @Test
    public void processParamMatch_NoToken_Test() {
        Assert.assertNull(inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}","Hello World", sanitizedUserCtxInboundPath));
    }

    @Test
    public void processParamMatch_InvalidToken_Test() {

        // Assertions
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Unsupported token : FOO");

        // Test
        final String responseBody = "Hello ${FOO=name}";

        Assert.assertNull(inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath));
    }

    @Test
    public void processParamMatch_InvalidTokenNoEqualsSymbol_Test() {

        // Assertions
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Unsupported token : FOO");

        // Test
        final String responseBody = "Hello ${FOO}";

        Assert.assertNull(inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath));
    }

    @Test
    public void processParamMatch_InvalidTokenNonsense_Test() {

        // Assertions
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Unsupported token : xxx YYY zzz");

        // Test
        final String responseBody = "Hello ${xxx YYY zzz}";

        Assert.assertNull(inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath));
    }

    @Test
    public void processParamMatch_Empty_Test() {

        // Assertions
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Unsupported token :   ");

        // Test
        final String responseBody = "Hello ${  }";

        Assert.assertNull(inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath));
    }

    @Test
    public void processParamMatch_Blank_Test() {

        // Assertions
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Unsupported token : ");

        // Test
        final String responseBody = "Hello ${}";

        Assert.assertNull(inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath));
    }

    @Test
    public void processParamMatch_header_Test() {

        // Setup
        final String responseBody = "Hello ${"+ ParamMatchTypeEnum.REQ_HEAD.name() +"=name}";

        Mockito.when(request.headers("name")).thenReturn("Roger");
        Mockito.when(request.headers()).thenReturn(new HashSet<String>() {
            {
                add("name");
            }
        });

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath);

        // Assertions
        Assert.assertEquals("Hello Roger", result);
    }

    @Test
    public void processParamMatch_headerCase_Test() {

        // Setup
        final String responseBody = "Hello ${"+ ParamMatchTypeEnum.REQ_HEAD.name() +"=NAME}";

        Mockito.when(request.headers("name")).thenReturn("Roger");
        Mockito.when(request.headers()).thenReturn(new HashSet<String>() {
            {
                add("name");
            }
        });

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath);

        // Assertions
        Assert.assertEquals("Hello Roger", result);
    }

    @Test
    public void processParamMatch_headerNoMatch_Test() {

        // Test
        final String responseBody = "Hello ${"+ ParamMatchTypeEnum.REQ_HEAD.name() +"=name}";
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath);

        // Assertions
        Assert.assertEquals("Hello ", result);
    }

    @Test
    public void processParamMatch_reqParam_Test() {

        // Setup
        final String responseBody = "Hello ${"+ ParamMatchTypeEnum.REQ_PARAM.name() +"=name}";

        Mockito.when(request.queryParams("name")).thenReturn("Roger");
        Mockito.when(request.queryParams()).thenReturn(new HashSet<String>() {
            {
                add("name");
            }
        });

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath);

        // Assertions
        Assert.assertEquals("Hello Roger", result);
    }

    @Test
    public void processParamMatch_reqParamCase_Test() {

        // Setup
        final String responseBody = "Hello ${"+ ParamMatchTypeEnum.REQ_PARAM.name() +"=NAME}";

        Mockito.when(request.queryParams("name")).thenReturn("Roger");
        Mockito.when(request.queryParams()).thenReturn(new HashSet<String>() {
            {
                add("name");
            }
        });

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath);

        // Assertions
        Assert.assertEquals("Hello Roger", result);
    }

    @Test
    public void processParamMatch_reqParamNoMatch_Test() {

        // Test
        final String responseBody = "Hello ${"+ ParamMatchTypeEnum.REQ_PARAM.name() +"=name}";
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath);

        // Assertions
        Assert.assertEquals("Hello ", result);
    }

    @Test
    public void processParamMatch_pathVar_Test() {

        // Setup
        final String responseBody = "Hello ${"+ ParamMatchTypeEnum.PATH_VAR.name() +"=name}";

        sanitizedUserCtxInboundPath = "/person/Roger";

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath);

        // Assertions
        Assert.assertEquals("Hello Roger", result);
    }

    @Test
    public void processParamMatch_pathVarCase_Test() {

        // Setup
        final String responseBody = "Hello ${"+ ParamMatchTypeEnum.PATH_VAR.name() +"=NAME}";

        sanitizedUserCtxInboundPath = "/person/Roger";

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath);

        // Assertions
        Assert.assertEquals("Hello Roger", result);
    }

    @Test
    public void processParamMatch_pathVarNoMatch_Test() {

        // Test
        final String responseBody = "Hello ${"+ ParamMatchTypeEnum.PATH_VAR.name() +"=name}";
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath);

        // Assertions
        Assert.assertEquals("Hello ", result);
    }

    @Test
    public void enrichWithInboundParamMatches_multiMatchesAndSpaces_Test() {

        // Setup
        final String responseBody = "Hello ${"+ ParamMatchTypeEnum.REQ_HEAD.name() +"=name}, you are ${"+ ParamMatchTypeEnum.REQ_HEAD.name() +"= gender  } and are ${"+ ParamMatchTypeEnum.REQ_HEAD.name() +"=age} years old";

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
        final String result = inboundParamMatchServiceImpl.enrichWithInboundParamMatches(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath);

        // Assertions
        Assert.assertEquals("Hello Roger, you are Male and are 21 years old", result);
    }

    @Test
    public void enrichWithInboundParamMatches_partialMatch_Test() {

        // Setup
        final String responseBody = "Hello ${"+ ParamMatchTypeEnum.REQ_HEAD.name() +"=name}, you are ${"+ ParamMatchTypeEnum.REQ_HEAD.name() +"=age} years old";

        Mockito.when(request.headers("name")).thenReturn("Roger");
        Mockito.when(request.headers()).thenReturn(new HashSet<String>() {
            {
                add("name");
            }
        });

        // Test
        final String result = inboundParamMatchServiceImpl.enrichWithInboundParamMatches(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath);

        // Assertions
        Assert.assertEquals("Hello Roger, you are  years old", result);
    }

    @Test
    public void enrichWithInboundParamMatches_withIllegalToken_Test() {

        // Assertions
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Unsupported token : FOO");

        // Setup
        final String responseBody = "Hello ${"+ ParamMatchTypeEnum.REQ_HEAD.name() +"=name}, you are ${FOO=age} years old";

        Mockito.when(request.headers("name")).thenReturn("Roger");
        Mockito.when(request.headers()).thenReturn(new HashSet<String>() {
            {
                add("name");
            }
        });

        inboundParamMatchServiceImpl.enrichWithInboundParamMatches(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath);
    }

    @Test
    public void processParamMatch_isoDate_Test() {

        // Setup
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        final String responseBody = "The date is ${"+ ParamMatchTypeEnum.ISO_DATE.name() + "}";

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath);

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
        final String responseBody = "The date and time is ${"+ ParamMatchTypeEnum.ISO_DATETIME.name() + "}";

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath);

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
        final String responseBody = "Your ID is ${"+ ParamMatchTypeEnum.UUID.name() + "}";

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath);

        // Assertions
        final String remainder = result.replaceAll("Your ID is ", "");

        try {
            Assert.assertNotNull(UUID.fromString(remainder));
        } catch (Throwable ex) {
            Assert.fail();
        }
    }

    @Test
    public void processParamMatch_random_Test() {

        // Setup
        final String responseBody = "Your number is ${"+ ParamMatchTypeEnum.RANDOM_NUMBER.name() + "}";

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath);

        // Assertions
        final String remainder = result.replaceAll("Your number is ", "");
        Assert.assertTrue(NumberUtils.isDigits(remainder));
    }

    @Test
    public void processParamMatch_randomRangeTO_AllPositive_Test() {

        // Setup
        final String responseBody = "Your number is ${"+ ParamMatchTypeEnum.RANDOM_NUMBER.name() + "=1to3}";

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath);

        // Assertions
        final String remainder = result.replaceAll("Your number is ", "");
        Assert.assertTrue(NumberUtils.isDigits(remainder));
        Assert.assertTrue((Integer.valueOf(remainder) == 1) || (Integer.valueOf(remainder) == 2) || (Integer.valueOf(remainder) == 3));
    }

    @Test
    public void processParamMatch_randomRangeTO_NegativeToPositive_Test() {

        // Setup
        final String responseBody = "Your number is ${"+ ParamMatchTypeEnum.RANDOM_NUMBER.name() + "=-2to2}";

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath);

        // Assertions
        final String remainder = result.replaceAll("Your number is ", "");
        Assert.assertTrue(NumberUtils.isCreatable(remainder));
        Assert.assertTrue((Integer.valueOf(remainder) == -2) || (Integer.valueOf(remainder) == -1) || (Integer.valueOf(remainder) == 0) || (Integer.valueOf(remainder) == 1) || (Integer.valueOf(remainder) == 2));
    }

    @Test
    public void processParamMatch_randomRangeTO_NegativeToNegative_Test() {

        // Setup
        final String responseBody = "Your number is ${"+ ParamMatchTypeEnum.RANDOM_NUMBER.name() + "=-4to-2}";

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath);

        // Assertions
        final String remainder = result.replaceAll("Your number is ", "");
        Assert.assertTrue(NumberUtils.isCreatable(remainder));
        Assert.assertTrue((Integer.valueOf(remainder) == -4) || (Integer.valueOf(remainder) == -3) || (Integer.valueOf(remainder) == -2));
    }

    @Test
    public void processParamMatch_randomRangeTO_NegativeToZero_Test() {

        // Setup
        final String responseBody = "Your number is ${"+ ParamMatchTypeEnum.RANDOM_NUMBER.name() + "=-3to0}";

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath);

        // Assertions
        final String remainder = result.replaceAll("Your number is ", "");
        Assert.assertTrue(NumberUtils.isCreatable(remainder));
        Assert.assertTrue((Integer.valueOf(remainder) == -3) || (Integer.valueOf(remainder) == -2) || (Integer.valueOf(remainder) == -1)  || (Integer.valueOf(remainder) == 0));
    }

    @Test
    public void processParamMatch_randomRangeTO_ZeroToPositive_Test() {

        // Setup
        final String responseBody = "Your number is ${"+ ParamMatchTypeEnum.RANDOM_NUMBER.name() + "=0to2}";

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath);

        // Assertions
        final String remainder = result.replaceAll("Your number is ", "");
        Assert.assertTrue(NumberUtils.isDigits(remainder));
        Assert.assertTrue((Integer.valueOf(remainder) == 0) || (Integer.valueOf(remainder) == 1) || (Integer.valueOf(remainder) == 2));
    }

    @Test
    public void processParamMatch_randomRangeTO_Zero_Test() {

        // Setup
        final String responseBody = "Your number is ${"+ ParamMatchTypeEnum.RANDOM_NUMBER.name() + "=0to0}";

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath);

        // Assertions
        final String remainder = result.replaceAll("Your number is ", "");
        Assert.assertTrue(NumberUtils.isDigits(remainder));
        Assert.assertEquals(Integer.valueOf(0), Integer.valueOf(remainder));
    }

    @Test
    public void processParamMatch_randomRangeTO_SamePositiveValue_Test() {

        // Setup
        final String responseBody = "Your number is ${"+ ParamMatchTypeEnum.RANDOM_NUMBER.name() + "=4to4}";

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath);

        // Assertions
        final String remainder = result.replaceAll("Your number is ", "");
        Assert.assertTrue(NumberUtils.isDigits(remainder));
        Assert.assertEquals(Integer.valueOf(4), Integer.valueOf(remainder));
    }

    @Test
    public void processParamMatch_randomRangeTO_SameNegativeValue_Test() {

        // Setup
        final String responseBody = "Your number is ${"+ ParamMatchTypeEnum.RANDOM_NUMBER.name() + "=-3 to -3}";

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath);

        // Assertions
        final String remainder = result.replaceAll("Your number is ", "");
        Assert.assertTrue(NumberUtils.isCreatable(remainder));
        Assert.assertEquals(Integer.valueOf(-3), Integer.valueOf(remainder));
    }

    @Test
    public void processParamMatch_randomRangeTOCase_Test() {

        // Setup
        final String responseBody = "Your number is ${"+ ParamMatchTypeEnum.RANDOM_NUMBER.name() + "=1 tO 3}";

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath);

        // Assertions
        final String remainder = result.replaceAll("Your number is ", "");
        Assert.assertTrue(NumberUtils.isDigits(remainder));
        Assert.assertTrue((Integer.valueOf(remainder) == 1) || (Integer.valueOf(remainder) == 2) || (Integer.valueOf(remainder) == 3));
    }

    @Test
    public void processParamMatch_randomRangeStartFrom5_Test() {

        // Setup
        final String responseBody = "Your number is ${"+ ParamMatchTypeEnum.RANDOM_NUMBER.name() + "=5 to 6}";

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath);

        // Assertions
        final String remainder = result.replaceAll("Your number is ", "");
        Assert.assertTrue(NumberUtils.isDigits(remainder));
        Assert.assertTrue((Integer.valueOf(remainder) == 5) || (Integer.valueOf(remainder) == 6));
    }

    @Test
    public void processParamMatch_randomRangeUNTIL_Test() {

        // Setup
        final String responseBody = "Your number is ${"+ ParamMatchTypeEnum.RANDOM_NUMBER.name() + "=1until3}";

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath);

        // Assertions
        final String remainder = result.replaceAll("Your number is ", "");
        Assert.assertTrue(NumberUtils.isDigits(remainder));
        Assert.assertTrue(((Integer.valueOf(remainder) == 1) || (Integer.valueOf(remainder) == 2)) && (Integer.valueOf(remainder) != 3));
    }

    @Test
    public void processParamMatch_randomRangeWhiteSpace_Test() {

        // Setup
        final String responseBody = "Your number is ${"+ ParamMatchTypeEnum.RANDOM_NUMBER.name() + "= 1  until  3   }";

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath);

        // Assertions
        final String remainder = result.replaceAll("Your number is ", "");
        Assert.assertTrue(NumberUtils.isDigits(remainder));
        Assert.assertTrue(((Integer.valueOf(remainder) == 1) || (Integer.valueOf(remainder) == 2)) && (Integer.valueOf(remainder) != 3));
    }

    @Test
    public void processParamMatch_randomRangeMissingArg_Test() {

        // Assertions
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Expected '" + inboundParamMatchServiceImpl.TO_ARG + "' or '" + inboundParamMatchServiceImpl.UNTIL_ARG + "' arg in '" + ParamMatchTypeEnum.RANDOM_NUMBER.name() + "=' token");

        // Setup
        final String responseBody = "Your number is ${"+ ParamMatchTypeEnum.RANDOM_NUMBER.name() + "=}";

        // Test
        inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath);
    }

    @Test
    public void processParamMatch_randomRangeInvalidArg_Test() {

        // Assertions
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Expected '" + inboundParamMatchServiceImpl.TO_ARG + "' or '" + inboundParamMatchServiceImpl.UNTIL_ARG + "' arg in '" + ParamMatchTypeEnum.RANDOM_NUMBER.name() + "=' token");

        // Setup
        final String responseBody = "Your number is ${"+ ParamMatchTypeEnum.RANDOM_NUMBER.name() + "=1foo2}";

        // Test
        inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath);
    }

    @Test
    public void processParamMatch_randomRangeMissingRangeNoNumbers_Test() {

        // Assertions
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Missing number range for '" + inboundParamMatchServiceImpl.TO_ARG + "' args. (i.e expect 1 " + inboundParamMatchServiceImpl.TO_ARG + " 5)");

        // Setup
        final String responseBody = "Your number is ${"+ ParamMatchTypeEnum.RANDOM_NUMBER.name() + "=" + inboundParamMatchServiceImpl.TO_ARG + "}";

        // Test
        inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath);
    }

    @Test
    public void processParamMatch_randomRangeMissingRangeStartNumberOnly_Test() {

        // Assertions
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Missing number range for '" + inboundParamMatchServiceImpl.TO_ARG + "' args. (i.e expect 1 " + inboundParamMatchServiceImpl.TO_ARG + " 5)");

        // Setup
        final String responseBody = "Your number is ${"+ ParamMatchTypeEnum.RANDOM_NUMBER.name() + "=2" + inboundParamMatchServiceImpl.TO_ARG + "}";

        // Test
        inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath);
    }

    @Test
    public void processParamMatch_randomRangeMissingRangeEndNumberOnly_Test() {

        // Assertions
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Range does not contain valid numbers. (i.e expect 1 " + inboundParamMatchServiceImpl.TO_ARG + " 5)");

        // Setup
        final String responseBody = "Your number is ${"+ ParamMatchTypeEnum.RANDOM_NUMBER.name() + "=" + inboundParamMatchServiceImpl.TO_ARG + "5}";

        // Test
        inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath);
    }

    @Test
    public void processParamMatch_randomRangeMissingRangeIsText_Test() {

        // Assertions
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Range does not contain valid numbers. (i.e expect 1 " + inboundParamMatchServiceImpl.TO_ARG + " 5)");

        // Setup
        final String responseBody = "Your number is ${"+ ParamMatchTypeEnum.RANDOM_NUMBER.name() + "=A" + inboundParamMatchServiceImpl.TO_ARG + "Z}";

        // Test
        inboundParamMatchServiceImpl.processParamMatch(request, "/person/{name}", responseBody, sanitizedUserCtxInboundPath);
    }

}
