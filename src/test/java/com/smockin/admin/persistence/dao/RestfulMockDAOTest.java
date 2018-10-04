package com.smockin.admin.persistence.dao;

import com.smockin.SmockinTestConfig;
import com.smockin.SmockinTestUtils;
import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.persistence.enums.RestMockTypeEnum;
import com.smockin.admin.persistence.enums.RecordStatusEnum;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import com.smockin.admin.persistence.enums.SmockinUserRoleEnum;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

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

    @Autowired
    private SmockinUserDAO smockinUserDAO;

    private RestfulMock a, b, c, d, e, f, g;
    private SmockinUser user;

    @Before
    public void setUp() {

        user = smockinUserDAO.saveAndFlush(SmockinTestUtils.buildSmockinUser());

        a = SmockinTestUtils.buildRestfulMock("/a", RestMockTypeEnum.SEQ, 6, RestMethodEnum.GET, RecordStatusEnum.ACTIVE, user);
        b = SmockinTestUtils.buildRestfulMock("/b", RestMockTypeEnum.SEQ, 2, RestMethodEnum.GET, RecordStatusEnum.ACTIVE, user);
        c = SmockinTestUtils.buildRestfulMock("/c", RestMockTypeEnum.SEQ, 3, RestMethodEnum.GET, RecordStatusEnum.ACTIVE, user);
        d = SmockinTestUtils.buildRestfulMock("/d", RestMockTypeEnum.SEQ, 10, RestMethodEnum.GET, RecordStatusEnum.INACTIVE, user);
        e = SmockinTestUtils.buildRestfulMock("/e", RestMockTypeEnum.SEQ, 1, RestMethodEnum.GET, RecordStatusEnum.ACTIVE, user);
        f = SmockinTestUtils.buildRestfulMock("/f", RestMockTypeEnum.SEQ, 12, RestMethodEnum.GET, RecordStatusEnum.ACTIVE, user);
        g = SmockinTestUtils.buildRestfulMock("/g", RestMockTypeEnum.SEQ, 14, RestMethodEnum.GET, RecordStatusEnum.INACTIVE, user);

        a = restfulMockDAO.saveAndFlush(a);
        b = restfulMockDAO.saveAndFlush(b);
        c = restfulMockDAO.saveAndFlush(c);
        d = restfulMockDAO.saveAndFlush(d);
        e = restfulMockDAO.saveAndFlush(e);
        f = restfulMockDAO.saveAndFlush(f);
        g = restfulMockDAO.saveAndFlush(g);

    }

    @After
    public void tearDown() {
        restfulMockDAO.deleteAll();
        restfulMockDAO.flush();

        smockinUserDAO.deleteAll();
        smockinUserDAO.flush();
    }

    @Test
    public void findAllByStatusTest() {

        final List<RestfulMock> mocks = restfulMockDAO.findAllByStatus(RecordStatusEnum.ACTIVE);

        Assert.assertNotNull(mocks);
        Assert.assertEquals(5, mocks.size());

        int[] expectedLoadOrder = new int[] { 1, 2, 3, 6, 12 };

        int index = 0;

        for (RestfulMock m : mocks) {
            Assert.assertEquals(expectedLoadOrder[index++], m.getInitializationOrder());
        }

    }

    @Test
    public void findAllTest() {

        final List<RestfulMock> mocks = restfulMockDAO.findAll();

        Assert.assertNotNull(mocks);
        Assert.assertEquals(7, mocks.size());

        int[] expectedLoadOrder = new int[] { 1, 2, 3, 6, 10, 12, 14 };

        int index = 0;

        for (RestfulMock m : mocks) {
            Assert.assertEquals(expectedLoadOrder[index++], m.getInitializationOrder());
        }

    }

    @Test(expected = DataIntegrityViolationException.class)
    public void uniquePathMethodAndUserConstraintTest() {

        a.setPath("/foo");
        a.setMethod(RestMethodEnum.GET);
        a.setCreatedBy(user);

        b.setPath("/foo");
        b.setMethod(RestMethodEnum.GET);
        b.setCreatedBy(user);

        restfulMockDAO.saveAndFlush(a);
        restfulMockDAO.saveAndFlush(b);
    }

    @Test
    public void findAllPathDuplicatesTest() {

        SmockinUser userB = SmockinTestUtils.buildSmockinUser();
        userB.setUsername("User_B");
        userB.setRole(SmockinUserRoleEnum.REGULAR);
        userB.setCtxPath(userB.getUsername());
        userB = smockinUserDAO.saveAndFlush(userB);

        SmockinUser userC = SmockinTestUtils.buildSmockinUser();
        userC.setUsername("User_C");
        userC.setRole(SmockinUserRoleEnum.REGULAR);
        userC.setCtxPath(userC.getUsername());
        userC = smockinUserDAO.saveAndFlush(userC);

        SmockinUser userD = SmockinTestUtils.buildSmockinUser();
        userD.setUsername("User_D");
        userD.setRole(SmockinUserRoleEnum.REGULAR);
        userD.setCtxPath(userD.getUsername());
        userD = smockinUserDAO.saveAndFlush(userD);

        SmockinUser userE = SmockinTestUtils.buildSmockinUser();
        userE.setUsername("User_E");
        userE.setRole(SmockinUserRoleEnum.REGULAR);
        userE.setCtxPath(userE.getUsername());
        userE = smockinUserDAO.saveAndFlush(userE);

        SmockinUser userF = SmockinTestUtils.buildSmockinUser();
        userF.setUsername("userF");
        userF.setRole(SmockinUserRoleEnum.ADMIN);
        userF.setCtxPath(userF.getUsername());
        userF = smockinUserDAO.saveAndFlush(userF);

        SmockinUser userG = SmockinTestUtils.buildSmockinUser();
        userG.setUsername("userG");
        userG.setRole(SmockinUserRoleEnum.REGULAR);
        userG.setCtxPath(userG.getUsername());
        userG = smockinUserDAO.saveAndFlush(userG);


        a.setPath("/a");
        a.setCreatedBy(user);
        b.setPath("/b");
        b.setCreatedBy(userB);
        c.setPath("/a");
        c.setCreatedBy(userC);
        d.setPath("/b");
        d.setCreatedBy(userD);
        d.setStatus(RecordStatusEnum.ACTIVE);
        e.setPath("/c");
        e.setCreatedBy(userE);
        f.setPath("/c");
        f.setCreatedBy(userF);
        f.setStatus(RecordStatusEnum.INACTIVE);
        g.setPath("/a");
        g.setCreatedBy(userG);
        g.setStatus(RecordStatusEnum.INACTIVE);

        a = restfulMockDAO.saveAndFlush(a);
        b = restfulMockDAO.saveAndFlush(b);
        c = restfulMockDAO.saveAndFlush(c);
        d = restfulMockDAO.saveAndFlush(d);
        e = restfulMockDAO.saveAndFlush(e);
        f = restfulMockDAO.saveAndFlush(f);
        g = restfulMockDAO.saveAndFlush(g);

        final Map<String, List<RestfulMock>> activeDuplicateMocks = restfulMockDAO.findAllActivePathDuplicates();

        Assert.assertNotNull(activeDuplicateMocks);
        Assert.assertEquals(2, activeDuplicateMocks.size());

        activeDuplicateMocks.entrySet().stream().forEach(m -> {

            if ("/a".equals(m.getKey())) {

                Assert.assertEquals(2, m.getValue().size());

                m.getValue().stream().forEach(r -> {
                    Assert.assertThat(r.getId(), CoreMatchers.anyOf(CoreMatchers.equalTo(a.getId()), CoreMatchers.equalTo(c.getId())));
                });

            } else if ("/b".equals(m.getKey())) {

                Assert.assertEquals(2, m.getValue().size());

                m.getValue().stream().forEach(r -> {
                    Assert.assertThat(r.getId(), CoreMatchers.anyOf(CoreMatchers.equalTo(b.getId()), CoreMatchers.equalTo(d.getId())));
                });

            } else {
                Assert.fail();
            }

        });

    }

}
