package com.smockin.admin.dto;

import com.smockin.admin.enums.ApiImportType;

public class ApiImportDTO {

    private ApiImportType type;
    private String content;
    private ApiImportConfigDTO config;

    public ApiImportDTO() {

    }

    public ApiImportDTO(ApiImportType type, String content, ApiImportConfigDTO config) {
        this.type = type;
        this.content = content;
        this.config = config;
    }

    public ApiImportType getType() {
        return type;
    }
    public void setType(ApiImportType type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }
    public void setContent(String content) {
        this.content = content;
    }

    public ApiImportConfigDTO getConfig() {
        return config;
    }
    public void setConfig(ApiImportConfigDTO config) {
        this.config = config;
    }

}
