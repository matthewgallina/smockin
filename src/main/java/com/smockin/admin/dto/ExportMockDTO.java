package com.smockin.admin.dto;

import com.smockin.admin.persistence.enums.ServerTypeEnum;

public class ExportMockDTO {

    private String externalId;
    private ServerTypeEnum mockType;

    public ExportMockDTO() {

    }

    public ExportMockDTO(String externalId, ServerTypeEnum mockType) {
        this.externalId = externalId;
        this.mockType = mockType;
    }

    public String getExternalId() {
        return externalId;
    }
    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public ServerTypeEnum getMockType() {
        return mockType;
    }
    public void setMockType(ServerTypeEnum mockType) {
        this.mockType = mockType;
    }

}
