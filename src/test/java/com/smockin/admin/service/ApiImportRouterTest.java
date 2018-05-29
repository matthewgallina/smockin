package com.smockin.admin.service;

import com.smockin.admin.dto.ApiImportDTO;
import com.smockin.admin.enums.ApiImportType;
import com.smockin.admin.exception.ApiImportException;
import com.smockin.admin.exception.ValidationException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ApiImportRouterTest {

    @Rule
    public ExpectedException expected = ExpectedException.none();

    @Mock
    private ApiImportService ramlApiImportService;

    @Spy
    @InjectMocks
    private ApiImportRouter apiImportRouter = new ApiImportRouter();

    private ApiImportDTO dto;

    @Before
    public void setUp() throws ApiImportException, ValidationException {

        dto = new ApiImportDTO();
        dto.setType(ApiImportType.RAML);

        Mockito.doNothing().when(ramlApiImportService).importApiDoc(Matchers.any(ApiImportDTO.class));

    }

    @Test
    public void routePass() throws ApiImportException, ValidationException {

        apiImportRouter.route(dto);

        Mockito.verify(ramlApiImportService, Mockito.times(1)).importApiDoc(Matchers.any(ApiImportDTO.class));

    }

    @Test
    public void route_nullDTO_Fail() throws ApiImportException, ValidationException {

        // Assertions
        expected.expect(ValidationException.class);
        expected.expectMessage("No data found");

        // Test
        apiImportRouter.route(null);

        Mockito.verify(ramlApiImportService, Mockito.never()).importApiDoc(Matchers.any(ApiImportDTO.class));
    }

    @Test
    public void route_nullType_Fail() throws ApiImportException, ValidationException {

        // Setup
        dto.setType(null);

        // Assertions
        expected.expect(ValidationException.class);
        expected.expectMessage("Import Type is required");

        // Test
        apiImportRouter.route(dto);

        Mockito.verify(ramlApiImportService, Mockito.never()).importApiDoc(Matchers.any(ApiImportDTO.class));
    }

}
