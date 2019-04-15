package com.smockin.admin.service.utils;

import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.admin.persistence.enums.RestMockTypeEnum;
import com.smockin.admin.persistence.enums.RecordStatusEnum;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mgallina.
 */
public class RestfulMockSortingUtilsTest {

    private RestfulMockSortingUtils utils;

    @Before
    public void setUp() {
        utils = new RestfulMockSortingUtils();
    }

    @Test
    public void orderPaths_PathsAndWildcards_Test() {

        // Setup
        final List<RestfulMock> mockList = new ArrayList<RestfulMock>() {
            {
                add(new RestfulMock("/a/1/b/1/c/1/d/1", RestMethodEnum.GET, RecordStatusEnum.ACTIVE, RestMockTypeEnum.SEQ, 0, 0, 0, false, false, false, null, false, 0, 0));
                add(new RestfulMock("/a/*", RestMethodEnum.GET, RecordStatusEnum.ACTIVE, RestMockTypeEnum.SEQ, 0, 0, 0, false, false, false, null, false, 0, 0));
                add(new RestfulMock("/a/1", RestMethodEnum.GET, RecordStatusEnum.ACTIVE, RestMockTypeEnum.SEQ, 0, 0, 0, false, false, false, null, false, 0, 0));
                add(new RestfulMock("/a/*/b/4", RestMethodEnum.GET, RecordStatusEnum.ACTIVE, RestMockTypeEnum.SEQ, 0, 0, 0, false, false, false, null, false, 0, 0));
                add(new RestfulMock("/a/1/b/*", RestMethodEnum.GET, RecordStatusEnum.ACTIVE, RestMockTypeEnum.SEQ, 0, 0, 0, false, false, false, null, false, 0, 0));
                add(new RestfulMock("/a/*/b/*", RestMethodEnum.GET, RecordStatusEnum.ACTIVE, RestMockTypeEnum.SEQ, 0, 0, 0, false, false, false, null, false, 0, 0));
                add(new RestfulMock("/a/1/b/1", RestMethodEnum.GET, RecordStatusEnum.ACTIVE, RestMockTypeEnum.SEQ, 0, 0, 0, false, false, false, null, false, 0, 0));
                add(new RestfulMock("/a/2", RestMethodEnum.GET, RecordStatusEnum.ACTIVE, RestMockTypeEnum.SEQ, 0, 0, 0, false, false, false, null, false, 0, 0));
                add(new RestfulMock("/b/*/c/*", RestMethodEnum.GET, RecordStatusEnum.ACTIVE, RestMockTypeEnum.SEQ, 0, 0, 0, false, false, false, null, false, 0, 0));
                add(new RestfulMock("/b/*", RestMethodEnum.GET, RecordStatusEnum.ACTIVE, RestMockTypeEnum.SEQ, 0, 0, 0, false, false, false, null, false, 0, 0));
                add(new RestfulMock("/a/8/b/*", RestMethodEnum.GET, RecordStatusEnum.ACTIVE, RestMockTypeEnum.SEQ, 0, 0, 0, false, false, false, null, false, 0, 0));
                add(new RestfulMock("/a/1/b/1/c/1", RestMethodEnum.GET, RecordStatusEnum.ACTIVE, RestMockTypeEnum.SEQ, 0, 0, 0, false, false, false, null, false, 0, 0));
                add(new RestfulMock("/b/2", RestMethodEnum.GET, RecordStatusEnum.ACTIVE, RestMockTypeEnum.SEQ, 0, 0, 0, false, false, false, null, false, 0, 0));
                add(new RestfulMock("/a/2/b/*", RestMethodEnum.GET, RecordStatusEnum.ACTIVE, RestMockTypeEnum.SEQ, 0, 0, 0, false, false, false, null, false, 0, 0));
                add(new RestfulMock("/a/3/b/*", RestMethodEnum.GET, RecordStatusEnum.ACTIVE, RestMockTypeEnum.SEQ, 0, 0, 0, false, false, false, null, false, 0, 0));
            }
        };

        final int originalSize = mockList.size();

        // Test
        utils.autoOrderEndpointPaths(mockList);

        // Assertions
        Assert.assertEquals(originalSize, mockList.size());

        Assert.assertEquals("/a/1", mockList.get(0).getPath());
        Assert.assertEquals("/a/1/b/1", mockList.get(1).getPath());
        Assert.assertEquals("/a/1/b/1/c/1", mockList.get(2).getPath());
        Assert.assertEquals("/a/1/b/1/c/1/d/1", mockList.get(3).getPath());
        Assert.assertEquals("/a/1/b/*", mockList.get(4).getPath());
        Assert.assertEquals("/a/2", mockList.get(5).getPath());
        Assert.assertEquals("/a/2/b/*", mockList.get(6).getPath());
        Assert.assertEquals("/a/3/b/*", mockList.get(7).getPath());
        Assert.assertEquals("/a/8/b/*", mockList.get(8).getPath());
        Assert.assertEquals("/a/*/b/4", mockList.get(9).getPath());
        Assert.assertEquals("/a/*/b/*", mockList.get(10).getPath());
        Assert.assertEquals("/a/*", mockList.get(11).getPath());
        Assert.assertEquals("/b/2", mockList.get(12).getPath());
        Assert.assertEquals("/b/*/c/*", mockList.get(13).getPath());
        Assert.assertEquals("/b/*", mockList.get(14).getPath());

    }

    @Test
    public void orderPaths_WildcardsOnly_Test() {

        // Setup
        final List<RestfulMock> mockList = new ArrayList<RestfulMock>() {
            {
                add(new RestfulMock("/a/*", RestMethodEnum.GET, RecordStatusEnum.ACTIVE, RestMockTypeEnum.SEQ, 0, 0, 0, false, false, false, null, false, 0,0));
                add(new RestfulMock("/a/1/b/*", RestMethodEnum.GET, RecordStatusEnum.ACTIVE, RestMockTypeEnum.SEQ, 0, 0, 0, false, false, false, null, false, 0,0));
                add(new RestfulMock("/a/*/b/*", RestMethodEnum.GET, RecordStatusEnum.ACTIVE, RestMockTypeEnum.SEQ, 0, 0, 0, false, false, false, null, false, 0,0));
                add(new RestfulMock("/b/*/c/*", RestMethodEnum.GET, RecordStatusEnum.ACTIVE, RestMockTypeEnum.SEQ, 0, 0, 0, false, false, false, null, false, 0,0));
                add(new RestfulMock("/b/*", RestMethodEnum.GET, RecordStatusEnum.ACTIVE, RestMockTypeEnum.SEQ, 0, 0, 0, false, false, false, null, false, 0,0));
                add(new RestfulMock("/a/2/b/*", RestMethodEnum.GET, RecordStatusEnum.ACTIVE, RestMockTypeEnum.SEQ, 0, 0, 0, false, false, false, null, false, 0,0));
                add(new RestfulMock("/a/3/b/*", RestMethodEnum.GET, RecordStatusEnum.ACTIVE, RestMockTypeEnum.SEQ, 0, 0, 0, false, false, false, null, false, 0,0));
                add(new RestfulMock("/b/1", RestMethodEnum.GET, RecordStatusEnum.ACTIVE, RestMockTypeEnum.SEQ, 0, 0, 0, false, false, false, null, false, 0,0));
            }
        };

        final int originalSize = mockList.size();

        // Test
        utils.autoOrderEndpointPaths(mockList);

        // Assertions
        Assert.assertEquals(originalSize, mockList.size());

        Assert.assertEquals("/a/1/b/*", mockList.get(0).getPath());
        Assert.assertEquals("/a/2/b/*", mockList.get(1).getPath());
        Assert.assertEquals("/a/3/b/*", mockList.get(2).getPath());
        Assert.assertEquals("/a/*/b/*", mockList.get(3).getPath());
        Assert.assertEquals("/a/*", mockList.get(4).getPath());
        Assert.assertEquals("/b/1", mockList.get(5).getPath());
        Assert.assertEquals("/b/*/c/*", mockList.get(6).getPath());
        Assert.assertEquals("/b/*", mockList.get(7).getPath());

    }

}
