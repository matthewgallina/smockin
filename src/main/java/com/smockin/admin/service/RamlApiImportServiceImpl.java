package com.smockin.admin.service;

import com.smockin.admin.dto.ApiImportConfigDTO;
import com.smockin.admin.dto.ApiImportDTO;
import com.smockin.admin.dto.RestfulMockDTO;
import com.smockin.admin.dto.RestfulMockDefinitionDTO;
import com.smockin.admin.exception.ApiImportException;
import com.smockin.admin.exception.RecordNotFoundException;
import com.smockin.admin.exception.ValidationException;
import com.smockin.admin.persistence.enums.RecordStatusEnum;
import com.smockin.admin.persistence.enums.RestMethodEnum;
import com.smockin.admin.persistence.enums.RestMockTypeEnum;
import org.raml.v2.api.RamlModelBuilder;
import org.raml.v2.api.RamlModelResult;
import org.raml.v2.api.model.v10.api.Api;
import org.raml.v2.api.model.v10.resources.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service("ramlApiImportService")
@Transactional
public class RamlApiImportServiceImpl implements ApiImportService {

    private final Logger logger = LoggerFactory.getLogger(RamlApiImportServiceImpl.class);

    @Autowired
    private RestfulMockService restfulMockService;

    @Override
    public void importApiDoc(final ApiImportDTO dto, final String token) throws ApiImportException, ValidationException {

        validate(dto);

        final Api api = readContent(dto.getContent());
        final ApiImportConfigDTO apiImportConfig = dto.getConfig();

        debug("Keep existing mocks: " + apiImportConfig.isKeepExisting());
        debug("Keep strategy: " + apiImportConfig.getKeepStrategy());

        debug("Base");
        debug("URI " + api.baseUri().value());
        debug("version " + api.version().value());

        loadInResources(api.resources(), apiImportConfig, token);

    }

    void validate(final ApiImportDTO dto) throws ValidationException {

        if (dto == null)
            throw new ValidationException("No data was provided");

        if (dto.getContent() == null)
            throw new ValidationException("No API doc content found");

    }

    Api readContent(final String fileContent) throws ApiImportException {

        final RamlModelResult ramlModelResult = new RamlModelBuilder().buildApi(fileContent, "");

        if (ramlModelResult.hasErrors()) {

            final String allErrors = ramlModelResult
                    .getValidationResults().stream().map(vr -> vr.getPath() + " " + vr.getMessage())
                    .collect(Collectors.joining("\n"));

            throw new ApiImportException(allErrors);
        }

        return ramlModelResult.getApiV10();
    }

    void loadInResources(final List<Resource> resources, final ApiImportConfigDTO apiImportConfig, final String token) throws ApiImportException {
        resources.stream()
                .forEach(r -> parseResource(r, apiImportConfig, token));
    }

    void parseResource(final Resource resource, final ApiImportConfigDTO apiImportConfig, final String token) throws ApiImportException {
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
                restfulMockService.createEndpoint(dto, token);
            } catch (RecordNotFoundException e) {
                throw new ApiImportException("Record not found");
            }
        });

        loadInResources(resource.resources(), apiImportConfig, token);
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

}
