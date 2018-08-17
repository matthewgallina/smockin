package com.smockin.admin.dto;

import com.smockin.admin.enums.ApiKeepStrategyEnum;

public class ApiImportConfigDTO {

    private boolean keepExisting;
    private ApiKeepStrategyEnum keepStrategy;

    public ApiImportConfigDTO() {

    }

    public ApiImportConfigDTO(ApiKeepStrategyEnum keepStrategy) {
        this.keepExisting = true;
        this.keepStrategy = keepStrategy;
    }

    public boolean isKeepExisting() {
        return keepExisting;
    }
    public void setKeepExisting(boolean keepExisting) {
        this.keepExisting = keepExisting;
    }

    public ApiKeepStrategyEnum getKeepStrategy() {
        return keepStrategy;
    }
    public void setKeepStrategy(ApiKeepStrategyEnum keepStrategy) {
        this.keepStrategy = keepStrategy;
    }

}
