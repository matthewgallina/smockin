package com.smockin.admin.persistence.entity;

import com.smockin.utils.GeneralUtils;
import lombok.Data;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by mgallina.
 */
@MappedSuperclass
@Data
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
