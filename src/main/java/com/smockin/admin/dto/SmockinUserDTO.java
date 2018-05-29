package com.smockin.admin.dto;

import com.smockin.admin.persistence.enums.SmockinUserRoleEnum;

/**
 * Created by mgallina.
 */
public class SmockinUserDTO {

    private String extId;
    private String username;
    private String fullName;
    private String ctxPath; // same as username for now.
    private SmockinUserRoleEnum role;

    public SmockinUserDTO() {

    }

    public SmockinUserDTO(String extId, String username, String fullName, String ctxPath, SmockinUserRoleEnum role) {
        this.extId = extId;
        this.username = username;
        this.fullName = fullName;
        this.ctxPath = ctxPath;
        this.role = role;
    }

    public String getExtId() {
        return extId;
    }
    public void setExtId(String extId) {
        this.extId = extId;
    }

    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getCtxPath() {
        return ctxPath;
    }
    public void setCtxPath(String ctxPath) {
        this.ctxPath = ctxPath;
    }

    public SmockinUserRoleEnum getRole() {
        return role;
    }
    public void setRole(SmockinUserRoleEnum role) {
        this.role = role;
    }

}
