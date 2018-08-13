package com.smockin.admin.service;

import com.smockin.admin.dto.ApiImportConfigDTO;
import com.smockin.admin.dto.ApiImportDTO;
import com.smockin.admin.enums.ApiImportTypeEnum;
import com.smockin.admin.exception.ApiImportException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.utils.GeneralUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.mock.web.MockMultipartFile;

@RunWith(MockitoJUnitRunner.class)
public class ApiImportRouterTest {

    @Rule
    public ExpectedException expected = ExpectedException.none();

    @Mock
    private ApiImportService ramlApiImportService;

    @Mock
    private MockMultipartFile multipartFile;

    private ApiImportDTO dto;

    @Spy
    @InjectMocks
    private ApiImportRouter apiImportRouter = new ApiImportRouter();

    @Before
    public void setUp() throws ApiImportException, ValidationException {

        dto = new ApiImportDTO(multipartFile, new ApiImportConfigDTO());

        Mockito.doNothing().when(ramlApiImportService).importApiDoc(Matchers.any(ApiImportDTO.class), Matchers.anyString());

    }

    @Test
    public void routePass() throws ApiImportException, ValidationException {

        apiImportRouter.route(ApiImportTypeEnum.RAML.name(), dto, GeneralUtils.generateUUID());

        Mockito.verify(ramlApiImportService, Mockito.times(1)).importApiDoc(Matchers.any(ApiImportDTO.class), Matchers.anyString());

    }

    @Test
    public void route_nullDto_Fail() throws ApiImportException, ValidationException {

        // Assertions
        expected.expect(ValidationException.class);
        expected.expectMessage("Inbound dto is undefined");

        // Test
        apiImportRouter.route(ApiImportTypeEnum.RAML.name(), null, GeneralUtils.generateUUID());

        Mockito.verify(ramlApiImportService, Mockito.never()).importApiDoc(Matchers.any(ApiImportDTO.class), Matchers.anyString());
    }

    @Test
    public void route_nullFile_Fail() throws ApiImportException, ValidationException {

        // Setup
        dto = new ApiImportDTO(null, new ApiImportConfigDTO());

        // Assertions
        expected.expect(ValidationException.class);
        expected.expectMessage("Inbound file (in dto) is undefined");

        // Test
        apiImportRouter.route(ApiImportTypeEnum.RAML.name(), dto, GeneralUtils.generateUUID());

        Mockito.verify(ramlApiImportService, Mockito.never()).importApiDoc(Matchers.any(ApiImportDTO.class), Matchers.anyString());
    }

    @Test
    public void route_nullImportType_Fail() throws ApiImportException, ValidationException {

        // Assertions
        expected.expect(ValidationException.class);
        expected.expectMessage("Import Type is required");

        // Test
        apiImportRouter.route(null, dto, GeneralUtils.generateUUID());

        Mockito.verify(ramlApiImportService, Mockito.never()).importApiDoc(Matchers.any(ApiImportDTO.class), Matchers.anyString());
    }

    @Test
    public void route_invalidImportType_Fail() throws ApiImportException, ValidationException {

        // Assertions
        expected.expect(ValidationException.class);
        expected.expectMessage("Invalid Import Type: Foo");

        // Test
        apiImportRouter.route("Foo", dto, GeneralUtils.generateUUID());

        Mockito.verify(ramlApiImportService, Mockito.never()).importApiDoc(Matchers.any(ApiImportDTO.class), Matchers.anyString());
    }

    @Test
    public void route_nullToken_Fail() throws ApiImportException, ValidationException {

        // Assertions
        expected.expect(ValidationException.class);
        expected.expectMessage("Auth token is undefined");

        // Test
        apiImportRouter.route(ApiImportTypeEnum.RAML.name(), dto, null);

        Mockito.verify(ramlApiImportService, Mockito.never()).importApiDoc(Matchers.any(ApiImportDTO.class), Matchers.anyString());
    }

    @Test
    public void route_nullConfig_Fail() throws ApiImportException, ValidationException {

        // Setup
        dto = new ApiImportDTO(multipartFile, null);

        // Assertions
        expected.expect(ValidationException.class);
        expected.expectMessage("Inbound config (in dto) is undefined");

        // Test
        apiImportRouter.route(ApiImportTypeEnum.RAML.name(), dto, GeneralUtils.generateUUID());

        Mockito.verify(ramlApiImportService, Mockito.never()).importApiDoc(Matchers.any(ApiImportDTO.class), Matchers.anyString());
    }

}
