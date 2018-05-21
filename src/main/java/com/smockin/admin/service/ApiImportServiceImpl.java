package com.smockin.admin.service;

import com.smockin.admin.exception.ApiImportException;
import org.raml.v2.api.RamlModelBuilder;
import org.raml.v2.api.RamlModelResult;
import org.raml.v2.api.model.common.ValidationResult;
import org.raml.v2.api.model.v10.api.Api;
import org.raml.v2.api.model.v10.resources.Resource;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.io.File;

@Service
@Transactional
public class ApiImportServiceImpl implements ApiImportService {

    public void importApiFile(final File file) throws ApiImportException {

        // TODO

        if (!file.exists()) {
            return;
        }

        final RamlModelResult ramlModelResult = new RamlModelBuilder().buildApi(file);

        if (ramlModelResult.hasErrors()) {

            for (ValidationResult validationResult : ramlModelResult.getValidationResults()) {

                System.out.println(validationResult.getMessage());

            }

        } else {

            final Api api = ramlModelResult.getApiV10();

            System.out.println(api.baseUri().value());
            System.out.println(api.version().value());
            System.out.println(api.baseUriParameters());
            System.out.println(api.protocols());

            System.out.println(" ");

            api.resources().stream().forEach(e -> {

                parseEndpointResource(e);

                System.out.println(" ");

                e.resources().stream().forEach(x -> {

                    parseEndpointResource(x);

                });

            });

        }

    }

    public void parseEndpointResource(final Resource e) {

        System.out.println(e.resourcePath()); // path
        System.out.println(e.uriParameters()); // path variables

        e.methods().forEach(m -> {

            System.out.println(m.method()); // Method

            m.queryParameters().forEach( x -> {
                System.out.println(x.displayName().value());
                System.out.println(x.type());
                System.out.println(x.required());
            });

            m.responses().stream().forEach(r -> {

                System.out.println(r.code().value()); // HTTP response code

                r.body().forEach(b -> {
                    System.out.println(b.displayName().value()); // Content type

                    if (b.example() != null)
                        System.out.println(b.example().value()); // response body

                });


                r.headers().forEach(h -> {
                    System.out.println(h.defaultValue()); // response headers
                });

            });
        });

    }

}
