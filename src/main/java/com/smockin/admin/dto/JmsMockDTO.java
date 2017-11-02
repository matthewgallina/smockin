package com.smockin.admin.dto;

import com.smockin.admin.persistence.enums.JmsMockTypeEnum;
import com.smockin.admin.persistence.enums.RecordStatusEnum;

/**
 * Created by mgallina.
 */
public class JmsMockDTO {

    private String name;
    private RecordStatusEnum status;
    private JmsMockTypeEnum jmsMockType;

    public JmsMockDTO() {

    }

    public JmsMockDTO(final String name, final RecordStatusEnum status, final JmsMockTypeEnum jmsMockType) {
        this.name = name;
        this.status = status;
        this.jmsMockType = jmsMockType;
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

    public JmsMockTypeEnum getJmsMockType() {
        return jmsMockType;
    }
    public void setJmsMockType(JmsMockTypeEnum jmsMockType) {
        this.jmsMockType = jmsMockType;
    }

}
