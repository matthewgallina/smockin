package com.smockin.admin.dto;

import org.springframework.web.multipart.MultipartFile;

public class ApiImportDTO {

    private MultipartFile file;
    private ApiImportConfigDTO config;

    public ApiImportDTO(final MultipartFile file, final ApiImportConfigDTO config) {
        this.file = file;
        this.config = config;
    }

    public MultipartFile getFile() {
        return file;
    }
    public ApiImportConfigDTO getConfig() {
        return config;
    }

}
