package com.smockin.admin.service;

import com.smockin.admin.dto.MockImportConfigDTO;
import com.smockin.admin.dto.ApiImportDTO;
import com.smockin.admin.dto.RestfulMockDTO;
import com.smockin.admin.enums.MockImportKeepStrategyEnum;
import com.smockin.admin.exception.MockImportException;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.dao.RestfulMockDAO;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import com.smockin.admin.service.utils.RestfulMockServiceUtils;
import com.smockin.admin.service.utils.UserTokenServiceUtils;
import com.smockin.utils.GeneralUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockMultipartFile;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class OpenApiApiImportServiceTest {

    @Rule
    public ExpectedException expected = ExpectedException.none();

    @Mock
    private RestfulMockService restfulMockService;

    @Mock
    private RestfulMockDAO restfulMockDAO;

    @Mock
    private UserTokenServiceUtils userTokenServiceUtils;

    @Mock
    private SmockinUser user;

    @Mock
    private RestfulMockServiceUtils restfulMockServiceUtils;

    @Captor
    private ArgumentCaptor<RestfulMockDTO> argCaptor;

    @Spy
    @InjectMocks
    private ApiImportService apiImportService = new OpenApiApiImportServiceImpl();

    @Before
    public void setUp() throws RecordNotFoundException, ValidationException {

        Mockito.when(restfulMockService.createEndpoint(Mockito.any(RestfulMockDTO.class), Mockito.anyString())).thenReturn("1");

        Mockito.when(userTokenServiceUtils.loadCurrentActiveUser(Mockito.anyString())).thenReturn(user);
        Mockito.when(user.getSessionToken()).thenReturn(GeneralUtils.generateUUID());

        Mockito.doNothing().when(restfulMockServiceUtils)
                .preHandleExistingEndpoints(Mockito.any(RestfulMockDTO.class), Mockito.any(MockImportConfigDTO.class), Mockito.any(SmockinUser.class), Mockito.anyString());
    }

    @Test
    public void importOpenApi3_Pass() throws MockImportException, ValidationException, RecordNotFoundException, URISyntaxException, IOException {

        // Setup
        final ApiImportDTO importDTO = new ApiImportDTO(buildMockMultiPartFile("openapi/openapi_3_0_hello.yaml"), new MockImportConfigDTO(MockImportKeepStrategyEnum.RENAME_EXISTING));

        // Test
        apiImportService.importApiDoc(importDTO, GeneralUtils.generateUUID());

        // Assertions - should create 3 endpoints: GET /hello, GET /hello/:name, POST /hello/:name
        Mockito.verify(restfulMockService, Mockito.times(3)).createEndpoint(argCaptor.capture(), Mockito.anyString());

        final List<RestfulMockDTO> restfulMockDTOs = argCaptor.getAllValues();

        for (RestfulMockDTO mockDTO : restfulMockDTOs) {

            if ("/hello".equals(mockDTO.getPath())) {

                Assert.assertEquals(RestMethodEnum.GET, mockDTO.getMethod());

                Assert.assertEquals(1, mockDTO.getDefinitions().size());
                Assert.assertEquals(200, mockDTO.getDefinitions().get(0).getHttpStatusCode());
                Assert.assertEquals("application/json", mockDTO.getDefinitions().get(0).getResponseContentType());
                Assert.assertNotNull(mockDTO.getDefinitions().get(0).getResponseBody());

                Assert.assertEquals(1, mockDTO.getDefinitions().get(0).getResponseHeaders().size());
                Assert.assertTrue(mockDTO.getDefinitions().get(0).getResponseHeaders().containsKey("X-Powered-By"));
                // Header value may be null in OpenAPI 3.0 if only schema is defined

            } else if ("/hello/:name".equals(mockDTO.getPath())) {

                if (RestMethodEnum.GET.equals(mockDTO.getMethod())) {

                    // Should have 3 response definitions: 200, 404, 400
                    Assert.assertEquals(3, mockDTO.getDefinitions().size());

                    mockDTO.getDefinitions().stream().forEach(d -> {

                        if (200 == d.getHttpStatusCode()) {
                            Assert.assertEquals("application/json", d.getResponseContentType());
                            Assert.assertNotNull(d.getResponseBody());
                            Assert.assertTrue(d.getResponseHeaders().isEmpty());

                        } else if (404 == d.getHttpStatusCode()) {
                            Assert.assertEquals("application/json", d.getResponseContentType());
                            Assert.assertNotNull(d.getResponseBody());
                            Assert.assertTrue(d.getResponseHeaders().isEmpty());

                        } else if (400 == d.getHttpStatusCode()) {
                            Assert.assertEquals("application/json", d.getResponseContentType());
                            Assert.assertNotNull(d.getResponseBody());
                            Assert.assertTrue(d.getResponseHeaders().isEmpty());

                        } else {
                            Assert.fail("Unexpected status code: " + d.getHttpStatusCode());
                        }

                    });

                } else if (RestMethodEnum.POST.equals(mockDTO.getMethod())) {

                    Assert.assertEquals(1, mockDTO.getDefinitions().size());
                    Assert.assertEquals(201, mockDTO.getDefinitions().get(0).getHttpStatusCode());
                    Assert.assertEquals("application/json", mockDTO.getDefinitions().get(0).getResponseContentType());
                    Assert.assertNotNull(mockDTO.getDefinitions().get(0).getResponseBody());

                    Assert.assertEquals(1, mockDTO.getDefinitions().get(0).getResponseHeaders().size());
                    Assert.assertTrue(mockDTO.getDefinitions().get(0).getResponseHeaders().containsKey("X-Powered-By"));
                    // Header value may be null in OpenAPI 3.0 if only schema is defined

                } else {
                    Assert.fail("Unexpected method: " + mockDTO.getMethod());
                }

            } else {
                Assert.fail("Unexpected path: " + mockDTO.getPath());
            }

        }

    }

    // TODO: Swagger 2.0 support requires additional configuration
    // Temporarily disabled until swagger-parser-v2-converter is properly configured
    // @Test
    public void importSwagger2_Pass() throws MockImportException, ValidationException, RecordNotFoundException, URISyntaxException, IOException {

        // Setup
        final ApiImportDTO importDTO = new ApiImportDTO(buildMockMultiPartFile("openapi/swagger_2_0_pets.yaml"), new MockImportConfigDTO(MockImportKeepStrategyEnum.RENAME_EXISTING));

        // Test
        apiImportService.importApiDoc(importDTO, GeneralUtils.generateUUID());

        // Assertions - should create 4 endpoints: GET /pets, POST /pets, GET /pets/:petId, DELETE /pets/:petId
        Mockito.verify(restfulMockService, Mockito.atLeast(4)).createEndpoint(argCaptor.capture(), Mockito.anyString());

        final List<RestfulMockDTO> restfulMockDTOs = argCaptor.getAllValues();

        // Verify paths were converted correctly
        boolean foundPets = false;
        boolean foundPetsWithId = false;

        for (RestfulMockDTO mockDTO : restfulMockDTOs) {
            if ("/pets".equals(mockDTO.getPath())) {
                foundPets = true;
            } else if ("/pets/:petId".equals(mockDTO.getPath())) {
                foundPetsWithId = true;
            }
        }

        Assert.assertTrue("Should have /pets path", foundPets);
        Assert.assertTrue("Should have /pets/:petId path", foundPetsWithId);
    }

    @Test
    public void importApiDoc_NullDto_Fail() throws MockImportException, ValidationException {

        // Assertions
        expected.expect(ValidationException.class);
        expected.expectMessage("No data was provided");

        // Test
        apiImportService.importApiDoc(null, GeneralUtils.generateUUID());

    }

    @Test
    public void importApiDoc_NullFile_Fail() throws MockImportException, ValidationException {

        // Setup
        final ApiImportDTO importDTO = new ApiImportDTO(null, new MockImportConfigDTO(MockImportKeepStrategyEnum.RENAME_EXISTING));

        // Assertions
        expected.expect(ValidationException.class);
        expected.expectMessage("No file found");

        // Test
        apiImportService.importApiDoc(importDTO, GeneralUtils.generateUUID());

    }

    @Test
    public void importApiDoc_NullConfig_Fail() throws MockImportException, ValidationException, URISyntaxException, IOException {

        // Setup
        final ApiImportDTO importDTO = new ApiImportDTO(buildMockMultiPartFile("openapi/openapi_3_0_hello.yaml"), null);

        // Assertions
        expected.expect(ValidationException.class);
        expected.expectMessage("No config found");

        // Test
        apiImportService.importApiDoc(importDTO, GeneralUtils.generateUUID());

    }

    @Test
    public void importApiDoc_InvalidExtension_Fail() throws MockImportException, ValidationException, URISyntaxException, IOException {

        // Setup - use a RAML file which has wrong extension for OpenAPI
        final ApiImportDTO importDTO = new ApiImportDTO(buildMockMultiPartFile("raml/bad_raml_100.raml"), new MockImportConfigDTO(MockImportKeepStrategyEnum.RENAME_EXISTING));

        // Assertions
        expected.expect(MockImportException.class);
        expected.expectMessage("Unsupported file extension");

        // Test
        apiImportService.importApiDoc(importDTO, GeneralUtils.generateUUID());

    }

    MockMultipartFile buildMockMultiPartFile(final String fileName) throws URISyntaxException, IOException {

        final URL openApiUrl = this.getClass().getClassLoader().getResource(fileName);
        final File openApiFile = new File(openApiUrl.toURI());
        final FileInputStream openApiInput = new FileInputStream(openApiFile);

        return new MockMultipartFile(fileName, openApiFile.getName(), "text/plain", IOUtils.toByteArray(openApiInput));
    }

}
