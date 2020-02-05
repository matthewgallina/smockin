package com.smockin.admin.service;

import com.smockin.admin.dto.ApiImportDTO;
import com.smockin.admin.dto.RestfulMockDTO;
import com.smockin.admin.dto.RestfulMockDefinitionDTO;
import com.smockin.admin.exception.MockImportException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.dao.RestfulMockDAO;
import com.smockin.admin.persistence.enums.RecordStatusEnum;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import com.smockin.admin.persistence.enums.RestMockTypeEnum;
import com.smockin.admin.service.utils.RestfulMockServiceUtils;
import com.smockin.admin.service.utils.UserTokenServiceUtils;
import com.smockin.utils.GeneralUtils;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.yaml.snakeyaml.Yaml;
import javax.transaction.Transactional;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@Service("openApiImportService")
@Transactional
public class OpenApiImportServiceImpl implements ApiImportService {

    private final Logger logger = LoggerFactory.getLogger(OpenApiImportServiceImpl.class);

    @Autowired
    private RestfulMockService restfulMockService;

    @Autowired
    private RestfulMockDAO restfulMockDAO;

    @Autowired
    private UserTokenServiceUtils userTokenServiceUtils;

    @Autowired
    private RestfulMockServiceUtils restfulMockServiceUtils;

    @Override
    public void handleApiDocImport(final ApiImportDTO dto, final File tempDir, final String token)
            throws MockImportException, ValidationException {
        logger.debug("handleApiDocImport (OpenAPI) called");

        final File importFile = loadOpenApiSpecFileFromUpload(dto.getFile(), tempDir);

        try {

            final String content = FileUtils.readFileToString(importFile, Charset.defaultCharset());

//            System.out.println(content);

            if (importFile.getName().endsWith(".json")) {
                final Object obj = GeneralUtils.deserialiseJson(content);



            } else if (importFile.getName().endsWith(".yaml")) {
                final Object obj = new Yaml().load(content);



            }

            final ParseOptions parseOptions = new ParseOptions();
            parseOptions.setResolveFully(true);

            final SwaggerParseResult swaggerParseResult = new OpenAPIParser()
                    .readContents(content, null, parseOptions);

            if (swaggerParseResult == null) {
                throw new MockImportException("Error opening OpenAPI file");
            }

            final List<String> messages = swaggerParseResult.getMessages();

            if (!messages.isEmpty()) {
                if (logger.isDebugEnabled()) {
                    messages.stream()
                            .forEach(logger::debug);
                }
                throw new ValidationException("Error reading OpenAPI content");
            }

            final OpenAPI openAPI = swaggerParseResult.getOpenAPI();

System.out.println(openAPI.getComponents());

            if (logger.isDebugEnabled()) {
                logger.debug("OpenAPI Version: " + openAPI.getOpenapi());
                logger.debug("Title: " + openAPI.getInfo().getTitle());

                if (openAPI.getServers() != null) {
                    openAPI.getServers().stream().forEach(s -> {
                        logger.debug("Url: " + s.getUrl());
                        logger.debug("Description: " + s.getDescription());
                    });
                }
            }

            openAPI.getPaths()
                    .entrySet()
                    .stream()
                    .forEach(p -> saveMockPath(p.getKey(), p.getValue()));


        } catch (Exception ex) {
            throw new MockImportException("Error processing OpenAPI file ", ex);
        }

    }

    void saveMockPath(final String path,
                      final PathItem pathItem) {

        if (logger.isDebugEnabled()) {
            logger.debug("path: " + path);
        }

        pathItem.readOperationsMap()
                .entrySet()
                .stream()
                .forEach(o -> {

                    final String method = o.getKey().name();
                    final RestMethodEnum restMethod = RestMethodEnum.findByName(method);

                    if (restMethod != null) {

                        final RestfulMockDTO dto = new RestfulMockDTO(path, restMethod, RecordStatusEnum.ACTIVE, RestMockTypeEnum.SEQ, 0, 0, 0, false, false, false, false, 0, 0, null, null);

                        final Operation operation = o.getValue();

/*
if (operation.getParameters() != null) {
    operation.getParameters().stream().forEach(p -> {
        System.out.println(p.getName());
    });
}
*/

                        if (operation.getResponses() != null) {
                            operation.getResponses().entrySet().stream().forEach(r -> {

//System.out.println(e.getKey());
//System.out.println(e.getValue().getContent());
//System.out.println(e.getValue().getHeaders());

System.out.println(" ");
System.out.println(" ");

                                if ("default".equalsIgnoreCase(r.getKey())) {

System.out.println("DEFAULT: " + r.getValue());

                                } else {

                                    final RestfulMockDefinitionDTO definitionDTO = new RestfulMockDefinitionDTO();
                                    definitionDTO.setHttpStatusCode(Integer.valueOf(r.getKey()));
                                    definitionDTO.setOrderNo(dto.getDefinitions().size()+1);
//                                    definitionDTO.setResponseContentType(e.getValue().getContent());

                                    if (r.getValue().getDescription() != null && r.getValue().getContent() == null) {

                                        definitionDTO.setResponseBody(r.getValue().getDescription());
System.out.println(r.getValue().getDescription());

                                    } else if (r.getValue().getContent() != null) {

                                        r.getValue().getContent().entrySet().stream().forEach(x -> {
System.out.println(" ");
System.out.println(x.getKey());
System.out.println(" ");
System.out.println(x.getValue());

//                                    definitionDTO.setResponseBody(e.getValue().getDescription());

                                            if ("#/components/schemas/Pet".equalsIgnoreCase(x.getValue().getSchema().get$ref())) {
System.out.println("TODO load schema entity!!!");
                                            }

                                        });

                                    }

                                    dto.getDefinitions().add(definitionDTO);

                                }

                            });
                        }

                    } else if (logger.isDebugEnabled()) {
                        logger.debug("Ignoring unsupported endpoint method: " + method);
                    }

                });


        /*
        e.getValue().getExtensions().entrySet().stream().forEach(o -> {
            System.out.println(o);
        });

        e.getValue().readOperations().stream().forEach(o -> {
            System.out.println(o);
            System.out.println(o.getSummary());
            System.out.println(o.getDescription());
        });
        */


        /*
        try {
            restfulMockServiceUtils.preHandleExistingEndpoints(dto, apiImportConfig, user, conflictCtxPath);
            restfulMockService.createEndpoint(dto, user.getSessionToken());
        } catch (RecordNotFoundException e) {
            throw new MockImportException("Unauthorized user access");
        } catch (ValidationException e) {
            throw new MockImportException("A validation issue occurred", e);
        }
        */

    }

    File loadOpenApiSpecFileFromUpload(final MultipartFile file, final File tempDir) {

        final String fileName = file.getOriginalFilename();
        final String fileTypeExtension = GeneralUtils.getFileTypeExtension(fileName);

        try {

            final File uploadedFile = new File(tempDir + File.separator + file.getOriginalFilename());
            FileUtils.copyInputStreamToFile(file.getInputStream(), uploadedFile);

            if (".zip".equalsIgnoreCase(fileTypeExtension)) {

                final String parent = uploadedFile.getParent();

                GeneralUtils.unpackArchive(uploadedFile.getAbsolutePath(), parent);

                // Delete uploaded zip file
                uploadedFile.delete();

                return Files.find(Paths.get(parent), 5, (path, attr)
                        -> path.getFileName().toString().toLowerCase().indexOf(".json") > -1)
                        .findFirst()
                        .orElseThrow(() -> new MockImportException("Error locating json file within uploaded OpenAPI archive"))
                        .toFile();

            } else if (".json".equalsIgnoreCase(fileTypeExtension)) {
                return uploadedFile;
            } else if (".yaml".equalsIgnoreCase(fileTypeExtension)) {
                return uploadedFile;
            } else {
                throw new MockImportException("Unsupported file extension: " + fileName);
            }

        } catch (IOException e) {
            logger.error("Error reading uploaded OpenAPI file: " + fileName, e);
            throw new MockImportException("Error reading uploaded OpenAPI file: " + fileName);
        }

    }

}
