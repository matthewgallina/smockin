package com.smockin.mockserver.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.smockin.utils.GeneralUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class StatefulServiceTest {

    private StatefulServiceImpl statefulServiceImpl;

    @Before
    public void setUp() {

        statefulServiceImpl = new StatefulServiceImpl();

    }

    @Test
    public void findStateRecord_inMultipleMapRecords_Test() {

        // Setup
        final String json = "[{\"jsonapi\":{\"version\":\"1.0\"},\"data\":{\"id\":\"1\",\"type\":\"customers\",\"name\":\"Bob\"},\"included\":[]},{\"jsonapi\":{\"version\":\"1.0\"},\"data\":{\"id\":\"2\",\"type\":\"customers\",\"name\":\"Mike\"},\"included\":[]},{\"jsonapi\":{\"version\":\"1.0\"},\"data\":{\"id\":\"3\",\"type\":\"customers\",\"name\":\"Pete\"},\"included\":[]}]";

        final List<Map<String, Object>> allState = GeneralUtils.deserialiseJson(json,
                new TypeReference<List<Map<String, Object>>>() {});

        final String[] pathArray = { "data", "id" };
        final String targetId = "2";

        // Test
        final Optional<String> outcome = statefulServiceImpl.findStateRecord(allState, pathArray, targetId);

        // Assertions
        Assert.assertTrue(outcome.isPresent());
        Assert.assertEquals("state[1].data.id=2", outcome.get());

        /*
        System.out.println(state);
        Assert.assertNotNull(state);
        Assert.assertNotNull(state.get("data"));
        Assert.assertNotNull(state.get("data") instanceof Map);
        Assert.assertNotNull(((Map)state.get("data")).get("id"));
        Assert.assertNotNull(targetId, ((Map)state.get("data")).get("id"));
        Assert.assertNotNull("Mike", ((Map)state.get("data")).get("name"));
        */
    }

    @Test
    public void findStateRecord_inMultipleMapRecordsWithDataLists_Test() {

        // Setup
        final String json = "[{\"jsonapi\":{\"version\":\"1.0\"},\"data\":[{\"id\":\"1\",\"type\":\"customers\",\"name\":\"Bob\"}],\"included\":[]},{\"jsonapi\":{\"version\":\"1.0\"},\"data\":[{\"id\":\"2\",\"type\":\"customers\",\"name\":\"Mike\"}],\"included\":[]},{\"jsonapi\":{\"version\":\"1.0\"},\"data\":[{\"id\":\"3\",\"type\":\"customers\",\"name\":\"Pete\"}],\"included\":[]}]";

        final List<Map<String, Object>> allState = GeneralUtils.deserialiseJson(json,
                new TypeReference<List<Map<String, Object>>>() {});

        final String[] pathArray = { "data", "id" };
        final String targetId = "3";

        // Test
        final Optional<String> outcome = statefulServiceImpl.findStateRecord(allState, pathArray, targetId);

        // Assertions
        Assert.assertTrue(outcome.isPresent());
        Assert.assertEquals("state[2].data[0].id=3", outcome.get());

        // Assertions
        /*
        System.out.println(state);
        Assert.assertNotNull(state);
        Assert.assertNotNull(state.get("data"));
        Assert.assertTrue(state.get("data") instanceof List);
        final List<Map<String, Object>> records = (List)state.get("data");
        Assert.assertEquals(1, records.size());
        final Map<String, Object> record = records.get(0);
        Assert.assertNotNull(record.get("id"));
        Assert.assertNotNull(targetId, record.get("id"));
        Assert.assertNotNull("Pete", record.get("name"));
        */
    }

    @Test
    public void findStateRecord_inSingleDataList_Test() {

        // Setup
        final String json = "[{\"jsonapi\":{\"version\":\"1.0\"},\"data\":[{\"id\":\"1\",\"type\":\"customers\",\"name\":\"Bob\"},{\"id\":\"2\",\"type\":\"customers\",\"name\":\"Billy\"},{\"id\":\"3\",\"type\":\"customers\",\"name\":\"Sally\"},{\"id\":\"4\",\"type\":\"customers\",\"name\":\"Jennifer\"}],\"included\":[]}]";

        final List<Map<String, Object>> allState = GeneralUtils.deserialiseJson(json,
                new TypeReference<List<Map<String, Object>>>() {});

        final String[] pathArray = { "data", "id" };
        final String targetId = "3";

        // Test
        final Optional<String> outcome = statefulServiceImpl.findStateRecord(allState, pathArray, targetId);

        // Assertions
        Assert.assertTrue(outcome.isPresent());
        Assert.assertEquals("state[0].data[2].id=3", outcome.get());

        /*
        // Assertions
        System.out.println(state);
        Assert.assertNotNull(state);
        Assert.assertNotNull(state.get("data"));
        Assert.assertNotNull(state.get("data") instanceof List);
        Assert.assertEquals(1, ((List)state.get("data")).size());
        Assert.assertEquals(targetId, ((Map)((List)state.get("data")).get(0)).get("id"));
        Assert.assertNotNull("Sally", ((Map)state.get("data")).get("name"));
        */
    }

    @Test
    public void findStateRecord_withOddNestedIdJsonStructure_Test() {

        // Setup
        final String json = "[{\"jsonapi\":{\"version\":\"1.0\"},\"data\":[{\"id\":\"1\",\"type\":\"customers\",\"name\":\"Bob\"},{\"id\":\"2\",\"type\":\"customers\",\"name\":\"Billy\"}],\"included\":[]},{\"jsonapi\":{\"version\":\"1.0\"},\"data\":[{\"id\":\"3\",\"type\":\"customers\",\"name\":\"Mike\"}],\"included\":[]}]";

        final List<Map<String, Object>> allState = GeneralUtils.deserialiseJson(json,
                new TypeReference<List<Map<String, Object>>>() {});

        final String[] pathArray = { "data", "id" };
        final String targetId = "2";

        // Test
        final Optional<String> outcome = statefulServiceImpl.findStateRecord(allState, pathArray, targetId);

        // Assertions
        Assert.assertTrue(outcome.isPresent());
        Assert.assertEquals("state[0].data[1].id=2", outcome.get());

        /*
        // Assertions
        System.out.println(state);
        Assert.assertNotNull(state);
        Assert.assertNotNull(state.get("data"));
        Assert.assertNotNull(state.get("data") instanceof List);
        Assert.assertEquals(1, ((List)state.get("data")).size());
        Assert.assertEquals(targetId, ((Map)((List)state.get("data")).get(0)).get("id"));
        Assert.assertEquals("Billy", ((Map)((List)state.get("name")).get(0)).get("id"));
        */

    }

    @Test
    public void findStateRecord_withComplexIdJsonStructure_Test() {

        // Setup
        final String json = "[{\"foo1\":\"bar1\",\"foo2\":1,\"foo3\":true,\"jsonapi\":{\"version\":\"1.0\"},\"data\":[{\"data1\":[{\"id\":\"1\",\"type\":\"customers\",\"name\":\"Bob\"}]},{\"data1\":[{\"id\":\"2\",\"type\":\"customers\",\"name\":\"Max\"},{\"id\":\"3\",\"type\":\"customers\",\"name\":\"Jane\"},{\"id\":\"4\",\"type\":\"customers\",\"name\":\"Sam\"}]},{\"data1\":[{\"id\":\"5\",\"type\":\"customers\",\"name\":\"Darren\"},{\"id\":\"6\",\"type\":\"customers\",\"name\":\"Mandy\"}]}],\"included\":[]},{\"foo1\":\"bar2\",\"foo2\":2,\"foo3\":true,\"jsonapi\":{\"version\":\"1.0\"},\"data\":[{\"data1\":[{\"id\":\"7\",\"type\":\"customers\",\"name\":\"Bob\"}]},{\"data1\":[{\"id\":\"8\",\"type\":\"customers\",\"name\":\"Max\"},{\"id\":\"9\",\"type\":\"customers\",\"name\":\"Jane\"},{\"id\":\"10\",\"type\":\"customers\",\"name\":\"Sam\"}]},{\"data1\":[{\"id\":\"11\",\"type\":\"customers\",\"name\":\"Darren\"},{\"id\":\"12\",\"type\":\"customers\",\"name\":\"Mandy\"}]}],\"included\":[]}]";

        final List<Map<String, Object>> allState = GeneralUtils.deserialiseJson(json,
                new TypeReference<List<Map<String, Object>>>() {});

        final String[] pathArray = { "data", "data1", "id" };
        final String targetId = "5";

        // Test
        final Optional<String> outcome = statefulServiceImpl.findStateRecord(allState, pathArray, targetId);

        // Assertions
        Assert.assertTrue(outcome.isPresent());
        Assert.assertEquals("state[0].data[2].data1[0].id=5", outcome.get());


        /*
        // Assertions
System.out.println(state);
        Assert.assertNotNull(state);
        Assert.assertNotNull(state.get("data"));
        Assert.assertNotNull(state.get("data") instanceof List);
        Assert.assertEquals(1, ((List)state.get("data")).size());
        Assert.assertTrue((((List)state.get("data")).get(0)) instanceof List);
        Assert.assertEquals(1, ((List)(((List)state.get("data")).get(0))).size());
        Assert.assertTrue(((List)(((List)state.get("data")).get(0))).get(0) instanceof Map);
        Assert.assertEquals("Darren", ((Map)((List)(((List)state.get("data")).get(0))).get(0)).get("name"));
        */
    }

    @Test
    public void findStateRecord_withComplexIdJsonStructure_Test2() {

        // Setup
        final String json = "[{\"foo1\":\"bar1\",\"foo2\":1,\"foo3\":true,\"jsonapi\":{\"version\":\"1.0\"},\"data\":[{\"data1\":[{\"id\":\"1\",\"type\":\"customers\",\"name\":\"Bob\"}]},{\"data1\":[{\"id\":\"2\",\"type\":\"customers\",\"name\":\"Max\"},{\"id\":\"3\",\"type\":\"customers\",\"name\":\"Jane\"},{\"id\":\"4\",\"type\":\"customers\",\"name\":\"Sam\"}]},{\"data1\":[{\"id\":\"5\",\"type\":\"customers\",\"name\":\"Darren\"},{\"id\":\"6\",\"type\":\"customers\",\"name\":\"Mandy\"}]}],\"included\":[]},{\"foo1\":\"bar2\",\"foo2\":2,\"foo3\":true,\"jsonapi\":{\"version\":\"1.0\"},\"data\":[{\"data1\":[{\"id\":\"7\",\"type\":\"customers\",\"name\":\"Bob\"}]},{\"data1\":[{\"id\":\"8\",\"type\":\"customers\",\"name\":\"Max\"},{\"id\":\"9\",\"type\":\"customers\",\"name\":\"Jane\"},{\"id\":\"10\",\"type\":\"customers\",\"name\":\"Sam\"}]},{\"data1\":[{\"id\":\"11\",\"type\":\"customers\",\"name\":\"Darren\"},{\"id\":\"12\",\"type\":\"customers\",\"name\":\"Mandy\"}]}],\"included\":[]}]";

        final List<Map<String, Object>> allState = GeneralUtils.deserialiseJson(json,
                new TypeReference<List<Map<String, Object>>>() {});

        final String[] pathArray = { "data", "data1", "id" };
        final String targetId = "7";

        // Test
        final Optional<String> outcome = statefulServiceImpl.findStateRecord(allState, pathArray, targetId);

        // Assertions
        Assert.assertTrue(outcome.isPresent());
        Assert.assertEquals("state[1].data[0].data1[0].id=7", outcome.get());

    }

    @Test
    public void findStateRecord_withComplexIdJsonStructure_Test3() {

        // Setup
        final String json = "[{\"foo1\":\"bar1\",\"foo2\":1,\"foo3\":true,\"jsonapi\":{\"version\":\"1.0\"},\"data\":[{\"data1\":[{\"id\":\"1\",\"type\":\"customers\",\"name\":\"Bob\"}]},{\"data1\":[{\"id\":\"2\",\"type\":\"customers\",\"name\":\"Max\"},{\"id\":\"3\",\"type\":\"customers\",\"name\":\"Jane\"},{\"id\":\"4\",\"type\":\"customers\",\"name\":\"Sam\"}]},{\"data1\":[{\"id\":\"5\",\"type\":\"customers\",\"name\":\"Darren\"},{\"id\":\"6\",\"type\":\"customers\",\"name\":\"Mandy\"}]}],\"included\":[]},{\"foo1\":\"bar2\",\"foo2\":2,\"foo3\":true,\"jsonapi\":{\"version\":\"1.0\"},\"data\":[{\"data1\":[{\"id\":\"7\",\"type\":\"customers\",\"name\":\"Bob\"}]},{\"data1\":[{\"id\":\"8\",\"type\":\"customers\",\"name\":\"Max\"},{\"id\":\"9\",\"type\":\"customers\",\"name\":\"Jane\"},{\"id\":\"10\",\"type\":\"customers\",\"name\":\"Sam\"}]},{\"data1\":[{\"id\":\"11\",\"type\":\"customers\",\"name\":\"Darren\"},{\"id\":\"12\",\"type\":\"customers\",\"name\":\"Mandy\"}]}],\"included\":[]}]";

        final List<Map<String, Object>> allState = GeneralUtils.deserialiseJson(json,
                new TypeReference<List<Map<String, Object>>>() {});

        final String[] pathArray = { "data", "data1", "id" };
        final String targetId = "10";

        // Test
        final Optional<String> outcome = statefulServiceImpl.findStateRecord(allState, pathArray, targetId);

        // Assertions
        Assert.assertTrue(outcome.isPresent());
        Assert.assertEquals("state[1].data[1].data1[2].id=10", outcome.get());

    }

}
