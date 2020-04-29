package com.smockin.mockserver.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.smockin.admin.persistence.entity.RestfulMockStatefulMeta;
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
    public void findStateRecordPath_inMultipleMapRecords_Test() {

        // Setup
        final String json = "[{\"jsonapi\":{\"version\":\"1.0\"},\"data\":{\"id\":\"1\",\"type\":\"customers\",\"name\":\"Bob\"},\"included\":[]},{\"jsonapi\":{\"version\":\"1.0\"},\"data\":{\"id\":\"2\",\"type\":\"customers\",\"name\":\"Mike\"},\"included\":[]},{\"jsonapi\":{\"version\":\"1.0\"},\"data\":{\"id\":\"3\",\"type\":\"customers\",\"name\":\"Pete\"},\"included\":[]}]";

        final List<Map<String, Object>> allState = GeneralUtils.deserialiseJson(json,
                new TypeReference<List<Map<String, Object>>>() {});

        final String[] pathArray = { "data", "id" };
        final String targetId = "2";

        // Test
        final Optional<StatefulServiceImpl.StatefulPath> outcome = statefulServiceImpl.findDataStateRecordPath(allState, pathArray, targetId);

        // Assertions
        Assert.assertTrue(outcome.isPresent());
        Assert.assertEquals("[1].data.id=2", outcome.get().getPath());
        Assert.assertEquals(Integer.valueOf(1), outcome.get().getIndex());

    }

    @Test
    public void findStateRecordPath_inMultipleMapRecordsWithDataLists_Test() {

        // Setup
        final String json = "[{\"jsonapi\":{\"version\":\"1.0\"},\"data\":[{\"id\":\"1\",\"type\":\"customers\",\"name\":\"Bob\"}],\"included\":[]},{\"jsonapi\":{\"version\":\"1.0\"},\"data\":[{\"id\":\"2\",\"type\":\"customers\",\"name\":\"Mike\"}],\"included\":[]},{\"jsonapi\":{\"version\":\"1.0\"},\"data\":[{\"id\":\"3\",\"type\":\"customers\",\"name\":\"Pete\"}],\"included\":[]}]";

        final List<Map<String, Object>> allState = GeneralUtils.deserialiseJson(json,
                new TypeReference<List<Map<String, Object>>>() {});

        final String[] pathArray = { "data", "id" };
        final String targetId = "3";

        // Test
        final Optional<StatefulServiceImpl.StatefulPath> outcome = statefulServiceImpl.findDataStateRecordPath(allState, pathArray, targetId);

        // Assertions
        Assert.assertTrue(outcome.isPresent());
        Assert.assertEquals("[2].data.[0].id=3", outcome.get().getPath());
        Assert.assertEquals(Integer.valueOf(2), outcome.get().getIndex());

    }

    @Test
    public void findStateRecordPath_inSingleDataList_Test() {

        // Setup
        final String json = "[{\"jsonapi\":{\"version\":\"1.0\"},\"data\":[{\"id\":\"1\",\"type\":\"customers\",\"name\":\"Bob\"},{\"id\":\"2\",\"type\":\"customers\",\"name\":\"Billy\"},{\"id\":\"3\",\"type\":\"customers\",\"name\":\"Sally\"},{\"id\":\"4\",\"type\":\"customers\",\"name\":\"Jennifer\"}],\"included\":[]}]";

        final List<Map<String, Object>> allState = GeneralUtils.deserialiseJson(json,
                new TypeReference<List<Map<String, Object>>>() {});

        final String[] pathArray = { "data", "id" };
        final String targetId = "3";

        // Test
        final Optional<StatefulServiceImpl.StatefulPath> outcome = statefulServiceImpl.findDataStateRecordPath(allState, pathArray, targetId);

        // Assertions
        Assert.assertTrue(outcome.isPresent());
        Assert.assertEquals("[0].data.[2].id=3", outcome.get().getPath());
        Assert.assertEquals(Integer.valueOf(0), outcome.get().getIndex());

    }

    @Test
    public void findStateRecordPath_withOddNestedIdJsonStructure_Test() {

        // Setup
        final String json = "[{\"jsonapi\":{\"version\":\"1.0\"},\"data\":[{\"id\":\"1\",\"type\":\"customers\",\"name\":\"Bob\"},{\"id\":\"2\",\"type\":\"customers\",\"name\":\"Billy\"}],\"included\":[]},{\"jsonapi\":{\"version\":\"1.0\"},\"data\":[{\"id\":\"3\",\"type\":\"customers\",\"name\":\"Mike\"}],\"included\":[]}]";

        final List<Map<String, Object>> allState = GeneralUtils.deserialiseJson(json,
                new TypeReference<List<Map<String, Object>>>() {});

        final String[] pathArray = { "data", "id" };
        final String targetId = "2";

        // Test
        final Optional<StatefulServiceImpl.StatefulPath> outcome = statefulServiceImpl.findDataStateRecordPath(allState, pathArray, targetId);

        // Assertions
        Assert.assertTrue(outcome.isPresent());
        Assert.assertEquals("[0].data.[1].id=2", outcome.get().getPath());
        Assert.assertEquals(Integer.valueOf(0), outcome.get().getIndex());

    }

    @Test
    public void findStateRecordPath_withComplexIdJsonStructure_Test() {

        // Setup
        final String json = "[{\"foo1\":\"bar1\",\"foo2\":1,\"foo3\":true,\"jsonapi\":{\"version\":\"1.0\"},\"data\":[{\"data1\":[{\"id\":\"1\",\"type\":\"customers\",\"name\":\"Bob\"}]},{\"data1\":[{\"id\":\"2\",\"type\":\"customers\",\"name\":\"Max\"},{\"id\":\"3\",\"type\":\"customers\",\"name\":\"Jane\"},{\"id\":\"4\",\"type\":\"customers\",\"name\":\"Sam\"}]},{\"data1\":[{\"id\":\"5\",\"type\":\"customers\",\"name\":\"Darren\"},{\"id\":\"6\",\"type\":\"customers\",\"name\":\"Mandy\"}]}],\"included\":[]},{\"foo1\":\"bar2\",\"foo2\":2,\"foo3\":true,\"jsonapi\":{\"version\":\"1.0\"},\"data\":[{\"data1\":[{\"id\":\"7\",\"type\":\"customers\",\"name\":\"Bob\"}]},{\"data1\":[{\"id\":\"8\",\"type\":\"customers\",\"name\":\"Max\"},{\"id\":\"9\",\"type\":\"customers\",\"name\":\"Jane\"},{\"id\":\"10\",\"type\":\"customers\",\"name\":\"Sam\"}]},{\"data1\":[{\"id\":\"11\",\"type\":\"customers\",\"name\":\"Darren\"},{\"id\":\"12\",\"type\":\"customers\",\"name\":\"Mandy\"}]}],\"included\":[]}]";

        final List<Map<String, Object>> allState = GeneralUtils.deserialiseJson(json,
                new TypeReference<List<Map<String, Object>>>() {});

        final String[] pathArray = { "data", "data1", "id" };
        final String targetId = "5";

        // Test
        final Optional<StatefulServiceImpl.StatefulPath> outcome = statefulServiceImpl.findDataStateRecordPath(allState, pathArray, targetId);

        // Assertions
        Assert.assertTrue(outcome.isPresent());
        Assert.assertEquals("[0].data.[2].data1.[0].id=5", outcome.get().getPath());
        Assert.assertEquals(Integer.valueOf(0), outcome.get().getIndex());

    }

    @Test
    public void findStateRecordPath_withComplexIdJsonStructure_Test2() {

        // Setup
        final String json = "[{\"foo1\":\"bar1\",\"foo2\":1,\"foo3\":true,\"jsonapi\":{\"version\":\"1.0\"},\"data\":[{\"data1\":[{\"id\":\"1\",\"type\":\"customers\",\"name\":\"Bob\"}]},{\"data1\":[{\"id\":\"2\",\"type\":\"customers\",\"name\":\"Max\"},{\"id\":\"3\",\"type\":\"customers\",\"name\":\"Jane\"},{\"id\":\"4\",\"type\":\"customers\",\"name\":\"Sam\"}]},{\"data1\":[{\"id\":\"5\",\"type\":\"customers\",\"name\":\"Darren\"},{\"id\":\"6\",\"type\":\"customers\",\"name\":\"Mandy\"}]}],\"included\":[]},{\"foo1\":\"bar2\",\"foo2\":2,\"foo3\":true,\"jsonapi\":{\"version\":\"1.0\"},\"data\":[{\"data1\":[{\"id\":\"7\",\"type\":\"customers\",\"name\":\"Bob\"}]},{\"data1\":[{\"id\":\"8\",\"type\":\"customers\",\"name\":\"Max\"},{\"id\":\"9\",\"type\":\"customers\",\"name\":\"Jane\"},{\"id\":\"10\",\"type\":\"customers\",\"name\":\"Sam\"}]},{\"data1\":[{\"id\":\"11\",\"type\":\"customers\",\"name\":\"Darren\"},{\"id\":\"12\",\"type\":\"customers\",\"name\":\"Mandy\"}]}],\"included\":[]}]";

        final List<Map<String, Object>> allState = GeneralUtils.deserialiseJson(json,
                new TypeReference<List<Map<String, Object>>>() {});

        final String[] pathArray = { "data", "data1", "id" };
        final String targetId = "7";

        // Test
        final Optional<StatefulServiceImpl.StatefulPath> outcome = statefulServiceImpl.findDataStateRecordPath(allState, pathArray, targetId);

        // Assertions
        Assert.assertTrue(outcome.isPresent());
        Assert.assertEquals("[1].data.[0].data1.[0].id=7", outcome.get().getPath());
        Assert.assertEquals(Integer.valueOf(1), outcome.get().getIndex());

    }

    @Test
    public void findStateRecordPath_withComplexIdJsonStructure_Test3() {

        // Setup
        final String json = "[{\"foo1\":\"bar1\",\"foo2\":1,\"foo3\":true,\"jsonapi\":{\"version\":\"1.0\"},\"data\":[{\"data1\":[{\"id\":\"1\",\"type\":\"customers\",\"name\":\"Bob\"}]},{\"data1\":[{\"id\":\"2\",\"type\":\"customers\",\"name\":\"Max\"},{\"id\":\"3\",\"type\":\"customers\",\"name\":\"Jane\"},{\"id\":\"4\",\"type\":\"customers\",\"name\":\"Sam\"}]},{\"data1\":[{\"id\":\"5\",\"type\":\"customers\",\"name\":\"Darren\"},{\"id\":\"6\",\"type\":\"customers\",\"name\":\"Mandy\"}]}],\"included\":[]},{\"foo1\":\"bar2\",\"foo2\":2,\"foo3\":true,\"jsonapi\":{\"version\":\"1.0\"},\"data\":[{\"data1\":[{\"id\":\"7\",\"type\":\"customers\",\"name\":\"Bob\"}]},{\"data1\":[{\"id\":\"8\",\"type\":\"customers\",\"name\":\"Max\"},{\"id\":\"9\",\"type\":\"customers\",\"name\":\"Jane\"},{\"id\":\"10\",\"type\":\"customers\",\"name\":\"Sam\"}]},{\"data1\":[{\"id\":\"11\",\"type\":\"customers\",\"name\":\"Darren\"},{\"id\":\"12\",\"type\":\"customers\",\"name\":\"Mandy\"}]}],\"included\":[]}]";

        final List<Map<String, Object>> allState = GeneralUtils.deserialiseJson(json,
                new TypeReference<List<Map<String, Object>>>() {});

        final String[] pathArray = { "data", "data1", "id" };
        final String targetId = "10";

        // Test
        final Optional<StatefulServiceImpl.StatefulPath> outcome = statefulServiceImpl.findDataStateRecordPath(allState, pathArray, targetId);

        // Assertions
        Assert.assertTrue(outcome.isPresent());
        Assert.assertEquals("[1].data.[1].data1.[2].id=10", outcome.get().getPath());
        Assert.assertEquals(Integer.valueOf(1), outcome.get().getIndex());

    }

    @Test
    public void extractArrayPositionTest() {

        // Test
        final int positionOutcome1 = statefulServiceImpl.extractArrayPosition("[1]");
        final int positionOutcome2 = statefulServiceImpl.extractArrayPosition("[2]");

        // Assertions
        Assert.assertEquals(1, positionOutcome1);
        Assert.assertEquals(2, positionOutcome2);
    }

    @Test
    public void extractArrayPositionNullTest() {

        // Test & Assertions
        Assert.assertNull(statefulServiceImpl.extractArrayPosition(null));
    }

    @Test
    public void extractArrayPositionInvalidCharTest() {

        // Test & Assertions
        Assert.assertNull(statefulServiceImpl.extractArrayPosition("[x]"));
    }

    @Test
    public void extractArrayPositionInvalidPathTest() {

        // Test & Assertions
        Assert.assertNull(statefulServiceImpl.extractArrayPosition("xxx"));
    }

    @Test
    public void findStateRecordByPath_inSingleDataList_Test() {

        // Setup
        final String json = "[{\"jsonapi\":{\"version\":\"1.0\"},\"data\":[{\"id\":\"1\",\"type\":\"customers\",\"name\":\"Bob\"},{\"id\":\"2\",\"type\":\"customers\",\"name\":\"Billy\"},{\"id\":\"3\",\"type\":\"customers\",\"name\":\"Sally\"},{\"id\":\"4\",\"type\":\"customers\",\"name\":\"Jennifer\"}],\"included\":[]}]";

        final List<Map<String, Object>> allState = GeneralUtils.deserialiseJson(json,
                new TypeReference<List<Map<String, Object>>>() {});

        final String jsonPath = "[0].data.[1].id=2";

        // Test
        final Optional<Map<String, Object>> result = statefulServiceImpl.findDataStateRecordByPath(allState, jsonPath);

        // Assertions
        Assert.assertTrue(result.isPresent());
        Assert.assertNotNull(result.get());

        Assert.assertNotNull(result.get().get("data"));
        Assert.assertTrue(result.get().get("data") instanceof List);
        Assert.assertEquals(1, ((List)result.get().get("data")).size());
        Assert.assertEquals("2", ((Map)((List)result.get().get("data")).get(0)).get("id"));
        Assert.assertEquals("Billy", ((Map)((List)result.get().get("data")).get(0)).get("name"));

        // Ensure cached state list remains unmodified.
        Assert.assertEquals(1, allState.size());
        Assert.assertTrue(allState.get(0).get("data") instanceof List);
        Assert.assertEquals(4, ((List)allState.get(0).get("data")).size());

    }

    @Test
    public void findStateRecordByPath_inMultipleMapRecords_Test() {

        // Setup
        final String json = "[{\"jsonapi\":{\"version\":\"1.0\"},\"data\":{\"id\":\"1\",\"type\":\"customers\",\"name\":\"Bob\"},\"included\":[]},{\"jsonapi\":{\"version\":\"1.0\"},\"data\":{\"id\":\"2\",\"type\":\"customers\",\"name\":\"Mike\"},\"included\":[]},{\"jsonapi\":{\"version\":\"1.0\"},\"data\":{\"id\":\"3\",\"type\":\"customers\",\"name\":\"Pete\"},\"included\":[]}]";

        final List<Map<String, Object>> allState = GeneralUtils.deserialiseJson(json,
                new TypeReference<List<Map<String, Object>>>() {});

        final String jsonPath = "[1].data.id=2";

        // Test
        final Optional<Map<String, Object>> result = statefulServiceImpl.findDataStateRecordByPath(allState, jsonPath);

        // Assertions
        Assert.assertTrue(result.isPresent());
        Assert.assertNotNull(result.get());
        Assert.assertNotNull(result.get().get("data"));
        Assert.assertTrue(result.get().get("data") instanceof Map);
        Assert.assertEquals("2", ((Map)result.get().get("data")).get("id"));
        Assert.assertEquals("Mike", ((Map)result.get().get("data")).get("name"));

    }

    @Test
    public void findStateRecordByPath_inMultipleMapRecordsWithDataLists_Test() {

        // Setup
        final String json = "[{\"jsonapi\":{\"version\":\"1.0\"},\"data\":[{\"id\":\"1\",\"type\":\"customers\",\"name\":\"Bob\"}],\"included\":[]},{\"jsonapi\":{\"version\":\"1.0\"},\"data\":[{\"id\":\"2\",\"type\":\"customers\",\"name\":\"Mike\"}],\"included\":[]},{\"jsonapi\":{\"version\":\"1.0\"},\"data\":[{\"id\":\"3\",\"type\":\"customers\",\"name\":\"Pete\"}],\"included\":[]}]";

        final List<Map<String, Object>> allState = GeneralUtils.deserialiseJson(json,
                new TypeReference<List<Map<String, Object>>>() {});

        final String jsonPath = "[2].data.[0].id=3";

        // Test
        final Optional<Map<String, Object>> result = statefulServiceImpl.findDataStateRecordByPath(allState, jsonPath);

        // Assertions
        Assert.assertTrue(result.isPresent());
        Assert.assertNotNull(result.get());
        Assert.assertNotNull(result.get().get("data"));
        Assert.assertTrue(result.get().get("data") instanceof List);
        Assert.assertEquals(1, ((List)result.get().get("data")).size());
        Assert.assertEquals("3", ((Map)((List)result.get().get("data")).get(0)).get("id"));
        Assert.assertEquals("Pete", ((Map)((List)result.get().get("data")).get(0)).get("name"));

    }

    @Test
    public void findStateRecordByPath_withOddNestedIdJsonStructure_Test() {

        // Setup
        final String json = "[{\"jsonapi\":{\"version\":\"1.0\"},\"data\":[{\"id\":\"1\",\"type\":\"customers\",\"name\":\"Bob\"},{\"id\":\"2\",\"type\":\"customers\",\"name\":\"Billy\"}],\"included\":[]},{\"jsonapi\":{\"version\":\"1.0\"},\"data\":[{\"id\":\"3\",\"type\":\"customers\",\"name\":\"Mike\"}],\"included\":[]}]";

        final List<Map<String, Object>> allState = GeneralUtils.deserialiseJson(json,
                new TypeReference<List<Map<String, Object>>>() {});

        final String jsonPath = "[0].data.[1].id=2";

        // Test
        final Optional<Map<String, Object>> result = statefulServiceImpl.findDataStateRecordByPath(allState, jsonPath);

        // Assertions
        Assert.assertTrue(result.isPresent());
        Assert.assertNotNull(result.get());
        Assert.assertNotNull(result.get().get("data"));
        Assert.assertTrue(result.get().get("data") instanceof List);
        Assert.assertEquals(1, ((List)result.get().get("data")).size());
        Assert.assertEquals("2", ((Map)((List)result.get().get("data")).get(0)).get("id"));
        Assert.assertEquals("Billy", ((Map)((List)result.get().get("data")).get(0)).get("name"));

    }

    @Test
    public void findStateRecordByPath_withComplexIdJsonStructure1_Test() {

        // Setup
        final String json = "[{\"foo1\":\"bar1\",\"foo2\":1,\"foo3\":true,\"jsonapi\":{\"version\":\"1.0\"},\"data\":[{\"data1\":[{\"id\":\"1\",\"type\":\"customers\",\"name\":\"Bob\"}]},{\"data1\":[{\"id\":\"2\",\"type\":\"customers\",\"name\":\"Max\"},{\"id\":\"3\",\"type\":\"customers\",\"name\":\"Jane\"},{\"id\":\"4\",\"type\":\"customers\",\"name\":\"Sam\"}]},{\"data1\":[{\"id\":\"5\",\"type\":\"customers\",\"name\":\"Darren\"},{\"id\":\"6\",\"type\":\"customers\",\"name\":\"Mandy\"}]}],\"included\":[]},{\"foo1\":\"bar2\",\"foo2\":2,\"foo3\":true,\"jsonapi\":{\"version\":\"1.0\"},\"data\":[{\"data1\":[{\"id\":\"7\",\"type\":\"customers\",\"name\":\"Bob\"}]},{\"data1\":[{\"id\":\"8\",\"type\":\"customers\",\"name\":\"Max\"},{\"id\":\"9\",\"type\":\"customers\",\"name\":\"Jane\"},{\"id\":\"10\",\"type\":\"customers\",\"name\":\"Sam\"}]},{\"data1\":[{\"id\":\"11\",\"type\":\"customers\",\"name\":\"Darren\"},{\"id\":\"12\",\"type\":\"customers\",\"name\":\"Mandy\"}]}],\"included\":[]}]";

        final List<Map<String, Object>> allState = GeneralUtils.deserialiseJson(json,
                new TypeReference<List<Map<String, Object>>>() {});

        final String jsonPath = "[1].data.[2].data1.[0].id=11";

        // Test
        final Optional<Map<String, Object>> result = statefulServiceImpl.findDataStateRecordByPath(allState, jsonPath);

        // Assertions
        Assert.assertTrue(result.isPresent());
        Assert.assertNotNull(result.get());
        Assert.assertNotNull(result.get().get("data"));
        Assert.assertNotNull(result.get().get("data") instanceof List);
        Assert.assertEquals(1, ((List)result.get().get("data")).size());
        Assert.assertTrue(((List)result.get().get("data")).get(0) instanceof Map);
        Assert.assertTrue(((Map)((List)result.get().get("data")).get(0)).get("data1") instanceof List);
        Assert.assertEquals(1, ((List)((Map)((List)result.get().get("data")).get(0)).get("data1")).size());
        Assert.assertTrue(((List)((Map)((List)result.get().get("data")).get(0)).get("data1")).get(0) instanceof Map);
        Assert.assertEquals("11", ((Map)((List)((Map)((List)result.get().get("data")).get(0)).get("data1")).get(0)).get("id"));
        Assert.assertEquals("Darren", ((Map)((List)((Map)((List)result.get().get("data")).get(0)).get("data1")).get(0)).get("name"));

    }

    @Test
    public void findStateRecordByPath_withComplexIdJsonStructure2_Test() {

        // Setup
        final String json = "[{\"foo1\":\"bar1\",\"foo2\":1,\"foo3\":true,\"jsonapi\":{\"version\":\"1.0\"},\"data\":[{\"data1\":[{\"id\":\"1\",\"type\":\"customers\",\"name\":\"Bob\"}]},{\"data1\":[{\"id\":\"2\",\"type\":\"customers\",\"name\":\"Max\"},{\"id\":\"3\",\"type\":\"customers\",\"name\":\"Jane\"},{\"id\":\"4\",\"type\":\"customers\",\"name\":\"Sam\"}]},{\"data1\":[{\"id\":\"5\",\"type\":\"customers\",\"name\":\"Darren\"},{\"id\":\"6\",\"type\":\"customers\",\"name\":\"Mandy\"}]}],\"included\":[]},{\"foo1\":\"bar2\",\"foo2\":2,\"foo3\":true,\"jsonapi\":{\"version\":\"1.0\"},\"data\":[{\"data1\":[{\"id\":\"7\",\"type\":\"customers\",\"name\":\"Bob\"}]},{\"data1\":[{\"id\":\"8\",\"type\":\"customers\",\"name\":\"Max\"},{\"id\":\"9\",\"type\":\"customers\",\"name\":\"Jane\"},{\"id\":\"10\",\"type\":\"customers\",\"name\":\"Sam\"}]},{\"data1\":[{\"id\":\"11\",\"type\":\"customers\",\"name\":\"Darren\"},{\"id\":\"12\",\"type\":\"customers\",\"name\":\"Mandy\"}]}],\"included\":[]}]";

        final List<Map<String, Object>> allState = GeneralUtils.deserialiseJson(json,
                new TypeReference<List<Map<String, Object>>>() {});

        final String jsonPath = "[1].data.[1].data1.[1].id=9";

        // Test
        final Optional<Map<String, Object>> result = statefulServiceImpl.findDataStateRecordByPath(allState, jsonPath);

        // Assertions
        Assert.assertTrue(result.isPresent());
        Assert.assertNotNull(result.get());
        Assert.assertNotNull(result.get().get("data"));
        Assert.assertNotNull(result.get().get("data") instanceof List);
        Assert.assertEquals(1, ((List)result.get().get("data")).size());
        Assert.assertTrue(((List)result.get().get("data")).get(0) instanceof Map);
        Assert.assertTrue(((Map)((List)result.get().get("data")).get(0)).get("data1") instanceof List);
        Assert.assertEquals(1, ((List)((Map)((List)result.get().get("data")).get(0)).get("data1")).size());
        Assert.assertTrue(((List)((Map)((List)result.get().get("data")).get(0)).get("data1")).get(0) instanceof Map);
        Assert.assertEquals("9", ((Map)((List)((Map)((List)result.get().get("data")).get(0)).get("data1")).get(0)).get("id"));
        Assert.assertEquals("Jane", ((Map)((List)((Map)((List)result.get().get("data")).get(0)).get("data1")).get(0)).get("name"));

    }

    @Test
    public void findStateRecordByPath_IdMisMatch_Test() {

        // Setup
        final String json = "[{\"jsonapi\":{\"version\":\"1.0\"},\"data\":[{\"id\":\"1\",\"type\":\"customers\",\"name\":\"Bob\"}],\"included\":[]},{\"jsonapi\":{\"version\":\"1.0\"},\"data\":[{\"id\":\"2\",\"type\":\"customers\",\"name\":\"Mike\"}],\"included\":[]},{\"jsonapi\":{\"version\":\"1.0\"},\"data\":[{\"id\":\"3\",\"type\":\"customers\",\"name\":\"Pete\"}],\"included\":[]}]";

        final List<Map<String, Object>> allState = GeneralUtils.deserialiseJson(json,
                new TypeReference<List<Map<String, Object>>>() {});

        final String jsonPath = "[2].data.[0].id=2";

        // Test
        final Optional<Map<String, Object>> result = statefulServiceImpl.findDataStateRecordByPath(allState, jsonPath);

        // Assertions
        Assert.assertFalse(result.isPresent());

    }

    @Test
    public void findDataStateRecordTest() {

        // Setup
        final String json = "[{\"jsonapi\":{\"version\":\"1.0\"},\"data\":{\"id\":\"1\",\"type\":\"customers\",\"name\":\"Bob\"},\"included\":[]},{\"jsonapi\":{\"version\":\"1.0\"},\"data\":{\"id\":\"2\",\"type\":\"customers\",\"name\":\"Mike\"},\"included\":[]},{\"jsonapi\":{\"version\":\"1.0\"},\"data\":{\"id\":\"3\",\"type\":\"customers\",\"name\":\"Pete\"},\"included\":[]}]";

        final List<Map<String, Object>> allState = GeneralUtils.deserialiseJson(json,
                new TypeReference<List<Map<String, Object>>>() {});

        // Test
        final Optional<Map<String, Object>> record = statefulServiceImpl.findDataStateRecord(allState, "data.id", "2");

        // Assertions
        Assert.assertTrue(record.isPresent());
        Assert.assertNotNull(record.get());
        Assert.assertTrue(record.get().get("data") instanceof Map);
        Assert.assertEquals("2", ((Map)record.get().get("data")).get("id"));
        Assert.assertEquals("Mike", ((Map)record.get().get("data")).get("name"));
    }

    @Test
    public void findDataStateRecord_complexJson_Test() {

        // Setup
        final String json = "[{\"version\":1,\"system\":\"Foo1\",\"active\":true,\"data\":[{\"type\":\"customers\",\"meta\":null,\"keys\":[{\"name\":\"Sian\"}]}],\"included\":[]},{\"version\":1,\"system\":\"Foo2\",\"active\":true,\"data\":[{\"type\":\"customers\",\"meta\":[\"A\",\"B\",\"C\"],\"keys\":[{\"name\":\"Sam\"}]}],\"included\":[]},{\"version\":1,\"system\":\"Foo3\",\"active\":true,\"data\":[{\"type\":\"customers\",\"meta\":[\"A\",\"C\"],\"keys\":[{\"name\":\"Will\"}]}],\"included\":[]},{\"version\":1,\"system\":\"Foo4\",\"active\":true,\"data\":[{\"type\":\"customers\",\"meta\":null,\"keys\":[{\"name\":\"Billy\"}]}],\"included\":[]}]";

        final List<Map<String, Object>> allState = GeneralUtils.deserialiseJson(json,
                new TypeReference<List<Map<String, Object>>>() {});

        // Test
        final Optional<Map<String, Object>> record = statefulServiceImpl.findDataStateRecord(allState, "data.keys.name", "Will");

        // Assertions
        Assert.assertTrue(record.isPresent());
        Assert.assertNotNull(record.get());
        Assert.assertTrue(record.get().get("data") instanceof List);
        Assert.assertEquals(1, ((List)record.get().get("data")).size());
        Assert.assertTrue(((List)record.get().get("data")).get(0) instanceof Map);
        Assert.assertTrue(((Map)((List)record.get().get("data")).get(0)).get("keys") instanceof List);
        Assert.assertEquals(1, ((List)((Map)((List)record.get().get("data")).get(0)).get("keys")).size());
        Assert.assertTrue(((List)((Map)((List)record.get().get("data")).get(0)).get("keys")).get(0) instanceof Map);
        Assert.assertEquals("Will", ((Map)((List)((Map)((List)record.get().get("data")).get(0)).get("keys")).get(0)).get("name") );
    }

    @Test
    public void appendIdToJson_noIdPresentInComplexJson_Test() {

        // Setup
        final String json = "{\"version\":1,\"system\":\"Foo2\",\"active\":true,\"data\":[{\"type\":\"customers\",\"meta\":[\"A\",\"B\",\"C\"],\"keys\":[{\"name\":\"Sam\"}]}],\"included\":[]}";

        final Map<String, Object> newState = GeneralUtils.deserialiseJson(json,
                new TypeReference<Map<String, Object>>() {});
        final RestfulMockStatefulMeta restfulMockStatefulMeta = new RestfulMockStatefulMeta();
        restfulMockStatefulMeta.setIdFieldName("id");
        restfulMockStatefulMeta.setIdFieldLocation("data.keys.id");

        // Test
        statefulServiceImpl.appendIdToJson(newState, restfulMockStatefulMeta);

        // Assertions
        Assert.assertNotNull(newState);
        Assert.assertNotNull(newState.get("data"));
        Assert.assertTrue(newState.get("data") instanceof List);
        Assert.assertEquals(1, ((List)newState.get("data")).size());
        Assert.assertNotNull(((List)newState.get("data")).get(0));
        Assert.assertTrue(((List)newState.get("data")).get(0) instanceof Map);
        Assert.assertTrue(((Map)((List)newState.get("data")).get(0)).containsKey("keys"));
        Assert.assertNotNull(((Map)((List)newState.get("data")).get(0)).get("keys"));
        Assert.assertTrue(((Map)((List)newState.get("data")).get(0)).get("keys") instanceof List);
        Assert.assertEquals(1, ((List)((Map)((List)newState.get("data")).get(0)).get("keys")).size());
        Assert.assertTrue(((List)((Map)((List)newState.get("data")).get(0)).get("keys")).get(0) instanceof Map);
        Assert.assertTrue(((Map)((List)((Map)((List)newState.get("data")).get(0)).get("keys")).get(0)).containsKey("id"));
        Assert.assertNotNull(((Map)((List)((Map)((List)newState.get("data")).get(0)).get("keys")).get(0)).get("id"));
    }

    @Test
    public void appendIdToJson_idAlreadyPresentInComplexJson_Test() {

        // Setup
        final String existingId = "12345";
        final String json = "{\"version\":1,\"system\":\"Foo2\",\"active\":true,\"data\":[{\"type\":\"customers\",\"meta\":[\"A\",\"B\",\"C\"],\"keys\":[{\"id\":\""+ existingId +"\",\"name\":\"Sam\"}]}],\"included\":[]}";

        final Map<String, Object> newState = GeneralUtils.deserialiseJson(json,
                new TypeReference<Map<String, Object>>() {});
        final RestfulMockStatefulMeta restfulMockStatefulMeta = new RestfulMockStatefulMeta();
        restfulMockStatefulMeta.setIdFieldName("id");
        restfulMockStatefulMeta.setIdFieldLocation("data.keys.id");

        // Test
        statefulServiceImpl.appendIdToJson(newState, restfulMockStatefulMeta);

        // Assertions
        Assert.assertNotNull(newState);
        Assert.assertNotNull(newState.get("data"));
        Assert.assertTrue(newState.get("data") instanceof List);
        Assert.assertEquals(1, ((List)newState.get("data")).size());
        Assert.assertNotNull(((List)newState.get("data")).get(0));
        Assert.assertTrue(((List)newState.get("data")).get(0) instanceof Map);
        Assert.assertTrue(((Map)((List)newState.get("data")).get(0)).containsKey("keys"));
        Assert.assertNotNull(((Map)((List)newState.get("data")).get(0)).get("keys"));
        Assert.assertTrue(((Map)((List)newState.get("data")).get(0)).get("keys") instanceof List);
        Assert.assertEquals(1, ((List)((Map)((List)newState.get("data")).get(0)).get("keys")).size());
        Assert.assertTrue(((List)((Map)((List)newState.get("data")).get(0)).get("keys")).get(0) instanceof Map);
        Assert.assertTrue(((Map)((List)((Map)((List)newState.get("data")).get(0)).get("keys")).get(0)).containsKey("id"));
        Assert.assertEquals(existingId, ((Map)((List)((Map)((List)newState.get("data")).get(0)).get("keys")).get(0)).get("id"));
    }

    @Test
    public void appendIdToJson_noIdPresentInSimpleFlatJson_Test() {

        // Setup
        final String json = "{\"version\":1,\"system\":\"Foo2\",\"active\":true,\"name\":\"Sam\"}";

        final Map<String, Object> newState = GeneralUtils.deserialiseJson(json,
                new TypeReference<Map<String, Object>>() {});
        final RestfulMockStatefulMeta restfulMockStatefulMeta = new RestfulMockStatefulMeta();
        restfulMockStatefulMeta.setIdFieldName("id");

        // Test
        statefulServiceImpl.appendIdToJson(newState, restfulMockStatefulMeta);

        // Assertions
        Assert.assertNotNull(newState);
        Assert.assertTrue(newState.containsKey("id"));
        Assert.assertNotNull(newState.get("id"));
    }

    @Test
    public void appendIdToJson_idAlreadyPresentInSimpleFlatJson_Test() {

        // Setup
        final String existingId = "12345";
        final String json = "{\"version\":1,\"system\":\"Foo2\",\"active\":true,\"id\":\""+existingId+"\",\"name\":\"Sam\"}";

        final Map<String, Object> newState = GeneralUtils.deserialiseJson(json,
                new TypeReference<Map<String, Object>>() {});
        final RestfulMockStatefulMeta restfulMockStatefulMeta = new RestfulMockStatefulMeta();
        restfulMockStatefulMeta.setIdFieldName("id");

        // Test
        statefulServiceImpl.appendIdToJson(newState, restfulMockStatefulMeta);

        // Assertions
        Assert.assertNotNull(newState);
        Assert.assertTrue(newState.containsKey("id"));
        Assert.assertNotNull(newState.get("id"));
        Assert.assertEquals(existingId, newState.get("id"));
    }

}
