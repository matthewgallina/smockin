package com.smockin.admin.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.smockin.admin.dto.*;
import com.smockin.admin.dto.response.FtpMockResponseDTO;
import com.smockin.admin.dto.response.JmsMockResponseDTO;
import com.smockin.admin.dto.response.RestfulMockResponseDTO;
import com.smockin.admin.enums.DeploymentStatusEnum;
import com.smockin.admin.persistence.enums.*;
import com.smockin.utils.GeneralUtils;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.MediaType;

import java.io.File;
import java.io.IOException;
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
    private JmsMockService jmsMockService;

    @Mock
    private FtpMockService ftpMockService;

    @Spy
    @InjectMocks
    private MockDefinitionImportExportService mockDefinitionImportExportService = new MockDefinitionImportExportServiceImpl();

    private List<RestfulMockResponseDTO> allRestfulMocks;
    private List<JmsMockResponseDTO> allJmsMocks;
    private List<FtpMockResponseDTO> allFtpMocks;

    @Before
    public void setUp() {

        // HTTP Mocks
        allRestfulMocks = new ArrayList<>();

        // Seq based HTTP mock
        final RestfulMockResponseDTO seqBasedDTO = new RestfulMockResponseDTO(GeneralUtils.generateUUID(), "/hello", null, DeploymentStatusEnum.DEPLOYED, RestMethodEnum.GET, RecordStatusEnum.ACTIVE,
                RestMockTypeEnum.SEQ, GeneralUtils.getCurrentDate(), "bob", 0, 0,0,
                false, false, false, false);

        RestfulMockDefinitionDTO seqDTO = new RestfulMockDefinitionDTO(GeneralUtils.generateUUID(), 1, 400, MediaType.TEXT_PLAIN_VALUE, "Not good!", 0, false, 1, 0);
        seqDTO.getResponseHeaders().put("X-Smockin-ID", "67890");

        seqBasedDTO.getDefinitions().add(seqDTO);

        allRestfulMocks.add(seqBasedDTO);

        // Rule based HTTP mock
        final RestfulMockResponseDTO ruleBasedDTO = new RestfulMockResponseDTO(GeneralUtils.generateUUID(), "/hello", null, DeploymentStatusEnum.PENDING, RestMethodEnum.GET, RecordStatusEnum.ACTIVE,
                RestMockTypeEnum.RULE, GeneralUtils.getCurrentDate(), "bob", 0, 0,0,
                false, false, false, false);

        final RuleDTO rule = new RuleDTO(GeneralUtils.generateUUID(), 1, 200, MediaType.APPLICATION_JSON_VALUE, "{ \"msg\" : \"hello\" }", 0, false);
        rule.getResponseHeaders().put("X-Smockin-ID", "12345");

        final RuleGroupDTO ruleGroupDTO = new RuleGroupDTO(GeneralUtils.generateUUID(), 1);
        ruleGroupDTO.getConditions().add(new RuleConditionDTO(GeneralUtils.generateUUID(), "name", RuleDataTypeEnum.TEXT, RuleComparatorEnum.EQUALS, "foo", RuleMatchingTypeEnum.REQUEST_PARAM, false));

        rule.getGroups().add(ruleGroupDTO);

        ruleBasedDTO.getRules().add(rule);

        allRestfulMocks.add(ruleBasedDTO);

        // WS based HTTP mock
        final RestfulMockResponseDTO wsBasedDTO = new RestfulMockResponseDTO(GeneralUtils.generateUUID(), "/ws", "mike", DeploymentStatusEnum.OFFLINE, RestMethodEnum.GET, RecordStatusEnum.ACTIVE,
                RestMockTypeEnum.PROXY_WS, GeneralUtils.getCurrentDate(), "mike", 0, 50000,0,
                true, false, false, false);

        allRestfulMocks.add(wsBasedDTO);

        // SSE based HTTP mock
        final RestfulMockResponseDTO sseBasedDTO = new RestfulMockResponseDTO(GeneralUtils.generateUUID(), "/sse", "paul", DeploymentStatusEnum.DEPLOYED, RestMethodEnum.GET, RecordStatusEnum.ACTIVE,
                RestMockTypeEnum.PROXY_SSE, GeneralUtils.getCurrentDate(), "paul", 0, 0,40000,
                false, false, false, false);

        allRestfulMocks.add(sseBasedDTO);

        // Remote Feed based HTTP mock
        final RestfulMockResponseDTO remoteFeedBasedDTO = new RestfulMockResponseDTO(GeneralUtils.generateUUID(), "/remotefeed", null, DeploymentStatusEnum.DEPLOYED, RestMethodEnum.POST, RecordStatusEnum.ACTIVE,
                RestMockTypeEnum.PROXY_HTTP, GeneralUtils.getCurrentDate(), "howard", 60000, 0,0,
                false, false, false, true);

        allRestfulMocks.add(remoteFeedBasedDTO);

        Mockito.when(restfulMockService.loadAll(Matchers.anyString(), Matchers.anyString())).thenReturn(allRestfulMocks);

        // JMS Mocks
        allJmsMocks = new ArrayList<>();

        allJmsMocks.add(new JmsMockResponseDTO(GeneralUtils.generateUUID(), "mike", DeploymentStatusEnum.OFFLINE, "foo-queue", RecordStatusEnum.INACTIVE, JmsMockTypeEnum.QUEUE, GeneralUtils.getCurrentDate()));
        allJmsMocks.add(new JmsMockResponseDTO(GeneralUtils.generateUUID(), null, DeploymentStatusEnum.OFFLINE, "foo-topic", RecordStatusEnum.ACTIVE, JmsMockTypeEnum.TOPIC, GeneralUtils.getCurrentDate()));

        Mockito.when(jmsMockService.loadAll(Matchers.anyString(), Matchers.anyString())).thenReturn(allJmsMocks);

        // FTP Mocks
        allFtpMocks = new ArrayList<>();

        allFtpMocks.add(new FtpMockResponseDTO(GeneralUtils.generateUUID(), "pets", RecordStatusEnum.ACTIVE, DeploymentStatusEnum.DEPLOYED, GeneralUtils.getCurrentDate()));
        allFtpMocks.add(new FtpMockResponseDTO(GeneralUtils.generateUUID(), "homes", RecordStatusEnum.ACTIVE, DeploymentStatusEnum.DEPLOYED, GeneralUtils.getCurrentDate()));

        Mockito.when(ftpMockService.loadAll(Matchers.anyString(), Matchers.anyString())).thenReturn(allFtpMocks);

    }

    @Test
    public void mockDefinitionImportExportService_All_Pass() throws IOException {

        // Test
        final String base64EncodedZipFile = mockDefinitionImportExportService.export(Optional.empty(), "ABC");

        // Assertions
        Stream.of(unpackZipToTempArchive(base64EncodedZipFile).listFiles()).forEach(f -> {

            try {

                final String json = readFileToString(f);

                if (f.getName().indexOf(MockDefinitionImportExportService.restExportFileName) > -1) {
                    Assert.assertEquals(allRestfulMocks.size(), ((List)GeneralUtils.deserialiseJson(json)).size());
                } else if (f.getName().indexOf(MockDefinitionImportExportService.jmsExportFileName) > -1) {
                    Assert.assertEquals(allJmsMocks.size(), ((List)GeneralUtils.deserialiseJson(json)).size());
                } else if (f.getName().indexOf(MockDefinitionImportExportService.ftpExportFileName) > -1) {
                    Assert.assertEquals(allFtpMocks.size(), ((List)GeneralUtils.deserialiseJson(json)).size());
                } else {
                    Assert.fail();
                }

            } catch (IOException e) {
                Assert.fail();
            }

        });

    }

    @Test
    public void mockDefinitionImportExportService_Selected_Pass() throws IOException {

        // Setup
        final RestfulMockResponseDTO restfulDTO = allRestfulMocks.get(1);
        final JmsMockResponseDTO jmsDTO = allJmsMocks.get(0);
        final FtpMockResponseDTO ftpDTO = allFtpMocks.get(1);

        final Optional<List<ExportMockDTO>> selectedExports = Optional.of(Arrays.asList(
            new ExportMockDTO(restfulDTO.getExtId(), ServerTypeEnum.RESTFUL),
            new ExportMockDTO(jmsDTO.getExtId(), ServerTypeEnum.JMS),
            new ExportMockDTO(ftpDTO.getExtId(), ServerTypeEnum.FTP)
        ));

        // Test
        final String base64EncodedZipFile = mockDefinitionImportExportService.export(selectedExports, "ABC");

        // Assertions
        Stream.of(unpackZipToTempArchive(base64EncodedZipFile).listFiles()).forEach(f -> {

            try {
                final String json = readFileToString(f);

                if (f.getName().indexOf(MockDefinitionImportExportService.restExportFileName) > -1) {
                    final List<RestfulMockResponseDTO> restfulMocks = GeneralUtils.deserialiseJson(json, new TypeReference<List<RestfulMockResponseDTO>>() {});
                    Assert.assertEquals(1, restfulMocks.size());
                    Assert.assertEquals(restfulDTO.getExtId(), restfulMocks.get(0).getExtId());
                } else if (f.getName().indexOf(MockDefinitionImportExportService.jmsExportFileName) > -1) {
                    final List<JmsMockResponseDTO> jmsMocks = GeneralUtils.deserialiseJson(json, new TypeReference<List<JmsMockResponseDTO>>() {});
                    Assert.assertEquals(1, jmsMocks.size());
                    Assert.assertEquals(jmsDTO.getExtId(), jmsMocks.get(0).getExtId());
                } else if (f.getName().indexOf(MockDefinitionImportExportService.ftpExportFileName) > -1) {
                    final List<FtpMockResponseDTO> ftpMocks = GeneralUtils.deserialiseJson(json, new TypeReference<List<FtpMockResponseDTO>>() {});
                    Assert.assertEquals(1, ftpMocks.size());
                    Assert.assertEquals(ftpDTO.getExtId(), ftpMocks.get(0).getExtId());
                } else {
                    Assert.fail();
                }

            } catch (IOException e) {
                Assert.fail();
            }

        });

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

}
