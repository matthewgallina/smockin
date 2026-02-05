package com.smockin.admin.persistence.entity;

import com.smockin.utils.GeneralUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.Data;

import java.util.Date;


/**
 * Created by mgallina.
 */
@Entity
@Table(name = "APP_CONFIG")
@Data
public class AppConfig {

    @Id
    @Column(name = "ID", nullable = false, unique = true, updatable = false)
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "DB_INSTALL_DATE", nullable = false, updatable = false)
    private Date databaseInstallDate;

    @Column(name = "APP_INSTALL_VERSION", nullable = false, length = 20, updatable = false)
    private String appInstallVersion;

    @Column(name = "APP_CURRENT_VERSION", nullable = false, length = 20)
    private String appCurrentVersion;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "LAST_VERSION_UPDATE", nullable = true)
    private Date lastVersionUpdate;

    public AppConfig() {
    }

    public AppConfig(final String appInstallVersion) {
        this.appInstallVersion = appInstallVersion;
    }


    @PrePersist
    public void onCreate() {
        if (databaseInstallDate == null)
            databaseInstallDate = GeneralUtils.getCurrentDate();
    }

    @PreUpdate
    public void onUpdate() {
        lastVersionUpdate = GeneralUtils.getCurrentDate();
    }

}
