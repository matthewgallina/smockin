package com.smockin.admin.service;

import com.smockin.admin.dto.ApiImportDTO;
import com.smockin.admin.dto.MockImportConfigDTO;
import com.smockin.admin.dto.RestfulMockDTO;
import com.smockin.admin.dto.RestfulMockDefinitionDTO;
import com.smockin.admin.exception.MockImportException;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.dao.RestfulMockDAO;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.persistence.enums.RecordStatusEnum;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import com.smockin.admin.persistence.enums.RestMockTypeEnum;
import com.smockin.admin.service.utils.RestfulMockServiceUtils;
import com.smockin.admin.service.utils.UserTokenServiceUtils;
import com.smockin.utils.GeneralUtils;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.transaction.Transactional;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

@Service("openApiApiImportService")
@Transactional
public class OpenApiApiImportServiceImpl implements ApiImportService {

    private final Logger logger = LoggerFactory.getLogger(OpenApiApiImportServiceImpl.class);

    @Autowired
    private RestfulMockService restfulMockService;

    @Autowired
    private RestfulMockDAO restfulMockDAO;

    @Autowired
    private UserTokenServiceUtils userTokenServiceUtils;

    @Autowired
    private RestfulMockServiceUtils restfulMockServiceUtils;

    @Override
    public void importApiDoc(final ApiImportDTO dto, final String token) throws MockImportException, ValidationException {
        logger.debug("importApiDoc (OpenAPI) called");

        validate(dto);

        File tempDir = null;

        try {
            tempDir = Files.createTempDirectory(Long.toString(System.nanoTime())).toFile();
            final OpenAPI openAPI = readContent(loadOpenApiFileFromUpload(dto.getFile(), tempDir));
            final MockImportConfigDTO apiImportConfig = dto.getConfig();
            final String conflictCtxPath = "openapi_" + GeneralUtils.createFileNameUniqueTimeStamp();

            debug("Keep existing mocks: " + apiImportConfig.isKeepExisting());
            debug("Keep strategy: " + apiImportConfig.getKeepStrategy());

            // Extract API info
            if (openAPI.getInfo() != null) {
                debug("API Title: " + openAPI.getInfo().getTitle());
                debug("API Version: " + openAPI.getInfo().getVersion());
            }

            // Determine default mime type from servers or first content type
            final String defaultMimeType = getDefaultMimeType(openAPI);

            final SmockinUser user = userTokenServiceUtils.loadCurrentActiveUser(token);
            loadPaths(openAPI, apiImportConfig, user, conflictCtxPath, defaultMimeType);

        } catch (RecordNotFoundException ex) {
            throw new MockImportException("Unauthorized user access");
        } catch (MockImportException ex) {
            throw ex;
        } catch (Throwable ex) {
            logger.error("Unexpected error whilst importing OpenAPI spec", ex);
            throw new MockImportException("Unexpected error whilst importing OpenAPI spec: " + ex.getMessage());
        } finally {
            if (tempDir != null && !FileUtils.deleteQuietly(tempDir)) {
                logger.error("Error deleting temp dir");
            }
        }
    }

    void validate(final ApiImportDTO dto) throws ValidationException {
        if (dto == null) {
            throw new ValidationException("No data was provided");
        }
        if (dto.getFile() == null) {
            throw new ValidationException("No file found");
        }
        if (dto.getConfig() == null) {
            throw new ValidationException("No config found");
        }
    }

    OpenAPI readContent(final File openApiFile) throws MockImportException {
        final ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolve(true);
        parseOptions.setFlatten(true);

        final SwaggerParseResult result = new OpenAPIV3Parser().readLocation(openApiFile.getAbsolutePath(), null, parseOptions);

        if (result.getMessages() != null && !result.getMessages().isEmpty()) {
            final String allErrors = String.join(GeneralUtils.CARRIAGE, result.getMessages());
            logger.warn("OpenAPI parsing warnings: {}", allErrors);
            // Don't throw exception for warnings, only for null result
        }

        if (result.getOpenAPI() == null) {
            throw new MockImportException("Failed to parse OpenAPI specification. Check the file format.");
        }

        return result.getOpenAPI();
    }

    String getDefaultMimeType(final OpenAPI openAPI) {
        // Try to get from first path's response content type
        if (openAPI.getPaths() != null) {
            for (Map.Entry<String, PathItem> entry : openAPI.getPaths().entrySet()) {
                PathItem pathItem = entry.getValue();
                for (Operation operation : pathItem.readOperations()) {
                    if (operation != null && operation.getResponses() != null) {
                        for (ApiResponse response : operation.getResponses().values()) {
                            if (response.getContent() != null && !response.getContent().isEmpty()) {
                                return response.getContent().keySet().iterator().next();
                            }
                        }
                    }
                }
            }
        }
        return "application/json";
    }

    void loadPaths(final OpenAPI openAPI, final MockImportConfigDTO apiImportConfig, 
                   final SmockinUser user, final String conflictCtxPath, final String defaultMimeType) throws MockImportException {
        if (openAPI.getPaths() == null) {
            throw new MockImportException("No paths found in OpenAPI specification");
        }

        for (Map.Entry<String, PathItem> entry : openAPI.getPaths().entrySet()) {
            final String path = formatPath(entry.getKey());
            final PathItem pathItem = entry.getValue();

            debug("Importing Endpoint: " + path);

            parsePathItem(path, pathItem, apiImportConfig, user, conflictCtxPath, defaultMimeType);
        }
    }

    void parsePathItem(final String path, final PathItem pathItem, final MockImportConfigDTO apiImportConfig,
                       final SmockinUser user, final String conflictCtxPath, final String defaultMimeType) throws MockImportException {

        // Process each HTTP method
        if (pathItem.getGet() != null) {
            parseOperation(path, RestMethodEnum.GET, pathItem.getGet(), apiImportConfig, user, conflictCtxPath, defaultMimeType);
        }
        if (pathItem.getPost() != null) {
            parseOperation(path, RestMethodEnum.POST, pathItem.getPost(), apiImportConfig, user, conflictCtxPath, defaultMimeType);
        }
        if (pathItem.getPut() != null) {
            parseOperation(path, RestMethodEnum.PUT, pathItem.getPut(), apiImportConfig, user, conflictCtxPath, defaultMimeType);
        }
        if (pathItem.getDelete() != null) {
            parseOperation(path, RestMethodEnum.DELETE, pathItem.getDelete(), apiImportConfig, user, conflictCtxPath, defaultMimeType);
        }
        if (pathItem.getPatch() != null) {
            parseOperation(path, RestMethodEnum.PATCH, pathItem.getPatch(), apiImportConfig, user, conflictCtxPath, defaultMimeType);
        }
        if (pathItem.getHead() != null) {
            parseOperation(path, RestMethodEnum.HEAD, pathItem.getHead(), apiImportConfig, user, conflictCtxPath, defaultMimeType);
        }
    }

    void parseOperation(final String path, final RestMethodEnum method, final Operation operation,
                        final MockImportConfigDTO apiImportConfig, final SmockinUser user, 
                        final String conflictCtxPath, final String defaultMimeType) throws MockImportException {

        debug("Method: " + method);

        // Log request parameters
        if (operation.getParameters() != null) {
            debug("Request Parameters:");
            for (Parameter param : operation.getParameters()) {
                debug("  Param name: " + param.getName());
                debug("  Param in: " + param.getIn());
                debug("  Param required: " + param.getRequired());
                // TODO we could create a rule here...
            }
        }

        // Log request body
        if (operation.getRequestBody() != null && operation.getRequestBody().getContent() != null) {
            debug("Request Body:");
            for (Map.Entry<String, MediaType> entry : operation.getRequestBody().getContent().entrySet()) {
                debug("  Content Type: " + entry.getKey());
                if (entry.getValue().getExample() != null) {
                    debug("  Body example: " + entry.getValue().getExample());
                }
            }
        }

        // Create mock DTO
        final RestfulMockDTO dto = new RestfulMockDTO(path, method, RecordStatusEnum.ACTIVE, RestMockTypeEnum.SEQ,
                false, 0, 0, 0, false,
                false, false, false, 0,
                0, null, null, null, null, null);

        // Process responses
        debug("Responses:");
        if (operation.getResponses() != null) {
            for (Map.Entry<String, ApiResponse> entry : operation.getResponses().entrySet()) {
                final String statusCodeStr = entry.getKey();
                final ApiResponse apiResponse = entry.getValue();

                int statusCode;
                try {
                    statusCode = Integer.parseInt(statusCodeStr);
                } catch (NumberFormatException e) {
                    // Handle "default" response
                    statusCode = 200;
                }

                debug("HTTP Response Status Code: " + statusCode);

                // Handle response with no body (e.g., 204)
                if (apiResponse.getContent() == null || apiResponse.getContent().isEmpty()) {
                    final RestfulMockDefinitionDTO restfulMockDefinitionDTO = new RestfulMockDefinitionDTO(1, statusCode, defaultMimeType, null, 1);
                    dto.getDefinitions().add(restfulMockDefinitionDTO);
                } else {
                    // Response Body
                    for (Map.Entry<String, MediaType> contentEntry : apiResponse.getContent().entrySet()) {
                        final String contentType = contentEntry.getKey();
                        debug("Content Type: " + contentType);

                        String responseBody = null;
                        if (contentEntry.getValue().getExample() != null) {
                            responseBody = contentEntry.getValue().getExample().toString();
                        } else if (contentEntry.getValue().getSchema() != null) {
                            // Generate a simple placeholder from schema
                            responseBody = "{ }";
                        }
                        debug("Body: " + responseBody);

                        final RestfulMockDefinitionDTO restfulMockDefinitionDTO = new RestfulMockDefinitionDTO(1, statusCode, contentType, responseBody, 1);

                        // Response Headers
                        if (apiResponse.getHeaders() != null) {
                            debug("Response Headers:");
                            for (Map.Entry<String, io.swagger.v3.oas.models.headers.Header> headerEntry : apiResponse.getHeaders().entrySet()) {
                                final String headerName = headerEntry.getKey();
                                debug("  Header name: " + headerName);
                                restfulMockDefinitionDTO.getResponseHeaders().put(headerName, null);
                            }
                        }

                        dto.getDefinitions().add(restfulMockDefinitionDTO);
                    }
                }
            }
        }

        try {
            restfulMockServiceUtils.preHandleExistingEndpoints(dto, apiImportConfig, user, conflictCtxPath);
            restfulMockService.createEndpoint(dto, user.getSessionToken());
        } catch (RecordNotFoundException e) {
            throw new MockImportException("Unauthorized user access");
        } catch (ValidationException e) {
            throw new MockImportException("A validation issue occurred", e);
        }
    }

    String formatPath(final String resourcePath) {
        // Convert OpenAPI path parameters {param} to smockin format :param
        String formattedPath = resourcePath;
        formattedPath = formattedPath.replace("{", ":");
        formattedPath = formattedPath.replace("}", "");
        return formattedPath;
    }

    void debug(final String msg) {
        if (logger.isDebugEnabled()) {
            logger.debug(msg);
        }
    }

    File loadOpenApiFileFromUpload(final MultipartFile file, final File tempDir) throws MockImportException {
        final String fileName = file.getOriginalFilename();
        final String fileTypeExtension = GeneralUtils.getFileTypeExtension(fileName);

        InputStream fis = null;

        try {
            fis = file.getInputStream();
            final File uploadedFile = new File(tempDir + File.separator + file.getOriginalFilename());
            FileUtils.copyInputStreamToFile(fis, uploadedFile);

            if (".zip".equalsIgnoreCase(fileTypeExtension)) {
                final String parent = uploadedFile.getParent();
                GeneralUtils.unpackArchive(uploadedFile.getAbsolutePath(), parent);

                // Delete uploaded zip file
                boolean deleted = uploadedFile.delete();
                if (!deleted) {
                    logger.warn("Failed to delete uploaded zip file: {}", uploadedFile.getAbsolutePath());
                }

                // Find OpenAPI/Swagger file in unpacked archive
                return Files.find(Paths.get(parent), 5, (path, attr) -> {
                    String name = path.getFileName().toString().toLowerCase();
                    return name.endsWith(".yaml") || name.endsWith(".yml") || name.endsWith(".json");
                })
                .findFirst()
                .orElseThrow(() -> new MockImportException("Error locating OpenAPI/Swagger file within uploaded archive"))
                .toFile();

            } else if (".yaml".equalsIgnoreCase(fileTypeExtension) || 
                       ".yml".equalsIgnoreCase(fileTypeExtension) || 
                       ".json".equalsIgnoreCase(fileTypeExtension)) {
                return uploadedFile;
            } else {
                throw new MockImportException("Unsupported file extension: " + fileName + ". Supported: .yaml, .yml, .json, .zip");
            }

        } catch (IOException e) {
            logger.error("Error reading uploaded OpenAPI file: " + fileName, e);
            throw new MockImportException("Error reading uploaded OpenAPI file: " + fileName);
        } finally {
            GeneralUtils.closeSilently(fis);
        }
    }
}
