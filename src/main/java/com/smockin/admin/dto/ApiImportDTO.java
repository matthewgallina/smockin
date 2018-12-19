package com.smockin.admin.dto;

import org.springframework.web.multipart.MultipartFile;

public class ApiImportDTO {

    private MultipartFile file;
    private MockImportConfigDTO config;

    public ApiImportDTO(final MultipartFile file, final MockImportConfigDTO config) {
        this.file = file;
        this.config = config;
    }

    public MultipartFile getFile() {
        return file;
    }
    public MockImportConfigDTO getConfig() {
        return config;
    }

}
