package com.smockin.admin.dto;

import com.smockin.admin.persistence.enums.JmsMockTypeEnum;
import com.smockin.admin.persistence.enums.RecordStatusEnum;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mgallina.
 */
public class JmsMockDTO {

    private String name;
    private RecordStatusEnum status;
    private JmsMockTypeEnum mockType;
    private List<JmsMockDefinitionDTO> mockDefinitions = new ArrayList<JmsMockDefinitionDTO>();

    public JmsMockDTO() {

    }

    public JmsMockDTO(String name, RecordStatusEnum status, JmsMockTypeEnum mockType) {
        this.name = name;
        this.status = status;
        this.mockType = mockType;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public RecordStatusEnum getStatus() {
        return status;
    }
    public void setStatus(RecordStatusEnum status) {
        this.status = status;
    }

    public JmsMockTypeEnum getMockType() {
        return mockType;
    }
    public void setMockType(JmsMockTypeEnum mockType) {
        this.mockType = mockType;
    }

    public List<JmsMockDefinitionDTO> getMockDefinitions() {
        return mockDefinitions;
    }
    public void setMockDefinitions(List<JmsMockDefinitionDTO> mockDefinitions) {
        this.mockDefinitions = mockDefinitions;
    }

}
