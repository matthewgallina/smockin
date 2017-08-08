package com.smockin.admin.service.utils;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import spark.Request;

import java.util.HashSet;

/**
 * Created by mgallina on 08/08/17.
 */
public class GeneralUtilsTest {

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
        final String result = GeneralUtils.findFirstInboundParamMatch("hello ${REQ_HEAD joe }. how are you?");
        Assert.assertEquals("REQ_HEAD joe ", result);
    }

    @Test
    public void findFirstInboundParamMatch_singleTokenWithSpaces_Test() {
        final String result = GeneralUtils.findFirstInboundParamMatch("hello ${  REQ_HEAD   joe   }. how are you?");
        Assert.assertEquals("  REQ_HEAD   joe   ", result);
    }

    @Test
    public void findFirstInboundParamMatch_multiToken_Test() {
        final String result = GeneralUtils.findFirstInboundParamMatch("hello ${REQ_HEAD max}. how are you ${REQ_HEAD bob }?");
        Assert.assertEquals("REQ_HEAD max", result);
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

        final Request req = Mockito.mock(Request.class);
        Mockito.when(req.headers("name")).thenReturn("Bob");
        Mockito.when(req.headers("Age")).thenReturn("21");
        Mockito.when(req.headers()).thenReturn(new HashSet<String>() {
            {
                add("name");
                add("Age");
            }
        });

        final String nameResult = GeneralUtils.findHeaderIgnoreCase(req, "NAME");
        Assert.assertNotNull(nameResult);
        Assert.assertEquals("Bob", nameResult);

        final String ageResult = GeneralUtils.findHeaderIgnoreCase(req, "age");
        Assert.assertNotNull(ageResult);
        Assert.assertEquals("21", ageResult);
    }

}
