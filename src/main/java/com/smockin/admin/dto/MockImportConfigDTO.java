package com.smockin.admin.dto;

import com.smockin.admin.enums.MockImportKeepStrategyEnum;

public class MockImportConfigDTO {

    private boolean keepExisting;
    private MockImportKeepStrategyEnum keepStrategy;

    public MockImportConfigDTO() {

    }

    public MockImportConfigDTO(MockImportKeepStrategyEnum keepStrategy) {
        this.keepExisting = true;
        this.keepStrategy = keepStrategy;
    }

    public boolean isKeepExisting() {
        return keepExisting;
    }
    public void setKeepExisting(boolean keepExisting) {
        this.keepExisting = keepExisting;
    }

    public MockImportKeepStrategyEnum getKeepStrategy() {
        return keepStrategy;
    }
    public void setKeepStrategy(MockImportKeepStrategyEnum keepStrategy) {
        this.keepStrategy = keepStrategy;
    }

}
