package com.smockin.admin.persistence.entity;

import com.smockin.utils.GeneralUtils;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by mgallina.
 */
@MappedSuperclass
public abstract class Identifier {

    @Id
    @Column(name = "ID", nullable = false, unique = true, updatable = false)
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @Column(name = "EXT_ID", nullable = false, unique = true, updatable = false)
    private String extId;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "DATE_CREATED", nullable = false)
    private Date dateCreated;

    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
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

    @PrePersist
    public void onCreate() {

        if (extId == null)
            extId = GeneralUtils.generateUUID();

        if (dateCreated == null)
            dateCreated = GeneralUtils.getCurrentDate();

    }

}
