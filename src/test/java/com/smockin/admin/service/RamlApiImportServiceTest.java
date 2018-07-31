package com.smockin.admin.service;

import com.smockin.admin.dto.ApiImportConfigDTO;
import com.smockin.admin.dto.ApiImportDTO;
import com.smockin.admin.dto.RestfulMockDTO;
import com.smockin.admin.enums.ApiImportType;
import com.smockin.admin.enums.ApiKeepStrategyEnum;
import com.smockin.admin.exception.ApiImportException;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import com.smockin.utils.GeneralUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class RamlApiImportServiceTest {

    @Rule
    public ExpectedException expected = ExpectedException.none();

    @Mock
    private RestfulMockService restfulMockService;

    @Captor
    private ArgumentCaptor<RestfulMockDTO> argCaptor;

    @Spy
    @InjectMocks
    private ApiImportService apiImportService = new RamlApiImportServiceImpl();;

    private ApiImportDTO importDTO;

    @Before
    public void setUp() throws URISyntaxException, IOException, RecordNotFoundException {

        final URL url = this.getClass().getClassLoader().getResource("hello-api.raml");
        final ApiImportConfigDTO configDto = new ApiImportConfigDTO(ApiKeepStrategyEnum.RENAME_EXISTING);

        importDTO = new ApiImportDTO(ApiImportType.RAML, new String(Files.readAllBytes(Paths.get(url.toURI()))), configDto);

        Mockito.when(restfulMockService.createEndpoint(Matchers.any(RestfulMockDTO.class), Matchers.anyString())).thenReturn("1");

    }

    @Test
    public void importApiDocPass() throws ApiImportException, ValidationException, RecordNotFoundException {

        // Test
        apiImportService.importApiDoc(importDTO, GeneralUtils.generateUUID());

        // Assertions
        Mockito.verify(restfulMockService, Mockito.times(3)).createEndpoint(argCaptor.capture(), Matchers.anyString());

        final List<RestfulMockDTO> restfulMockDTOs = argCaptor.getAllValues();

        for (RestfulMockDTO mockDTO : restfulMockDTOs) {

            if ("/hello".equals(mockDTO.getPath())) {

                Assert.assertEquals(RestMethodEnum.GET, mockDTO.getMethod());

                Assert.assertEquals(1, mockDTO.getDefinitions().size());
                Assert.assertEquals(200, mockDTO.getDefinitions().get(0).getHttpStatusCode());
                Assert.assertEquals("application/json", mockDTO.getDefinitions().get(0).getResponseContentType());
                Assert.assertEquals("{ \"message\": \"helloworld\" }\n", mockDTO.getDefinitions().get(0).getResponseBody());

                Assert.assertEquals(1, mockDTO.getDefinitions().get(0).getResponseHeaders().size());
                Assert.assertNotNull(mockDTO.getDefinitions().get(0).getResponseHeaders().get("X-Powered-By"));
                Assert.assertEquals("FooBar", mockDTO.getDefinitions().get(0).getResponseHeaders().get("X-Powered-By"));

            } else if ("/hello/:name".equals(mockDTO.getPath())) {

                if (RestMethodEnum.GET.equals(mockDTO.getMethod())) {

                    Assert.assertEquals(3, mockDTO.getDefinitions().size());

                    mockDTO.getDefinitions().stream().forEach(d -> {

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

                } else if (RestMethodEnum.POST.equals(mockDTO.getMethod())) {

                    Assert.assertEquals(1, mockDTO.getDefinitions().size());
                    Assert.assertEquals(201, mockDTO.getDefinitions().get(0).getHttpStatusCode());
                    Assert.assertEquals("application/json", mockDTO.getDefinitions().get(0).getResponseContentType());
                    Assert.assertEquals("{ \"id\" : 1 }\n", mockDTO.getDefinitions().get(0).getResponseBody());

                    Assert.assertEquals(1, mockDTO.getDefinitions().get(0).getResponseHeaders().size());
                    Assert.assertNotNull(mockDTO.getDefinitions().get(0).getResponseHeaders().get("X-Powered-By"));
                    Assert.assertEquals("FooBar", mockDTO.getDefinitions().get(0).getResponseHeaders().get("X-Powered-By"));

                } else {

                    Assert.fail();

                }

            } else {
                Assert.fail();
            }


        }

    }

    @Test
    public void importApiDoc_NullDto_Fail() throws ApiImportException, ValidationException {

        // Assertions
        expected.expect(ValidationException.class);
        expected.expectMessage("No data was provided");

        // Test
        apiImportService.importApiDoc(null, GeneralUtils.generateUUID());

    }

    @Test
    public void importApiDoc_NullContent_Fail() throws ApiImportException, ValidationException {

        // Setup
        importDTO.setContent(null);

        // Assertions
        expected.expect(ValidationException.class);
        expected.expectMessage("No API doc content found");

        // Test
        apiImportService.importApiDoc(importDTO, GeneralUtils.generateUUID());

    }

    @Test
    public void importApiDoc_InvalidContent_Fail() throws ApiImportException, ValidationException {

        // Setup
        importDTO.setContent("#%RAML 1.0\ntitle: helloworld API\nversion: v1\nbaseUri: http://localhost:8001/{version}\n/hello:\nget:"); // get is not indented correctly in YAML config

        // Assertions
        expected.expect(ApiImportException.class);
        expected.expectMessage("Unexpected key 'get'. Options are :");

        // Test
        apiImportService.importApiDoc(importDTO, GeneralUtils.generateUUID());

    }

}
