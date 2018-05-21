package com.smockin.admin.service;

import com.smockin.admin.exception.ApiImportException;

import java.io.File;

public interface ApiImportService {

    void importApiFile(final File file) throws ApiImportException;

}
