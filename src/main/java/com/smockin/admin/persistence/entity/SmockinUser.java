package com.smockin.admin.persistence.entity;

import com.smockin.admin.persistence.enums.RecordStatusEnum;
import com.smockin.admin.persistence.enums.SmockinUserRoleEnum;
import lombok.Data;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by mgallina.
 */
@Entity
@Table(name = "SMKN_USER")
@Data
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

    @Column(name = "SESS_TOKEN", nullable = false, length = 350, unique = true)
    private String sessionToken; // JWT based

    @Column(name = "PW_RESET_TOKEN", nullable = false, length = 50, unique = true)
    private String passwordResetToken;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "PW_RESET_TOKEN_EXPIRY")
    private Date passwordResetTokenExpiry;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "createdBy", orphanRemoval = true)
    @OrderBy("path ASC")
    private List<RestfulMock> restfulMocks = new ArrayList<>();

    public SmockinUser() {

    }

    public SmockinUser(String username, String password, String fullName, String ctxPath, SmockinUserRoleEnum role, RecordStatusEnum status, String sessionToken, String passwordResetToken) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.ctxPath = ctxPath;
        this.role = role;
        this.status = status;
        this.sessionToken = sessionToken;
        this.passwordResetToken = passwordResetToken;
    }

}
