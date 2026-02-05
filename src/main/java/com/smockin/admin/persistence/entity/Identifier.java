package com.smockin.admin.persistence.entity;

import com.smockin.utils.GeneralUtils;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * Created by mgallina.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class Identifier {

    final int VARCHAR_MAX_VALUE = 1000000000;

    @Id
    @Column(name = "ID", nullable = false, unique = true, updatable = false)
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @Column(name = "EXT_ID", nullable = false, unique = true, updatable = false)
    private String extId;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "DATE_CREATED", nullable = false)
    private Date dateCreated;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "LAST_UPDATED", nullable = true)
    private Date lastUpdated;


    @PrePersist
    public void onCreate() {

        if (extId == null)
            extId = GeneralUtils.generateUUID();

        if (dateCreated == null)
            dateCreated = GeneralUtils.getCurrentDate();

    }

    @PreUpdate
    public void onUpdate() {
        lastUpdated = GeneralUtils.getCurrentDate();
    }

}
