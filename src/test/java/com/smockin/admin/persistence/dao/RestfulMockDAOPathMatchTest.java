package com.smockin.admin.persistence.dao;

import com.smockin.SmockinTestUtils;
import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.admin.persistence.enums.RecordStatusEnum;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import com.smockin.admin.persistence.enums.RestMockTypeEnum;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import java.util.Arrays;

/**
 * Created by mgallina.
 */
public class RestfulMockDAOPathMatchTest {

    private RestfulMock a, b, c, d, e;
    private RestfulMockDAOImpl restfulMockDAOImpl;

    @Before
    public void setUp() {

        a = SmockinTestUtils.buildRestfulMock("/js", RestMockTypeEnum.CUSTOM_JS, 1, RestMethodEnum.GET, RecordStatusEnum.ACTIVE, null);
        b = SmockinTestUtils.buildRestfulMock("/js2/{id}", RestMockTypeEnum.CUSTOM_JS, 2, RestMethodEnum.GET, RecordStatusEnum.ACTIVE, null);
        c = SmockinTestUtils.buildRestfulMock("/js3/1", RestMockTypeEnum.CUSTOM_JS, 3, RestMethodEnum.GET, RecordStatusEnum.ACTIVE, null);
        d = SmockinTestUtils.buildRestfulMock("/firstname/{name}/lastname", RestMockTypeEnum.SEQ, 4, RestMethodEnum.GET, RecordStatusEnum.INACTIVE, null);
        e = SmockinTestUtils.buildRestfulMock("/hello/{name}/howareyou/{date}", RestMockTypeEnum.SEQ, 5, RestMethodEnum.GET, RecordStatusEnum.ACTIVE, null);

        restfulMockDAOImpl = new RestfulMockDAOImpl();

    }

    @Test
    public void matchPath_simpleMatch_Test() {

        final RestfulMock loadedMock = restfulMockDAOImpl.matchPath(Arrays.asList(a,b,c,d,e), "/js", false);
        Assert.assertNotNull(loadedMock);
        Assert.assertEquals(a.getPath(), loadedMock.getPath());
    }

    @Test
    public void matchPath_pathVar_Test() {

        final RestfulMock loadedMock = restfulMockDAOImpl.matchPath(Arrays.asList(a,b,c,d,e), "/js2/1", false);
        Assert.assertNotNull(loadedMock);
        Assert.assertEquals(b.getPath(), loadedMock.getPath());
    }

    @Test
    public void matchPath_pathVar2_Test() {

        final RestfulMock loadedMock = restfulMockDAOImpl.matchPath(Arrays.asList(a,b,c,d,e), "/firstname/bob/lastname", false);
        Assert.assertNotNull(loadedMock);
        Assert.assertEquals(d.getPath(), loadedMock.getPath());
    }

    @Test
    public void matchPath_pathVar3_Test() {

        final RestfulMock loadedMock = restfulMockDAOImpl.matchPath(Arrays.asList(a,b,c,d,e), "/hello/mike/howareyou/today", false);
        Assert.assertNotNull(loadedMock);
        Assert.assertEquals(e.getPath(), loadedMock.getPath());
    }

}
