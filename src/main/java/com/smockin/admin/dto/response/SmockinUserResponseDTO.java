package com.smockin.admin.dto.response;

import com.smockin.admin.dto.SmockinUserDTO;
import com.smockin.admin.persistence.enums.SmockinUserRoleEnum;
import java.util.Date;

public class SmockinUserResponseDTO extends SmockinUserDTO {

    private String extId;
    private Date dateCreated;

    public SmockinUserResponseDTO() {
    }

    public SmockinUserResponseDTO(String extId, Date dateCreated, String username, String fullName, SmockinUserRoleEnum role) {
        super(username, fullName, role);
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
