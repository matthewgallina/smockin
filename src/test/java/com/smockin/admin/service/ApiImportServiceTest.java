package com.smockin.admin.service;

import com.smockin.admin.exception.ApiImportException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

public class ApiImportServiceTest {

    @Rule public ExpectedException expected = ExpectedException.none();

    @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

    private ApiImportService apiImportService;

    private File f = null;

    @Before
    public void setUp() throws URISyntaxException, IOException {

        final URL url = this.getClass().getClassLoader().getResource("hello-api.raml");

        apiImportService = new ApiImportServiceImpl();

        f = new File(url.getPath());

    }

    @Test
    public void importApiFilePass() throws ApiImportException {

        apiImportService.importApiFile(f);

    }

}
