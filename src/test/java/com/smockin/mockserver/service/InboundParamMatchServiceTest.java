package com.smockin.mockserver.service;

import com.smockin.mockserver.service.enums.ParamMatchTypeEnum;
import com.smockin.utils.GeneralUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import spark.Request;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by mgallina.
 */
public class InboundParamMatchServiceTest {

    private Request request;
    private InboundParamMatchServiceImpl inboundParamMatchServiceImpl;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {

        inboundParamMatchServiceImpl = new InboundParamMatchServiceImpl();
        request = Mockito.mock(Request.class);
    }

    @Test
    public void processParamMatch_NoToken_Test() {
        Assert.assertNull(inboundParamMatchServiceImpl.processParamMatch(request, "Hello World"));
    }

    @Test
    public void processParamMatch_InvalidToken_Test() {

        // Assertions
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Unsupported token : FOO");

        // Test
        final String responseBody = "Hello ${FOO=name}";

        Assert.assertNull(inboundParamMatchServiceImpl.processParamMatch(request, responseBody));
    }

    @Test
    public void processParamMatch_InvalidTokenNoEqualsSymbol_Test() {

        // Assertions
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Unsupported token : FOO");

        // Test
        final String responseBody = "Hello ${FOO}";

        Assert.assertNull(inboundParamMatchServiceImpl.processParamMatch(request, responseBody));
    }

    @Test
    public void processParamMatch_InvalidTokenNonsense_Test() {

        // Assertions
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Unsupported token : xxx YYY zzz");

        // Test
        final String responseBody = "Hello ${xxx YYY zzz}";

        Assert.assertNull(inboundParamMatchServiceImpl.processParamMatch(request, responseBody));
    }

    @Test
    public void processParamMatch_Empty_Test() {

        // Assertions
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Unsupported token :   ");

        // Test
        final String responseBody = "Hello ${  }";

        Assert.assertNull(inboundParamMatchServiceImpl.processParamMatch(request, responseBody));
    }

    @Test
    public void processParamMatch_Blank_Test() {

        // Assertions
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Unsupported token : ");

        // Test
        final String responseBody = "Hello ${}";

        Assert.assertNull(inboundParamMatchServiceImpl.processParamMatch(request, responseBody));
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
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, responseBody);

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
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, responseBody);

        // Assertions
        Assert.assertEquals("Hello Roger", result);
    }

    @Test
    public void processParamMatch_headerNoMatch_Test() {

        // Test
        final String responseBody = "Hello ${"+ ParamMatchTypeEnum.REQ_HEAD.name() +"=name}";
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, responseBody);

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
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, responseBody);

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
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, responseBody);

        // Assertions
        Assert.assertEquals("Hello Roger", result);
    }

    @Test
    public void processParamMatch_reqParamNoMatch_Test() {

        // Test
        final String responseBody = "Hello ${"+ ParamMatchTypeEnum.REQ_PARAM.name() +"=name}";
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, responseBody);

        // Assertions
        Assert.assertEquals("Hello ", result);
    }

    @Test
    public void processParamMatch_pathVar_Test() {

        // Setup
        final String responseBody = "Hello ${"+ ParamMatchTypeEnum.PATH_VAR.name() +"=name}";

        Mockito.when(request.params()).thenReturn(new HashMap<String, String>() {
            {
                put(":name", "Roger");
            }
        });

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, responseBody);

        // Assertions
        Assert.assertEquals("Hello Roger", result);
    }

    @Test
    public void processParamMatch_pathVarCase_Test() {

        // Setup
        final String responseBody = "Hello ${"+ ParamMatchTypeEnum.PATH_VAR.name() +"=NAME}";

        Mockito.when(request.params()).thenReturn(new HashMap<String, String>() {
            {
                put(":name", "Roger");
            }
        });

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, responseBody);

        // Assertions
        Assert.assertEquals("Hello Roger", result);
    }

    @Test
    public void processParamMatch_pathVarNoMatch_Test() {

        // Test
        final String responseBody = "Hello ${"+ ParamMatchTypeEnum.PATH_VAR.name() +"=name}";
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, responseBody);

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

        // Test
        final String result = inboundParamMatchServiceImpl.enrichWithInboundParamMatches(request, responseBody);

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
        final String result = inboundParamMatchServiceImpl.enrichWithInboundParamMatches(request, responseBody);

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

        inboundParamMatchServiceImpl.enrichWithInboundParamMatches(request, responseBody);
    }

    @Test
    public void processParamMatch_isoDate_Test() {

        // Setup
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        final String responseBody = "The date is ${"+ ParamMatchTypeEnum.ISO_DATE.name() + "}";

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, responseBody);

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
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, responseBody);

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
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, responseBody);

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
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, responseBody);

        // Assertions
        final String remainder = result.replaceAll("Your number is ", "");
        Assert.assertTrue(NumberUtils.isDigits(remainder));
    }

    @Test
    public void processParamMatch_randomRangeTo_Test() {

        // Setup
        final String responseBody = "Your number is ${"+ ParamMatchTypeEnum.RANDOM_NUMBER.name() + "=1to3}";

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, responseBody);

        // Assertions
        final String remainder = result.replaceAll("Your number is ", "");
        Assert.assertTrue(NumberUtils.isDigits(remainder));
        Assert.assertTrue((Integer.valueOf(remainder) == 1) || (Integer.valueOf(remainder) == 2) || (Integer.valueOf(remainder) == 3));
    }

    @Test
    public void processParamMatch_randomRangeUntil_Test() {

        // Setup
        final String responseBody = "Your number is ${"+ ParamMatchTypeEnum.RANDOM_NUMBER.name() + "=1until3}";

        // Test
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, responseBody);

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
        final String result = inboundParamMatchServiceImpl.processParamMatch(request, responseBody);

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
        inboundParamMatchServiceImpl.processParamMatch(request, responseBody);
    }

    @Test
    public void processParamMatch_randomRangeInvalidArg_Test() {

        // Assertions
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Expected '" + inboundParamMatchServiceImpl.TO_ARG + "' or '" + inboundParamMatchServiceImpl.UNTIL_ARG + "' arg in '" + ParamMatchTypeEnum.RANDOM_NUMBER.name() + "=' token");

        // Setup
        final String responseBody = "Your number is ${"+ ParamMatchTypeEnum.RANDOM_NUMBER.name() + "=1foo2}";

        // Test
        inboundParamMatchServiceImpl.processParamMatch(request, responseBody);
    }

    @Test
    public void processParamMatch_randomRangeMissingRangeNoNumbers_Test() {

        // Assertions
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Missing number range for '" + inboundParamMatchServiceImpl.TO_ARG + "' args. (i.e expect 1 " + inboundParamMatchServiceImpl.TO_ARG + " 5)");

        // Setup
        final String responseBody = "Your number is ${"+ ParamMatchTypeEnum.RANDOM_NUMBER.name() + "=" + inboundParamMatchServiceImpl.TO_ARG + "}";

        // Test
        inboundParamMatchServiceImpl.processParamMatch(request, responseBody);
    }

    @Test
    public void processParamMatch_randomRangeMissingRangeStartNumberOnly_Test() {

        // Assertions
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Missing number range for '" + inboundParamMatchServiceImpl.TO_ARG + "' args. (i.e expect 1 " + inboundParamMatchServiceImpl.TO_ARG + " 5)");

        // Setup
        final String responseBody = "Your number is ${"+ ParamMatchTypeEnum.RANDOM_NUMBER.name() + "=2" + inboundParamMatchServiceImpl.TO_ARG + "}";

        // Test
        inboundParamMatchServiceImpl.processParamMatch(request, responseBody);
    }

    @Test
    public void processParamMatch_randomRangeMissingRangeEndNumberOnly_Test() {

        // Assertions
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Range does not contain valid numbers. (i.e expect 1 to 5)");

        // Setup
        final String responseBody = "Your number is ${"+ ParamMatchTypeEnum.RANDOM_NUMBER.name() + "=" + inboundParamMatchServiceImpl.TO_ARG + "5}";

        // Test
        inboundParamMatchServiceImpl.processParamMatch(request, responseBody);
    }

    @Test
    public void processParamMatch_randomRangeMissingRangeIsText_Test() {

        // Assertions
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Range does not contain valid numbers. (i.e expect 1 " + inboundParamMatchServiceImpl.TO_ARG + " 5)");

        // Setup
        final String responseBody = "Your number is ${"+ ParamMatchTypeEnum.RANDOM_NUMBER.name() + "=A" + inboundParamMatchServiceImpl.TO_ARG + "Z}";

        // Test
        inboundParamMatchServiceImpl.processParamMatch(request, responseBody);
    }

}
