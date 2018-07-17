package com.smockin.admin.dto.response;

import com.smockin.admin.dto.SmockinUserDTO;
import com.smockin.admin.persistence.enums.SmockinUserRoleEnum;

public class SmockinUserResponseDTO extends SmockinUserDTO {

    private String extId;

    public SmockinUserResponseDTO() {
    }

    public SmockinUserResponseDTO(String extId, String username, String fullName, SmockinUserRoleEnum role) {
        super(username, fullName, role);
        this.extId = extId;
    }

    public String getExtId() {
        return extId;
    }
    public void setExtId(String extId) {
        this.extId = extId;
    }

}
