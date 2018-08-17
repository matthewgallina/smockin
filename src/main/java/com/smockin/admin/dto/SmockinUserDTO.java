package com.smockin.admin.dto;

import com.smockin.admin.persistence.enums.SmockinUserRoleEnum;

/**
 * Created by mgallina.
 */
public class SmockinUserDTO {

    private String username;
    private String fullName;
    private SmockinUserRoleEnum role;

    public SmockinUserDTO() {

    }

    public SmockinUserDTO(String username, String fullName, SmockinUserRoleEnum role) {
        this.username = username;
        this.fullName = fullName;
        this.role = role;
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

    public SmockinUserRoleEnum getRole() {
        return role;
    }
    public void setRole(SmockinUserRoleEnum role) {
        this.role = role;
    }

}
