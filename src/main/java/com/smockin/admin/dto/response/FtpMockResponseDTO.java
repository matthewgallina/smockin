package com.smockin.admin.dto.response;

import com.smockin.admin.dto.FtpMockDTO;
import com.smockin.admin.persistence.enums.RecordStatusEnum;

import java.util.Date;


/**
 * Created by mgallina.
 */
public class FtpMockResponseDTO extends FtpMockDTO {

    private String extId;
    private Date dateCreated;

    public FtpMockResponseDTO() {

    }

    public FtpMockResponseDTO(final String extId, final String name, final RecordStatusEnum status, final Date dateCreated) {
        super(name,  status);
        this.extId = extId;
        this.dateCreated = dateCreated;
    }

    public String getExtId() {
        return extId;
    }
    public void setExtId(String extId) {
        this.extId = extId;
    }

    public Date getDateCreated() {
        return dateCreated;
    }
    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

}
