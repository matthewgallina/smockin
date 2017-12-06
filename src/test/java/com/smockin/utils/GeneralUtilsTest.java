package com.smockin.utils;

import com.smockin.utils.GeneralUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import spark.Request;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Created by mgallina on 08/08/17.
 */
public class GeneralUtilsTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void generateUUID_Populated_Test() {
        Assert.assertNotNull(GeneralUtils.generateUUID());
    }

    @Test
    public void generateUUID_Distinct_Test() {
        Assert.assertNotEquals(GeneralUtils.generateUUID(), GeneralUtils.generateUUID());
    }

    @Test
    public void getCurrentDate_Populated_Test() {
        Assert.assertNotNull(GeneralUtils.getCurrentDate());
    }

    @Test
    public void findFirstInboundParamMatch_singleTokenWithSpace_Test() {
        final String result = GeneralUtils.findFirstInboundParamMatch("hello ${REQ_HEAD= joe }. how are you?");
        Assert.assertEquals("REQ_HEAD= joe ", result);
    }

    @Test
    public void findFirstInboundParamMatch_singleTokenWithSpaces_Test() {
        final String result = GeneralUtils.findFirstInboundParamMatch("hello ${  REQ_HEAD=   joe   }. how are you?");
        Assert.assertEquals("  REQ_HEAD=   joe   ", result);
    }

    @Test
    public void findFirstInboundParamMatch_multiToken_Test() {
        final String result = GeneralUtils.findFirstInboundParamMatch("hello ${REQ_HEAD= max}. how are you ${REQ_HEAD bob }?");
        Assert.assertEquals("REQ_HEAD= max", result);
    }

    @Test
    public void findFirstInboundParamMatch_NoToken_Test() {
        Assert.assertNull(GeneralUtils.findFirstInboundParamMatch("hello world"));
    }

    @Test
    public void findFirstInboundParamMatch_Null_Test() {
        Assert.assertNull(GeneralUtils.findFirstInboundParamMatch(null));
    }

    @Test
    public void findFirstInboundParamMatch_Blank_Test() {
        Assert.assertNull(GeneralUtils.findFirstInboundParamMatch(""));
    }

    @Test
    public void findFirstInboundParamMatch_Empty_Test() {
        Assert.assertNull(GeneralUtils.findFirstInboundParamMatch("  "));
    }

    @Test
    public void findHeaderIgnoreCaseTest() {

        // Setup
        final Request req = Mockito.mock(Request.class);
        Mockito.when(req.headers("name")).thenReturn("Bob");
        Mockito.when(req.headers("Age")).thenReturn("21");
        Mockito.when(req.headers()).thenReturn(new HashSet<String>() {
            {
                add("name");
                add("Age");
            }
        });

        // Test
        final String nameResult = GeneralUtils.findHeaderIgnoreCase(req, "NAME");

        // Assertions
        Assert.assertNotNull(nameResult);
        Assert.assertEquals("Bob", nameResult);

        // Test
        final String ageResult = GeneralUtils.findHeaderIgnoreCase(req, "age");

        // Assertions
        Assert.assertNotNull(ageResult);
        Assert.assertEquals("21", ageResult);
    }

    @Test
    public void findRequestParamIgnoreCaseTest() {

        // Setup
        final Request req = Mockito.mock(Request.class);
        Mockito.when(req.queryParams("name")).thenReturn("Bob");
        Mockito.when(req.queryParams("Age")).thenReturn("21");
        Mockito.when(req.queryParams()).thenReturn(new HashSet<String>() {
            {
                add("name");
                add("Age");
            }
        });

        // Test
        final String nameResult = GeneralUtils.findRequestParamIgnoreCase(req, "NAME");

        // Assertions
        Assert.assertNotNull(nameResult);
        Assert.assertEquals("Bob", nameResult);

        // Test
        final String ageResult = GeneralUtils.findRequestParamIgnoreCase(req, "age");

        // Assertions
        Assert.assertNotNull(ageResult);
        Assert.assertEquals("21", ageResult);
    }

    @Test
    public void findPathVarIgnoreCaseTest() {

        // Setup
        final Request req = Mockito.mock(Request.class);
        Mockito.when(req.params()).thenReturn(new HashMap<String, String>() {
            {
                put(":name", "Bob");
                put(":Age", "21");
            }
        });

        // Test
        final String nameResult = GeneralUtils.findPathVarIgnoreCase(req, "NAME");

        // Assertions
        Assert.assertNotNull(nameResult);
        Assert.assertEquals("Bob", nameResult);

        // Test
        final String ageResult = GeneralUtils.findPathVarIgnoreCase(req, "age");

        // Assertions
        Assert.assertNotNull(ageResult);
        Assert.assertEquals("21", ageResult);
    }

    @Test
    public void prefixPath_AddPrefix_Test() {
        Assert.assertEquals("/xxx", GeneralUtils.prefixPath("xxx"));
    }

    @Test
    public void prefixPath_PrefixAlreadyExists_Test() {
        Assert.assertEquals("/xxx", GeneralUtils.prefixPath("/xxx"));
    }

    @Test
    public void prefixPath_Null_Test() {
        Assert.assertNull(GeneralUtils.prefixPath(null));
    }

    @Test
    public void prefixPath_Blank_Test() {
        Assert.assertNull(GeneralUtils.prefixPath(""));
    }

    @Test
    public void prefixPath_Empty_Test() {
        Assert.assertNull(GeneralUtils.prefixPath("   "));
    }

    @Test
    public void exactVersionNo_FullInfoTest() {
        Assert.assertEquals(121, GeneralUtils.exactVersionNo("1.2.1-SNAPSHOT"));
    }

    @Test
    public void exactVersionNo_FullInfoCaseTest() {
        Assert.assertEquals(124, GeneralUtils.exactVersionNo("1.2.4-SnapShot"));
    }

    @Test
    public void exactVersionNo_PartialInfoTest() {
        Assert.assertEquals(152, GeneralUtils.exactVersionNo("1.5.2"));
    }

    @Test
    public void exactVersionNo_NullTest() {

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("versionNo is not defined");

        GeneralUtils.exactVersionNo(null);
    }

    @Test
    public void exactVersionNo_InvalidFormatTest() {

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("extracted versionNo is not a valid number: Mambo no 5!");

        GeneralUtils.exactVersionNo("Mambo no 5!");
    }

    @Test
    public void deserialiseJSON_SimpleParse_Test() {

        // Test
        final Map<String, ?> jsonObj = GeneralUtils.deserialiseJSON("{ \"name\" : \"bob\", \"age\" : 21 }");

        // Assertions
        Assert.assertNotNull(jsonObj);
        Assert.assertTrue(jsonObj.containsKey("name"));
        Assert.assertTrue(jsonObj.containsKey("age"));
        Assert.assertEquals("bob", jsonObj.get("name"));
        Assert.assertEquals(21, jsonObj.get("age"));
    }

    @Test
    public void deserialiseJSON_InvalidJson_Test() {
        Assert.assertNull(GeneralUtils.deserialiseJSON("{ \"name\" :, \"age\" : 21 }"));
    }

    @Test
    public void deserialiseJSON_Null_Test() {
        Assert.assertNull(GeneralUtils.deserialiseJSON(null));
    }

    @Test
    public void removeAllLineBreaks_Test() {
        final String lb = System.getProperty("line.separator");
        Assert.assertEquals("{ \"name\" :\"John Smith\", \"age\":21}", GeneralUtils.removeAllLineBreaks("{"+lb+" \"name\" :\"John Smith\","+lb+" \"age\":21"+lb+"}"));
    }

    @Test
    public void removeAllLineBreaks_NoChange_Test() {
        Assert.assertEquals("{ \"name\" :\"Max\", \"age\" : 21 }", GeneralUtils.removeAllLineBreaks("{ \"name\" :\"Max\", \"age\" : 21 }"));
    }

    @Test
    public void removeAllLineBreaks_Null_Test() {
        Assert.assertNull(GeneralUtils.removeAllLineBreaks(null));
    }

    @Test
    public void removeAllLineBreaks_Blank_Test() {
        Assert.assertEquals("", GeneralUtils.removeAllLineBreaks(""));
    }

}
