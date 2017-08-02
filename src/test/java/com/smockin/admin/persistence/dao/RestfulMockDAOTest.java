package com.smockin.admin.persistence.dao;

import com.smockin.SmockinTestConfig;
import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.admin.persistence.enums.MockTypeEnum;
import com.smockin.admin.persistence.enums.RecordStatusEnum;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Created by mgallina.
 */
@RunWith(SpringRunner.class)
@DataJpaTest
@ContextConfiguration(classes = {SmockinTestConfig.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class RestfulMockDAOTest {

    @Autowired
    private RestfulMockDAO restfulMockDAO;

    private RestfulMock a, b, c, d, e;

    @Before
    public void setUp() {

        a = buildRestfulMock("/a", MockTypeEnum.SEQ, 6, RestMethodEnum.GET, RecordStatusEnum.ACTIVE);
        b = buildRestfulMock("/b", MockTypeEnum.SEQ, 2, RestMethodEnum.GET, RecordStatusEnum.ACTIVE);
        c = buildRestfulMock("/c", MockTypeEnum.SEQ, 3, RestMethodEnum.GET, RecordStatusEnum.ACTIVE);
        d = buildRestfulMock("/d", MockTypeEnum.SEQ, 10, RestMethodEnum.GET, RecordStatusEnum.INACTIVE);
        e = buildRestfulMock("/e", MockTypeEnum.SEQ, 1, RestMethodEnum.GET, RecordStatusEnum.ACTIVE);

        a = restfulMockDAO.saveAndFlush(a);
        b = restfulMockDAO.saveAndFlush(b);
        c = restfulMockDAO.saveAndFlush(c);
        d = restfulMockDAO.saveAndFlush(d);
        e = restfulMockDAO.saveAndFlush(e);
    }

    @After
    public void tearDown() {
        restfulMockDAO.deleteAll();
        restfulMockDAO.flush();
    }

    @Test
    public void findAllByStatusTest() {

        final List<RestfulMock> mocks = restfulMockDAO.findAllByStatus(RecordStatusEnum.ACTIVE);

        Assert.assertNotNull(mocks);
        Assert.assertEquals(4, mocks.size());

        int[] expectedLoadOrder = new int[] { 1, 2, 3, 6 };

        int index = 0;

        for (RestfulMock m : mocks) {
            Assert.assertEquals(expectedLoadOrder[index++], m.getInitializationOrder());
        }

    }

    @Test
    public void findAllTest() {

        final List<RestfulMock> mocks = restfulMockDAO.findAll();

        Assert.assertNotNull(mocks);
        Assert.assertEquals(5, mocks.size());

        int[] expectedLoadOrder = new int[] { 1, 2, 3, 6, 10 };

        int index = 0;

        for (RestfulMock m : mocks) {
            Assert.assertEquals(expectedLoadOrder[index++], m.getInitializationOrder());
        }

    }

    private RestfulMock buildRestfulMock(final String path, final MockTypeEnum mockType, final int initOrder, final RestMethodEnum restMethod, final RecordStatusEnum status) {

        final RestfulMock mock = new RestfulMock();
        mock.setPath(path);
        mock.setMockType(mockType);
        mock.setInitializationOrder(initOrder);
        mock.setMethod(restMethod);
        mock.setStatus(status);

        return mock;
    }

}
