package com.smockin.admin.service;

import com.smockin.SmockinTestUtils;
import com.smockin.admin.dto.ApiImportDTO;
import com.smockin.admin.dto.MockImportConfigDTO;
import com.smockin.admin.dto.RestfulMockDTO;
import com.smockin.admin.enums.MockImportKeepStrategyEnum;
import com.smockin.admin.exception.MockImportException;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.dao.RestfulMockDAO;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.service.utils.RestfulMockServiceUtils;
import com.smockin.admin.service.utils.UserTokenServiceUtils;
import com.smockin.utils.GeneralUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import java.io.IOException;
import java.net.URISyntaxException;

@RunWith(MockitoJUnitRunner.class)
public class OpenApiImportServiceTest {

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
    private OpenApiImportServiceImpl apiImportService = new OpenApiImportServiceImpl();

    private SmockinTestUtils smockinTestUtils;

    @Before
    public void setUp() throws RecordNotFoundException, ValidationException {

        smockinTestUtils = new SmockinTestUtils();

    }

    @Test
    public void processFileImport_v2_Pass() throws MockImportException, ValidationException, RecordNotFoundException, URISyntaxException, IOException {

        // Setup
        final ApiImportDTO importDTO = new ApiImportDTO(smockinTestUtils.buildMockMultiPartFile("openapi/v2/petstore.json"), new MockImportConfigDTO(MockImportKeepStrategyEnum.RENAME_EXISTING));

        // Test
        apiImportService.processFileImport(importDTO, GeneralUtils.generateUUID());

    }

    @Test
    public void processFileImport_v3_Pass() throws MockImportException, ValidationException, RecordNotFoundException, URISyntaxException, IOException {

        // Setup
        final ApiImportDTO importDTO = new ApiImportDTO(smockinTestUtils.buildMockMultiPartFile("openapi/v3/petstore.yaml"), new MockImportConfigDTO(MockImportKeepStrategyEnum.RENAME_EXISTING));

        // Test
        apiImportService.processFileImport(importDTO, GeneralUtils.generateUUID());

    }

}
