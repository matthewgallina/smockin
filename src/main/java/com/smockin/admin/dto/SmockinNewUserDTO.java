package com.smockin.admin.dto;

import com.smockin.admin.persistence.enums.SmockinUserRoleEnum;

/**
 * Created by mgallina.
 */
public class SmockinNewUserDTO extends SmockinUserDTO {

    private String password;

    public SmockinNewUserDTO() {
    }

    public SmockinNewUserDTO(String username, String fullName, SmockinUserRoleEnum role, String password) {
        super(username, fullName, role);
        this.password = password;
    }

    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }

}
