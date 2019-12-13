package com.smockin.admin.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.smockin.admin.dto.*;
import com.smockin.admin.dto.response.RestfulMockResponseDTO;
import com.smockin.admin.exception.MockImportException;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.persistence.enums.*;
import com.smockin.admin.service.utils.RestfulMockServiceUtils;
import com.smockin.admin.service.utils.UserTokenServiceUtils;
import com.smockin.utils.GeneralUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Stream;

@RunWith(MockitoJUnitRunner.class)
public class MockDefinitionImportExportServiceTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Mock
    private RestfulMockService restfulMockService;

    @Mock
    private UserTokenServiceUtils userTokenServiceUtils;

    @Mock
    private RestfulMockServiceUtils restfulMockServiceUtils;


    @Spy
    @InjectMocks
    private MockDefinitionImportExportService mockDefinitionImportExportService = new MockDefinitionImportExportServiceImpl();

    private List<RestfulMockResponseDTO> allRestfulMocks;

    @Before
    public void setUp() {

        // HTTP Mocks
        allRestfulMocks = new ArrayList<>();

        // Seq based HTTP mock
        final RestfulMockResponseDTO seqBasedDTO = new RestfulMockResponseDTO(GeneralUtils.generateUUID(), "/hello", null, RestMethodEnum.GET, RecordStatusEnum.ACTIVE,
                RestMockTypeEnum.SEQ, GeneralUtils.getCurrentDate(), "bob", 0, 0, 0,
                false, false, false, false, 0,0, null);

        RestfulMockDefinitionDTO seqDTO = new RestfulMockDefinitionDTO(GeneralUtils.generateUUID(), 1, 400, MediaType.TEXT_PLAIN_VALUE, "Not good!", 0, false, 1, 0);
        seqDTO.getResponseHeaders().put("X-Smockin-ID", "67890");

        seqBasedDTO.getDefinitions().add(seqDTO);

        allRestfulMocks.add(seqBasedDTO);

        // Rule based HTTP mock
        final RestfulMockResponseDTO ruleBasedDTO = new RestfulMockResponseDTO(GeneralUtils.generateUUID(), "/hello", null, RestMethodEnum.GET, RecordStatusEnum.ACTIVE,
                RestMockTypeEnum.RULE, GeneralUtils.getCurrentDate(), "bob", 0, 0, 0,
                false, false, false, false, 0,0, null);

        final RuleDTO rule = new RuleDTO(GeneralUtils.generateUUID(), 1, 200, MediaType.APPLICATION_JSON_VALUE, "{ \"msg\" : \"hello\" }", 0, false);
        rule.getResponseHeaders().put("X-Smockin-ID", "12345");

        final RuleGroupDTO ruleGroupDTO = new RuleGroupDTO(GeneralUtils.generateUUID(), 1);
        ruleGroupDTO.getConditions().add(new RuleConditionDTO(GeneralUtils.generateUUID(), "name", RuleDataTypeEnum.TEXT, RuleComparatorEnum.EQUALS, "foo", RuleMatchingTypeEnum.REQUEST_PARAM, false));

        rule.getGroups().add(ruleGroupDTO);

        ruleBasedDTO.getRules().add(rule);

        allRestfulMocks.add(ruleBasedDTO);

        // WS based HTTP mock
        final RestfulMockResponseDTO wsBasedDTO = new RestfulMockResponseDTO(GeneralUtils.generateUUID(), "/ws", "mike", RestMethodEnum.GET, RecordStatusEnum.ACTIVE,
                RestMockTypeEnum.PROXY_WS, GeneralUtils.getCurrentDate(), "mike", 0, 50000, 0,
                true, false, false, false, 0,0, null);

        allRestfulMocks.add(wsBasedDTO);

        // SSE based HTTP mock
        final RestfulMockResponseDTO sseBasedDTO = new RestfulMockResponseDTO(GeneralUtils.generateUUID(), "/sse", "paul", RestMethodEnum.GET, RecordStatusEnum.ACTIVE,
                RestMockTypeEnum.PROXY_SSE, GeneralUtils.getCurrentDate(), "paul", 0, 0, 40000,
                false, false, false, false, 0,0, null);

        allRestfulMocks.add(sseBasedDTO);

        // Remote Feed based HTTP mock
        final RestfulMockResponseDTO remoteFeedBasedDTO = new RestfulMockResponseDTO(GeneralUtils.generateUUID(), "/remotefeed", null, RestMethodEnum.POST, RecordStatusEnum.ACTIVE,
                RestMockTypeEnum.PROXY_HTTP, GeneralUtils.getCurrentDate(), "howard", 60000, 0, 0,
                false, false, false, false, 0, 0, null);

        allRestfulMocks.add(remoteFeedBasedDTO);

        Mockito.when(restfulMockService.loadAll(Mockito.anyString())).thenReturn(allRestfulMocks);

    }

    @Test
    public void export_allRestful_Pass() throws IOException {

        // Test
        final String base64Content = mockDefinitionImportExportService.export(ServerTypeEnum.RESTFUL, Arrays.asList(), "ABC");

        // Assertions
        Stream.of(unpackZipToTempArchive(base64Content).listFiles())
                .forEach(f -> {

            try {

                if (f.getName().indexOf(MockDefinitionImportExportService.restExportFileName) > -1) {
                    Assert.assertEquals(allRestfulMocks.size(), ((List)GeneralUtils.deserialiseJson(readFileToString(f))).size());
                } else {
                    Assert.fail();
                }

            } catch (IOException e) {
                Assert.fail();
            }

        });

    }

    @Test
    public void export_selectedRestful_Pass() throws IOException {

        // Setup
        final RestfulMockResponseDTO restfulDTO = allRestfulMocks.get(1);

        // Test
        final String base64Content = mockDefinitionImportExportService.export(ServerTypeEnum.RESTFUL, Arrays.asList(restfulDTO.getExtId()), "ABC");

        // Assertions
        Stream.of(unpackZipToTempArchive(base64Content).listFiles()).forEach(f -> {

            try {

                if (f.getName().indexOf(MockDefinitionImportExportService.restExportFileName) > -1) {
                    final List<RestfulMockResponseDTO> restfulMocks = GeneralUtils.deserialiseJson(readFileToString(f), new TypeReference<List<RestfulMockResponseDTO>>() {});
                    Assert.assertEquals(1, restfulMocks.size());
                    Assert.assertEquals(restfulDTO.getExtId(), restfulMocks.get(0).getExtId());
                } else {
                    Assert.fail();
                }

            } catch (IOException e) {
                Assert.fail();
            }

        });

    }

    @Test
    public void importFile_restful_Pass()
            throws MockImportException, ValidationException, RecordNotFoundException, IOException, URISyntaxException {

        // Setup
        final SmockinUser smockinUser = new SmockinUser();
        smockinUser.setSessionToken(GeneralUtils.generateUUID());
        Mockito.when(userTokenServiceUtils.loadCurrentUser(Mockito.anyString())).thenReturn(smockinUser);

        // Test
        final String result = mockDefinitionImportExportService.importFile(buildMockMultiPartFile("import-export/" + mockDefinitionImportExportService.exportZipFileNamePrefix + "rest" + mockDefinitionImportExportService.exportZipFileNameExt), new MockImportConfigDTO(), "ABC");

        // Assertions (NOTE: smockin_export_rest.zip file contains 5 records)
        Assert.assertNotNull(result);
        Assert.assertThat(result, CoreMatchers.is("RESTFUL mock: GET /hello successfully imported\n"
                + "RESTFUL mock: GET /hello successfully imported\n"
                + "RESTFUL mock: GET /ws successfully imported\n"
                + "RESTFUL mock: GET /sse successfully imported\n"
                + "RESTFUL mock: POST /remotefeed successfully imported\n"));
        Mockito.verify(restfulMockServiceUtils, Mockito.times(5))
                .preHandleExistingEndpoints(Mockito.any(RestfulMockDTO.class), Mockito.any(MockImportConfigDTO.class), Mockito.any(SmockinUser.class), Mockito.anyString());
        Mockito.verify(restfulMockService, Mockito.times(5)).createEndpoint(Mockito.any(RestfulMockResponseDTO.class), Mockito.anyString());
    }

    private File unpackZipToTempArchive(final String base64EncodedZipFile) throws IOException {

        Assert.assertNotNull(base64EncodedZipFile);

        final byte[] zipFileBytes = Base64.getDecoder().decode(base64EncodedZipFile);
        final File unpackedDir = tempFolder.newFolder();
        final File zipFile = tempFolder.newFile("test_export.zip");

        FileUtils.writeByteArrayToFile(zipFile, zipFileBytes);

        Assert.assertNotNull(zipFile);

        GeneralUtils.unpackArchive(zipFile.getAbsolutePath(), unpackedDir.getAbsolutePath());

        return unpackedDir;
    }

    private String readFileToString(final File f) throws IOException {

        final String json = FileUtils.readFileToString(f, Charset.defaultCharset());

        Assert.assertNotNull(json);
        Assert.assertTrue(json.length() > 0);

        return json;
    }

    private MockMultipartFile buildMockMultiPartFile(final String fileName) throws URISyntaxException, IOException {

        final URL importFileUrl = this.getClass().getClassLoader().getResource(fileName);
        final File archiveFile = new File(importFileUrl.toURI());
        final FileInputStream importFileStream = new FileInputStream(archiveFile);

        return new MockMultipartFile(fileName, archiveFile.getName(), "text/plain", IOUtils.toByteArray(importFileStream));
    }

}
