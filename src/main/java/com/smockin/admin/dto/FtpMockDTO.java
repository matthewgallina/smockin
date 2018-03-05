package com.smockin.admin.dto;

import com.smockin.admin.persistence.enums.RecordStatusEnum;

/**
 * Created by mgallina.
 */
public class FtpMockDTO {

    private String name;
    private RecordStatusEnum status;

    public FtpMockDTO() {

    }

    public FtpMockDTO(final String name, final RecordStatusEnum status) {
        this.name = name;
        this.status = status;
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

}
