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
import jakarta.transaction.Transactional;
import org.apache.commons.io.FileUtils;
import org.raml.v2.api.RamlModelBuilder;
import org.raml.v2.api.RamlModelResult;
import org.raml.v2.api.model.v10.api.Api;
import org.raml.v2.api.model.v10.resources.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
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

    @Autowired
    private RestfulMockServiceUtils restfulMockServiceUtils;

    @Override
    public void importApiDoc(final ApiImportDTO dto, final String token) throws MockImportException, ValidationException {
        logger.debug("importApiDoc (RAML) called");

        validate(dto);

        File tempDir = null;

        try {

            tempDir = Files.createTempDirectory(Long.toString(System.nanoTime())).toFile();
            final Api api = readContent(loadRamlFileFromUpload(dto.getFile(), tempDir));
            final MockImportConfigDTO apiImportConfig = dto.getConfig();
            final String conflictCtxPath = "raml_" + GeneralUtils.createFileNameUniqueTimeStamp();

            debug("Keep existing mocks: " + apiImportConfig.isKeepExisting());
            debug("Keep strategy: " + apiImportConfig.getKeepStrategy());

            debug("Base");
            debug("URI " + api.baseUri().value());
            debug("version " + api.version().value());

            final String defaultMimeType = api.mediaType().stream().findFirst().orElse(() -> "text/plain").value();

            loadInResources(api.resources(), apiImportConfig, userTokenServiceUtils.loadCurrentActiveUser(token), conflictCtxPath, defaultMimeType);
        } catch (RecordNotFoundException ex) {
            throw new MockImportException("Unauthorized user access");
        } catch (MockImportException ex) {
            throw ex;
        } catch (Throwable ex) {
            logger.error("Unexpected error whilst importing RAML API", ex);
            throw new MockImportException("Unexpected error whilst importing RAML API");
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

    Api readContent(final File ramlFile) throws MockImportException {

        final RamlModelResult ramlModelResult = new RamlModelBuilder().buildApi(ramlFile);

        if (ramlModelResult.hasErrors()) {

            final String allErrors = ramlModelResult
                    .getValidationResults().stream().map(vr -> vr.getPath() + " " + vr.getMessage())
                    .collect(Collectors.joining(GeneralUtils.CARRIAGE));

            throw new MockImportException(allErrors);
        }

        return ramlModelResult.getApiV10();
    }

    void loadInResources(final List<Resource> resources, final MockImportConfigDTO apiImportConfig, final SmockinUser user, final String conflictCtxPath, final String defaultMimeType) throws MockImportException {
        resources.stream()
                .forEach(r -> parseResource(r, apiImportConfig, user, conflictCtxPath, defaultMimeType));
    }

    void parseResource(final Resource resource, final MockImportConfigDTO apiImportConfig, final SmockinUser user, final String conflictCtxPath, final String defaultMimeType) throws MockImportException {
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

            final RestfulMockDTO dto = new RestfulMockDTO(path, method, RecordStatusEnum.ACTIVE, RestMockTypeEnum.SEQ,
                    false, 0, 0, 0, false,
                    false, false, false, 0,
                    0, null, null, null, null, null);

            //
            // Responses
            debug("Responses");

            m.responses().stream().forEach(resp -> {

                final int statusCode = Integer.valueOf(resp.code().value());
                debug("HTTP Response Status Code: " + statusCode); // HTTP response code

                // Accounts for response codes which will not have a body such as 204.
                if (resp.body().isEmpty()) {
                    final RestfulMockDefinitionDTO restfulMockDefinitionDTO = new RestfulMockDefinitionDTO(1, statusCode, defaultMimeType, null, 1);
                    dto.getDefinitions().add(restfulMockDefinitionDTO);
                }

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
                restfulMockServiceUtils.preHandleExistingEndpoints(dto, apiImportConfig, user, conflictCtxPath);
                restfulMockService.createEndpoint(dto, user.getSessionToken());
            } catch (RecordNotFoundException e) {
                throw new MockImportException("Unauthorized user access");
            } catch (ValidationException e) {
                throw new MockImportException("A validation issue occurred", e);
            }
        });

        loadInResources(resource.resources(), apiImportConfig, user, conflictCtxPath, defaultMimeType);
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
                uploadedFile.delete();

                return Files.find(Paths.get(parent), 5, (path, attr)
                        -> path.getFileName().toString().toLowerCase().indexOf(".raml") > -1)
                    .findFirst()
                    .orElseThrow(() -> new MockImportException("Error locating raml file within uploaded archive"))
                    .toFile();

            } else if (".raml".equalsIgnoreCase(fileTypeExtension)) {
                return uploadedFile;
            } else {
                throw new MockImportException("Unsupported file extension: " + fileName);
            }

        } catch (IOException e) {
            logger.error("Error reading uploaded RAML file: " + fileName, e);
            throw new MockImportException("Error reading uploaded RAML file: " + fileName);
        } finally {
            GeneralUtils.closeSilently(fis);
        }

    }

}
