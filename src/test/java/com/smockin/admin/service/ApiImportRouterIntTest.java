package com.smockin.admin.service;

import com.smockin.SmockinTestConfig;
import com.smockin.SmockinTestUtils;
import com.smockin.admin.dto.ApiImportConfigDTO;
import com.smockin.admin.dto.ApiImportDTO;
import com.smockin.admin.enums.ApiImportTypeEnum;
import com.smockin.admin.enums.ApiKeepStrategyEnum;
import com.smockin.admin.exception.ApiImportException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.dao.RestfulMockDAO;
import com.smockin.admin.persistence.dao.SmockinUserDAO;
import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.persistence.enums.RecordStatusEnum;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import com.smockin.admin.persistence.enums.RestMockTypeEnum;
import org.apache.commons.io.IOUtils;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Optional;

/**
 * Created by mgallina.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SmockinTestConfig.class)
@DataJpaTest
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

    private SmockinUser user;

    @Before
    public void setUp() throws URISyntaxException, IOException {

        user = smockinUserDAO.saveAndFlush(SmockinTestUtils.buildSmockinUser());

    }

    @After
    public void tearDown() {
        restfulMockDAO.deleteAll();
        restfulMockDAO.flush();

        smockinUserDAO.deleteAll();
        smockinUserDAO.flush();
    }

    @Test
    public void route_Raml100_Pass() throws ApiImportException, ValidationException, URISyntaxException, IOException {

        // Setup
        final ApiImportDTO dto = new ApiImportDTO(buildMockMultipartFile("raml_100.raml"),
                new ApiImportConfigDTO(ApiKeepStrategyEnum.RENAME_NEW));

        Assert.assertTrue(restfulMockDAO.findAll().isEmpty());

        // Test
        apiImportRouter.route(ApiImportTypeEnum.RAML.name(), dto, user.getSessionToken());

        // Assertions
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
    public void route_Raml200_Pass() throws ApiImportException, ValidationException, URISyntaxException, IOException {

        // Setup
        final ApiImportDTO dto = new ApiImportDTO(buildMockMultipartFile("raml_200.zip"),
                new ApiImportConfigDTO(ApiKeepStrategyEnum.RENAME_NEW));

        Assert.assertTrue(restfulMockDAO.findAll().isEmpty());

        // Test
        apiImportRouter.route(ApiImportTypeEnum.RAML.name(), dto, user.getSessionToken());

        // Assertions
        final List<RestfulMock> mocks = restfulMockDAO.findAll();

        Assert.assertEquals(12, mocks.size());

        mocks.stream().forEach(m -> {

            if ("/bars".equals(m.getPath())) {

                if (RestMethodEnum.GET.equals(m.getMethod())) {

                    Assert.assertEquals(1, m.getDefinitions().size());

                    Assert.assertEquals(200, m.getDefinitions().get(0).getHttpStatusCode());
                    Assert.assertEquals("application/json", m.getDefinitions().get(0).getResponseContentType());
                    Assert.assertEquals("[ { \"id\" : 1, \"name\" : \"First Bar\", \"city\" : \"Austin\", \"fooId\" : 2 }, { \"id\" : 2, \"name\" : \"Second Bar\", \"city\" : \"Dallas\", \"fooId\" : 1 }, { \"id\" : 3, \"name\" : \"Third Bar\", \"fooId\" : 2 } ]", flattenJson(m.getDefinitions().get(0).getResponseBody()));

                } else if (RestMethodEnum.POST.equals(m.getMethod())) {

                    Assert.assertEquals(1, m.getDefinitions().size());

                    Assert.assertEquals(201, m.getDefinitions().get(0).getHttpStatusCode());
                    Assert.assertEquals("application/json", m.getDefinitions().get(0).getResponseContentType());
                    Assert.assertEquals("{ \"id\" : 1, \"name\" : \"First Bar\", \"city\" : \"Austin\", \"fooId\" : 2 }", flattenJson(m.getDefinitions().get(0).getResponseBody()));

                } else {
                    Assert.fail();
                }

/*
                Assert.assertEquals(1, m.getDefinitions().get(0).getResponseHeaders().size());
                Assert.assertNotNull(m.getDefinitions().get(0).getResponseHeaders().get("X-Powered-By"));
                Assert.assertEquals("FooBar", m.getDefinitions().get(0).getResponseHeaders().get("X-Powered-By"));
*/
            } else if ("/bars/:barId".equals(m.getPath())) {

                if (RestMethodEnum.GET.equals(m.getMethod())) {

                    Assert.assertEquals(2, m.getDefinitions().size());

                    Assert.assertEquals(200, m.getDefinitions().get(0).getHttpStatusCode());
                    Assert.assertEquals("application/json", m.getDefinitions().get(0).getResponseContentType());
                    Assert.assertEquals("{ \"id\" : 1, \"name\" : \"First Bar\", \"city\" : \"Austin\", \"fooId\" : 2 }", flattenJson(m.getDefinitions().get(0).getResponseBody()));

                    Assert.assertEquals(404, m.getDefinitions().get(1).getHttpStatusCode());
                    Assert.assertEquals("application/json", m.getDefinitions().get(1).getResponseContentType());
                    Assert.assertEquals("{ \"message\" : \"Not found\", \"code\" : 1001 }", flattenJson(m.getDefinitions().get(1).getResponseBody()));

                } else if (RestMethodEnum.PUT.equals(m.getMethod())) {

                    Assert.assertEquals(2, m.getDefinitions().size());

                    Assert.assertEquals(200, m.getDefinitions().get(0).getHttpStatusCode());
                    Assert.assertEquals("application/json", m.getDefinitions().get(0).getResponseContentType());
                    Assert.assertEquals("{ \"id\" : 1, \"name\" : \"First Bar\", \"city\" : \"Austin\", \"fooId\" : 2 }", flattenJson(m.getDefinitions().get(0).getResponseBody()));

                    Assert.assertEquals(404, m.getDefinitions().get(1).getHttpStatusCode());
                    Assert.assertEquals("application/json", m.getDefinitions().get(1).getResponseContentType());
                    Assert.assertEquals("{ \"message\" : \"Not found\", \"code\" : 1001 }", flattenJson(m.getDefinitions().get(1).getResponseBody()));

                } else if (RestMethodEnum.DELETE.equals(m.getMethod())) {

                    Assert.assertEquals(2, m.getDefinitions().size());

                    Assert.assertEquals(204, m.getDefinitions().get(0).getHttpStatusCode());
                    Assert.assertEquals("application/json", m.getDefinitions().get(0).getResponseContentType());
                    Assert.assertNull(m.getDefinitions().get(0).getResponseBody());

                    Assert.assertEquals(404, m.getDefinitions().get(1).getHttpStatusCode());
                    Assert.assertEquals("application/json", m.getDefinitions().get(1).getResponseContentType());
                    Assert.assertEquals("{ \"message\" : \"Not found\", \"code\" : 1001 }", flattenJson(m.getDefinitions().get(1).getResponseBody()));

                } else {
                    Assert.fail();
                }

            } else if ("/bars/fooId/:fooId".equals(m.getPath())) {

                Assert.assertEquals(RestMethodEnum.GET, m.getMethod());
                Assert.assertEquals(1, m.getDefinitions().size());

                Assert.assertEquals(200, m.getDefinitions().get(0).getHttpStatusCode());
                Assert.assertEquals("application/json", m.getDefinitions().get(0).getResponseContentType());
                Assert.assertEquals("[ { \"id\" : 1, \"name\" : \"First Bar\", \"city\" : \"Austin\", \"fooId\" : 2 }, { \"id\" : 2, \"name\" : \"Second Bar\", \"city\" : \"Dallas\", \"fooId\" : 1 }, { \"id\" : 3, \"name\" : \"Third Bar\", \"fooId\" : 2 } ]", flattenJson(m.getDefinitions().get(0).getResponseBody()));

            } else if ("/foos".equals(m.getPath())) {

                if (RestMethodEnum.GET.equals(m.getMethod())) {

                    Assert.assertEquals(1, m.getDefinitions().size());

                    Assert.assertEquals(200, m.getDefinitions().get(0).getHttpStatusCode());
                    Assert.assertEquals("application/json", m.getDefinitions().get(0).getResponseContentType());
                    Assert.assertEquals("[ { \"id\" : 1, \"name\" : \"First Foo\", \"ownerName\" : \"Jack Robinson\" }, { \"id\" : 2, \"name\" : \"Second Foo\" }, { \"id\" : 3, \"name\" : \"Third Foo\", \"ownerName\" : \"Chuck Norris\" } ]",
                            flattenJson(m.getDefinitions().get(0).getResponseBody()));

                } else if (RestMethodEnum.POST.equals(m.getMethod())) {

                    Assert.assertEquals(1, m.getDefinitions().size());

                    Assert.assertEquals(201, m.getDefinitions().get(0).getHttpStatusCode());
                    Assert.assertEquals("application/json", m.getDefinitions().get(0).getResponseContentType());
                    Assert.assertEquals("{ \"id\" : 1, \"name\" : \"First Foo\" }", flattenJson(m.getDefinitions().get(0).getResponseBody()));

                } else {
                    Assert.fail();
                }

            } else if ("/foos/:fooId".equals(m.getPath())) {

                if (RestMethodEnum.GET.equals(m.getMethod())) {

                    Assert.assertEquals(2, m.getDefinitions().size());

                    Assert.assertEquals(200, m.getDefinitions().get(0).getHttpStatusCode());
                    Assert.assertEquals("application/json", m.getDefinitions().get(0).getResponseContentType());
                    Assert.assertEquals("{ \"id\" : 1, \"name\" : \"First Foo\" }",
                            flattenJson(m.getDefinitions().get(0).getResponseBody()));

                    Assert.assertEquals(404, m.getDefinitions().get(1).getHttpStatusCode());
                    Assert.assertEquals("application/json", m.getDefinitions().get(1).getResponseContentType());
                    Assert.assertEquals("{ \"message\" : \"Not found\", \"code\" : 1001 }", flattenJson(m.getDefinitions().get(1).getResponseBody()));

                } else if (RestMethodEnum.PUT.equals(m.getMethod())) {

                    Assert.assertEquals(2, m.getDefinitions().size());

                    Assert.assertEquals(200, m.getDefinitions().get(0).getHttpStatusCode());
                    Assert.assertEquals("application/json", m.getDefinitions().get(0).getResponseContentType());
                    Assert.assertEquals("{ \"id\" : 1, \"name\" : \"First Foo\" }", flattenJson(m.getDefinitions().get(0).getResponseBody()));

                    Assert.assertEquals(404, m.getDefinitions().get(1).getHttpStatusCode());
                    Assert.assertEquals("application/json", m.getDefinitions().get(1).getResponseContentType());
                    Assert.assertEquals("{ \"message\" : \"Not found\", \"code\" : 1001 }", flattenJson(m.getDefinitions().get(1).getResponseBody()));

                } else if (RestMethodEnum.DELETE.equals(m.getMethod())) {

                    Assert.assertEquals(2, m.getDefinitions().size());

                    Assert.assertEquals(204, m.getDefinitions().get(0).getHttpStatusCode());
                    Assert.assertEquals("application/json", m.getDefinitions().get(0).getResponseContentType());
                    Assert.assertNull(m.getDefinitions().get(0).getResponseBody());

                    Assert.assertEquals(404, m.getDefinitions().get(1).getHttpStatusCode());
                    Assert.assertEquals("application/json", m.getDefinitions().get(1).getResponseContentType());
                    Assert.assertEquals("{ \"message\" : \"Not found\", \"code\" : 1001 }", flattenJson(m.getDefinitions().get(1).getResponseBody()));

                } else {
                    Assert.fail();
                }

            } else if ("/foos/name/:name".equals(m.getPath())) {

                Assert.assertEquals(RestMethodEnum.GET, m.getMethod());

                Assert.assertEquals(1, m.getDefinitions().size());

                Assert.assertEquals(200, m.getDefinitions().get(0).getHttpStatusCode());
                Assert.assertEquals("application/json", m.getDefinitions().get(0).getResponseContentType());
                Assert.assertEquals("[ { \"id\" : 1, \"name\" : \"First Foo\", \"ownerName\" : \"Jack Robinson\" }, { \"id\" : 2, \"name\" : \"Second Foo\" }, { \"id\" : 3, \"name\" : \"Third Foo\", \"ownerName\" : \"Chuck Norris\" } ]",
                        flattenJson(m.getDefinitions().get(0).getResponseBody()));

            }

        });

    }

    @Test
    public void route_RemoveExisting_Pass() throws ApiImportException, ValidationException, URISyntaxException, IOException {

        // Setup
        final ApiImportDTO dto = new ApiImportDTO(buildMockMultipartFile("raml_100.raml"), new ApiImportConfigDTO());

        // Pre-test Assertions
        Assert.assertTrue(restfulMockDAO.findAll().isEmpty());

        final long originalMockId = restfulMockDAO.save(new RestfulMock("/hello/:name", RestMethodEnum.POST, RecordStatusEnum.ACTIVE, RestMockTypeEnum.PROXY_HTTP, 0, 0, 0, false, false, false, user)).getId();
        Assert.assertEquals(1, restfulMockDAO.findAll().size());
        restfulMockDAO.flush();

        // Test
        apiImportRouter.route(ApiImportTypeEnum.RAML.name(), dto, user.getSessionToken());

        // Assertions
        final List<RestfulMock> mocks = restfulMockDAO.findAll();

        Assert.assertEquals(3, restfulMockDAO.findAll().size());

        final Optional<RestfulMock> mock = mocks.stream()
                .filter(m -> "/hello/:name".equals(m.getPath()))
                .findFirst();

        Assert.assertTrue(mock.isPresent());
        Assert.assertNotEquals(originalMockId, mock.get().getId());

    }

    private MockMultipartFile buildMockMultipartFile(final String fileName) throws URISyntaxException, IOException {

        final URL ramlUrl = this.getClass().getClassLoader().getResource(fileName);
        final File ramlFile = new File(ramlUrl.toURI());
        final FileInputStream ramlInput = new FileInputStream(ramlFile);

        return new MockMultipartFile(fileName, ramlFile.getName(), "text/plain", IOUtils.toByteArray(ramlInput));
    }

    private String flattenJson(final String str) {
        return str.replaceAll("\\s+", " ");
    }

}

