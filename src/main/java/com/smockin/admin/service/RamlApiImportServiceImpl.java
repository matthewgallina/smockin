package com.smockin.admin.service;

import com.smockin.admin.dto.ApiImportConfigDTO;
import com.smockin.admin.dto.ApiImportDTO;
import com.smockin.admin.dto.RestfulMockDTO;
import com.smockin.admin.dto.RestfulMockDefinitionDTO;
import com.smockin.admin.exception.ApiImportException;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.dao.RestfulMockDAO;
import com.smockin.admin.persistence.entity.RestfulMock;
import com.smockin.admin.persistence.entity.SmockinUser;
import com.smockin.admin.persistence.enums.RecordStatusEnum;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import com.smockin.admin.persistence.enums.RestMockTypeEnum;
import com.smockin.admin.service.utils.UserTokenServiceUtils;
import com.smockin.utils.GeneralUtils;
import org.apache.commons.io.FileUtils;
import org.raml.v2.api.RamlModelBuilder;
import org.raml.v2.api.RamlModelResult;
import org.raml.v2.api.model.v10.api.Api;
import org.raml.v2.api.model.v10.resources.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.transaction.Transactional;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.stream.Collectors;

@Service("ramlApiImportService")
@Transactional
public class RamlApiImportServiceImpl implements ApiImportService {

    private final Logger logger = LoggerFactory.getLogger(RamlApiImportServiceImpl.class);

    @Autowired
    private RestfulMockService restfulMockService;

    @Autowired
    private RestfulMockDAO restfulMockDAO;

    @Autowired
    private UserTokenServiceUtils userTokenServiceUtils;

    @Override
    public void importApiDoc(final ApiImportDTO dto, final String token) throws ApiImportException, ValidationException {
        logger.debug("importApiDoc (RAML) called");

        validate(dto);

        File tempDir = null;

        try {

            tempDir = Files.createTempDirectory(Long.toString(System.nanoTime())).toFile();
            final Api api = readContent(loadRamlFileFromUpload(dto.getFile(), tempDir));
            final ApiImportConfigDTO apiImportConfig = dto.getConfig();
            final String conflictCtxPath = "raml_" + new SimpleDateFormat(GeneralUtils.UNIQUE_TIMESTAMP_FORMAT)
                    .format(GeneralUtils.getCurrentDate());

            debug("Keep existing mocks: " + apiImportConfig.isKeepExisting());
            debug("Keep strategy: " + apiImportConfig.getKeepStrategy());

            debug("Base");
            debug("URI " + api.baseUri().value());
            debug("version " + api.version().value());

            loadInResources(api.resources(), apiImportConfig, userTokenServiceUtils.loadCurrentUser(token), conflictCtxPath);
        } catch (RecordNotFoundException ex) {
            throw new ApiImportException("Unauthorized user access");
        } catch (ApiImportException ex) {
            throw ex;
        } catch (Throwable ex) {
            logger.error("Unexpected error whilst importing RAML API", ex);
            throw new ApiImportException("Unexpected error whilst importing RAML API");
        } finally {
            if (!FileUtils.deleteQuietly(tempDir)) {
                logger.error("Error deleting temp dir");
            }
        }

    }

    void validate(final ApiImportDTO dto) throws ValidationException {

        if (dto == null)
            throw new ValidationException("No data was provided");

        if (dto.getFile() == null)
            throw new ValidationException("No file found");

        if (dto.getConfig() == null)
            throw new ValidationException("No config found");

    }

    Api readContent(final File ramlFile) throws ApiImportException {

        final RamlModelResult ramlModelResult = new RamlModelBuilder().buildApi(ramlFile);

        if (ramlModelResult.hasErrors()) {

            final String allErrors = ramlModelResult
                    .getValidationResults().stream().map(vr -> vr.getPath() + " " + vr.getMessage())
                    .collect(Collectors.joining("\n"));

            throw new ApiImportException(allErrors);
        }

        return ramlModelResult.getApiV10();
    }

    void loadInResources(final List<Resource> resources, final ApiImportConfigDTO apiImportConfig, final SmockinUser user, final String conflictCtxPath) throws ApiImportException {
        resources.stream()
                .forEach(r -> parseResource(r, apiImportConfig, user, conflictCtxPath));
    }

    void parseResource(final Resource resource, final ApiImportConfigDTO apiImportConfig, final SmockinUser user, final String conflictCtxPath) throws ApiImportException {
        debug("Importing Endpoint...");

        // path
        final String path = formatPath(resource.resourcePath());

        debug("Path " + path);

        resource.methods().forEach(m -> {

            // Method
            final RestMethodEnum method = RestMethodEnum.findByName(m.method());
            debug("Method " + method);

            // Request Parameters
            debug("Request Parameters");

            m.queryParameters().forEach( x -> {
                debug("Param name " + x.name());
                debug("Param Data type " + x.type());
                // TODO we could create a rule here...
                debug("Param Is Required " + x.required());
            });

            // Request Headers
            debug("Request Headers");

            m.headers().forEach(res -> {
                debug("Header name " + res.name());
                if (res.example() != null) {
                    debug("Header value " + res.example().value());
                }
            });

            // Request Body
            debug("Request Body");

            if (m.body() != null) {
                m.body().forEach(b -> {
                    // Content Type
                    if (b.displayName() != null)
                        debug("Content Type " + b.displayName().value());
                    // Body
                    if (b.example() != null) {
                        debug("Body " + b.example().value());
                    }
                });
            }

            final RestfulMockDTO dto = new RestfulMockDTO(path, method, RecordStatusEnum.ACTIVE, RestMockTypeEnum.SEQ, 0, 0, 0, false, false);

            //
            // Responses
            debug("Responses");

            m.responses().stream().forEach(resp -> {

                final int statusCode = Integer.valueOf(resp.code().value());
                debug("HTTP Response Status Code: " + statusCode); // HTTP response code

                // Response Body
                resp.body().forEach(b -> {

                    // Content Type
                    String contentType = (b.displayName() != null) ? b.displayName().value() : null;
                    debug("Content Type " + contentType);

                    // Response body
                    String responseBody = (b.example() != null) ? b.example().value() : null;
                    debug("Body " + responseBody);

                    final RestfulMockDefinitionDTO restfulMockDefinitionDTO = new RestfulMockDefinitionDTO(1, statusCode, contentType, responseBody, 1);

                    // Response Headers
                    debug("Response Headers");

                    resp.headers().forEach(res -> {

                        final String headerName = res.name();
                        debug("Header name " + headerName);

                        final String headerValue = (res.example() != null) ? res.example().value() : null;
                        debug("Header value " + headerValue);

                        restfulMockDefinitionDTO.getResponseHeaders().put(headerName, headerValue);

                    });

                    dto.getDefinitions().add(restfulMockDefinitionDTO);

                });

            });

            try {
                handleExistingEndpoints(dto, apiImportConfig, user, conflictCtxPath);
                restfulMockService.createEndpoint(dto, user.getSessionToken());
            } catch (RecordNotFoundException e) {
                throw new ApiImportException("Unauthorized user access");
            }
        });

        loadInResources(resource.resources(), apiImportConfig, user, conflictCtxPath);
    }

    void handleExistingEndpoints(final RestfulMockDTO dto, final ApiImportConfigDTO apiImportConfig, final SmockinUser user, final String conflictCtxPath) {

        final RestfulMock existingRestFulMock = restfulMockDAO.findByPathAndMethodAndUser(dto.getPath(), dto.getMethod(), user);

        if (existingRestFulMock == null) {
            return;
        }

        if (!apiImportConfig.isKeepExisting()) {
            restfulMockDAO.delete(existingRestFulMock);
            restfulMockDAO.flush();
            return;
        }

        switch (apiImportConfig.getKeepStrategy()) {
            case RENAME_EXISTING:
                existingRestFulMock.setPath("/" + conflictCtxPath + existingRestFulMock.getPath());
                restfulMockDAO.save(existingRestFulMock);
                break;
            case RENAME_NEW:
                dto.setPath("/" + conflictCtxPath + dto.getPath());
                break;
        }

    }

    String formatPath(final String resourcePath) {

        String formattedPath = resourcePath;
        formattedPath = formattedPath.replace("{", ":");
        formattedPath = formattedPath.replace("}", "");

        return formattedPath;
    }

    void debug(final String msg) {

        if (logger.isDebugEnabled())
            logger.debug(msg);

    }

    File loadRamlFileFromUpload(final MultipartFile file, final File tempDir) {

        final String fileName = file.getName();
        final String fileTypeExtension = GeneralUtils.getFileTypeExtension(fileName);

        try {

            final File uploadedFile = new File(tempDir + File.separator + file.getName());
            FileUtils.copyInputStreamToFile(file.getInputStream(), uploadedFile);

            if (".zip".equalsIgnoreCase(fileTypeExtension)) {

                final String parent = uploadedFile.getParent();

                GeneralUtils.unpackArchive(uploadedFile.getAbsolutePath(), parent);

                // Delete uploaded zip file
                uploadedFile.delete();

                return Files.find(Paths.get(parent), 5, (path, attr)
                        -> path.getFileName().toString().toLowerCase().indexOf(".raml") > -1)
                    .findFirst()
                    .orElseThrow(() -> new ApiImportException("Error locating raml file within uploaded archive"))
                    .toFile();

            } else if (".raml".equalsIgnoreCase(fileTypeExtension)) {
                return uploadedFile;
            } else {
                throw new ApiImportException("Unsupported file extension: " + fileName);
            }

        } catch (IOException e) {
            logger.error("Error reading uploaded RAML file: " + fileName, e);
            throw new ApiImportException("Error reading uploaded RAML file: " + fileName);
        }

    }

}
