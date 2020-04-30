package com.smockin.admin.persistence.entity;

import javax.persistence.*;

/**
 * Created by mgallina.
 */
@Entity
@Table(name = "USER_KEY_VALUE_DATA", uniqueConstraints={
        @UniqueConstraint(columnNames = {"KEY", "CREATED_BY"})
})
public class UserKeyValueData extends Identifier {

    @Column(name = "KEY", nullable = false, length = 50)
    private String key;

    @Column(name = "VALUE", nullable = false, length = Integer.MAX_VALUE)
    private String value;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="CREATED_BY", nullable = false)
    private SmockinUser createdBy;

    public String getKey() {
        return key;
    }
    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }
    public void setValue(String value) {
        this.value = value;
    }

    public SmockinUser getCreatedBy() {
        return createdBy;
    }
    public void setCreatedBy(SmockinUser createdBy) {
        this.createdBy = createdBy;
    }
}
