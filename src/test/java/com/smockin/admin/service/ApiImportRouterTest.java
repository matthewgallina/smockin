package com.smockin.admin.service;

import com.smockin.admin.dto.MockImportConfigDTO;
import com.smockin.admin.dto.ApiImportDTO;
import com.smockin.admin.enums.ApiImportTypeEnum;
import com.smockin.admin.exception.MockImportException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.utils.GeneralUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
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
    public void setUp() throws MockImportException, ValidationException {

        dto = new ApiImportDTO(multipartFile, new MockImportConfigDTO());

        Mockito.doNothing().when(ramlApiImportService).processFileImport(Mockito.any(ApiImportDTO.class), Mockito.anyString());

    }

    @Test
    public void routePass() throws MockImportException, ValidationException {

        apiImportRouter.route(ApiImportTypeEnum.RAML.name(), dto, GeneralUtils.generateUUID());

        Mockito.verify(ramlApiImportService, Mockito.times(1)).processFileImport(Mockito.any(ApiImportDTO.class), Mockito.anyString());

    }

    @Test
    public void route_nullDto_Fail() throws MockImportException, ValidationException {

        // Assertions
        expected.expect(ValidationException.class);
        expected.expectMessage("Inbound dto is undefined");

        // Test
        apiImportRouter.route(ApiImportTypeEnum.RAML.name(), null, GeneralUtils.generateUUID());

        Mockito.verify(ramlApiImportService, Mockito.never()).processFileImport(Mockito.any(ApiImportDTO.class), Mockito.anyString());
    }

    @Test
    public void route_nullFile_Fail() throws MockImportException, ValidationException {

        // Setup
        dto = new ApiImportDTO(null, new MockImportConfigDTO());

        // Assertions
        expected.expect(ValidationException.class);
        expected.expectMessage("Inbound file (in dto) is undefined");

        // Test
        apiImportRouter.route(ApiImportTypeEnum.RAML.name(), dto, GeneralUtils.generateUUID());

        Mockito.verify(ramlApiImportService, Mockito.never()).processFileImport(Mockito.any(ApiImportDTO.class), Mockito.anyString());
    }

    @Test
    public void route_nullImportType_Fail() throws MockImportException, ValidationException {

        // Assertions
        expected.expect(ValidationException.class);
        expected.expectMessage("Import Type is required");

        // Test
        apiImportRouter.route(null, dto, GeneralUtils.generateUUID());

        Mockito.verify(ramlApiImportService, Mockito.never()).processFileImport(Mockito.any(ApiImportDTO.class), Mockito.anyString());
    }

    @Test
    public void route_invalidImportType_Fail() throws MockImportException, ValidationException {

        // Assertions
        expected.expect(ValidationException.class);
        expected.expectMessage("Invalid Import Type: Foo");

        // Test
        apiImportRouter.route("Foo", dto, GeneralUtils.generateUUID());

        Mockito.verify(ramlApiImportService, Mockito.never()).processFileImport(Mockito.any(ApiImportDTO.class), Mockito.anyString());
    }

    @Test
    public void route_nullConfig_Fail() throws MockImportException, ValidationException {

        // Setup
        dto = new ApiImportDTO(multipartFile, null);

        // Assertions
        expected.expect(ValidationException.class);
        expected.expectMessage("Inbound config (in dto) is undefined");

        // Test
        apiImportRouter.route(ApiImportTypeEnum.RAML.name(), dto, GeneralUtils.generateUUID());

        Mockito.verify(ramlApiImportService, Mockito.never()).processFileImport(Mockito.any(ApiImportDTO.class), Mockito.anyString());
    }

}
