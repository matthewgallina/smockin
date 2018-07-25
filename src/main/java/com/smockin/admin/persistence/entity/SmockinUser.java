package com.smockin.admin.persistence.entity;

import com.smockin.admin.persistence.enums.RecordStatusEnum;
import com.smockin.admin.persistence.enums.SmockinUserRoleEnum;

import javax.persistence.*;

/**
 * Created by mgallina.
 */
@Entity
@Table(name = "SMKN_USER")
public class SmockinUser extends Identifier {

    @Column(name = "USER_NAME", nullable = false, length = 35, unique = true)
    private String username;

    @Column(name = "PASSWORD", nullable = false, length = 100)
    private String password; // digest encrypted

    @Column(name = "FULL_NAME", nullable = false, length = 100)
    private String fullName;

    @Column(name = "CTX_PATH", nullable = false, length = 50, unique = true)
    private String ctxPath; // just copy username to here for now.

    @Enumerated(EnumType.STRING)
    @Column(name = "ROLE", nullable = false, length = 10)
    private SmockinUserRoleEnum role;

    @Enumerated(EnumType.STRING)
    @Column(name = "REC_STATUS", nullable = false, length = 15)
    private RecordStatusEnum status;

    @Column(name = "SESS_TOKEN", nullable = false, length = 250, unique = true)
    private String sessionToken; // JWT based

    public SmockinUser() {

    }

    public SmockinUser(String username, String password, String fullName, String ctxPath, SmockinUserRoleEnum role, RecordStatusEnum status, String sessionToken) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.ctxPath = ctxPath;
        this.role = role;
        this.status = status;
        this.sessionToken = sessionToken;
    }

    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
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

    public RecordStatusEnum getStatus() {
        return status;
    }
    public void setStatus(RecordStatusEnum status) {
        this.status = status;
    }

    public String getSessionToken() {
        return sessionToken;
    }
    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

}
