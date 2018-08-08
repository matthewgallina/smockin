package com.smockin.admin.service;

import com.smockin.SmockinTestConfig;
import com.smockin.SmockinTestUtils;
import com.smockin.admin.dto.ApiImportConfigDTO;
import com.smockin.admin.dto.ApiImportDTO;
import com.smockin.admin.enums.ApiImportType;
import com.smockin.admin.exception.ApiImportException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.dao.RestfulMockDAO;
import com.smockin.admin.persistence.dao.SmockinUserDAO;
import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.persistence.enums.RecordStatusEnum;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import com.smockin.admin.persistence.enums.RestMockTypeEnum;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.hibernate.AssertionFailure;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.model.MultipleFailureException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;


/**
 * Created by mgallina.
 */
@RunWith(SpringRunner.class)
@DataJpaTest
@ContextConfiguration(classes = {SmockinTestConfig.class})
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class ApiImportRouterIntTest {

    @Rule
    public ExpectedException expected = ExpectedException.none();

    @Autowired
    private ApiImportRouter apiImportRouter;

    @Autowired
    private RestfulMockDAO restfulMockDAO;

    @Autowired
    private SmockinUserDAO smockinUserDAO;

    private ApiImportDTO dto;
    private SmockinUser user;

    @Before
    public void setUp() throws URISyntaxException, IOException {

        user = smockinUserDAO.saveAndFlush(SmockinTestUtils.buildSmockinUser());

        final URL url = this.getClass().getClassLoader().getResource("hello-api.raml");

        final String content = new String(Base64.getEncoder().encode(Files.readAllBytes(Paths.get(url.toURI()))));
        dto = new ApiImportDTO(ApiImportType.RAML, content, new ApiImportConfigDTO());
    }

    @After
    public void tearDown() {
        restfulMockDAO.deleteAll();
        restfulMockDAO.flush();

        smockinUserDAO.deleteAll();
        smockinUserDAO.flush();
    }

    @Test
    public void routePass() throws ApiImportException, ValidationException {

        Assert.assertTrue(restfulMockDAO.findAll().isEmpty());

        apiImportRouter.route(dto, user.getSessionToken());

        final List<RestfulMock> mocks = restfulMockDAO.findAll();

        Assert.assertEquals(3, mocks.size());

        mocks.stream().forEach(m -> {

            if ("/hello".equals(m.getPath())) {

                Assert.assertEquals(RestMethodEnum.GET, m.getMethod());

                Assert.assertEquals(1, m.getDefinitions().size());
                Assert.assertEquals(200, m.getDefinitions().get(0).getHttpStatusCode());
                Assert.assertEquals("application/json", m.getDefinitions().get(0).getResponseContentType());
                Assert.assertEquals("{ \"message\": \"helloworld\" }\n", m.getDefinitions().get(0).getResponseBody());

                Assert.assertEquals(1, m.getDefinitions().get(0).getResponseHeaders().size());
                Assert.assertNotNull(m.getDefinitions().get(0).getResponseHeaders().get("X-Powered-By"));
                Assert.assertEquals("FooBar", m.getDefinitions().get(0).getResponseHeaders().get("X-Powered-By"));

            } else if ("/hello/:name".equals(m.getPath())) {

                if (RestMethodEnum.GET.equals(m.getMethod())) {

                    Assert.assertEquals(3, m.getDefinitions().size());

                    m.getDefinitions().stream().forEach(d -> {

                        if (200 == d.getHttpStatusCode()) {

                            Assert.assertEquals("application/json", d.getResponseContentType());
                            Assert.assertEquals("{ \"message\": \"hello John!\" }\n", d.getResponseBody());
                            Assert.assertTrue(d.getResponseHeaders().isEmpty());

                        } else if (404 == d.getHttpStatusCode()) {

                            Assert.assertEquals("application/json", d.getResponseContentType());
                            Assert.assertNull(d.getResponseBody());
                            Assert.assertTrue(d.getResponseHeaders().isEmpty());

                        } else if (400 == d.getHttpStatusCode()) {

                            Assert.assertEquals("application/json", d.getResponseContentType());
                            Assert.assertEquals("{ \"message\": \"Missing name!\" }\n", d.getResponseBody());
                            Assert.assertTrue(d.getResponseHeaders().isEmpty());

                        } else {
                            Assert.fail();
                        }

                    });

                } else if (RestMethodEnum.POST.equals(m.getMethod())) {

                    Assert.assertEquals(1, m.getDefinitions().size());
                    Assert.assertEquals(201, m.getDefinitions().get(0).getHttpStatusCode());
                    Assert.assertEquals("application/json", m.getDefinitions().get(0).getResponseContentType());
                    Assert.assertEquals("{ \"id\" : 1 }\n", m.getDefinitions().get(0).getResponseBody());

                    Assert.assertEquals(1, m.getDefinitions().get(0).getResponseHeaders().size());
                    Assert.assertNotNull(m.getDefinitions().get(0).getResponseHeaders().get("X-Powered-By"));
                    Assert.assertEquals("FooBar", m.getDefinitions().get(0).getResponseHeaders().get("X-Powered-By"));

                } else {
                    Assert.fail();
                }

            } else {
                Assert.fail();
            }

        });

    }

    @Test
    public void route_EndpointAlreadyExistsConflict_Fail() throws ApiImportException, ValidationException {

        // Pre-test Assertions
        Assert.assertTrue(restfulMockDAO.findAll().isEmpty());

        // Setup
        restfulMockDAO.save(new RestfulMock("/hello/:name", RestMethodEnum.POST, RecordStatusEnum.ACTIVE, RestMockTypeEnum.PROXY_HTTP, 0, 0, 0, false, false, user));
        restfulMockDAO.flush();

        Assert.assertEquals(1, restfulMockDAO.findAll().size());

        // Assertions
        expected.expect(MultipleFailureException.class);
        expected.expect(new AssertInnerExceptions());

        // Test
        apiImportRouter.route(dto, user.getSessionToken());

    }

    class AssertInnerExceptions extends TypeSafeMatcher<Throwable> {

        @Override
        protected boolean matchesSafely(Throwable cause) {

            if (!(cause instanceof MultipleFailureException)) {
                return false;
            }

            return !((MultipleFailureException)cause).getFailures().stream().anyMatch(f ->
                    (!(f instanceof DataIntegrityViolationException)
                        && !(f instanceof AssertionFailure)
                    )
            );
        }

        @Override
        public void describeTo(Description description) {

        }
    }

}
