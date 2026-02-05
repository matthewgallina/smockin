package com.smockin.admin.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

/**
 * Created by mgallina.
 */
@Entity
@Table(name = "USER_KEY_VALUE_DATA", uniqueConstraints={
        @UniqueConstraint(columnNames = {"USER_KEY", "CREATED_BY"})
})
@Data
public class UserKeyValueData extends Identifier {

    @Column(name = "USER_KEY", nullable = false, length = 50)
    private String key;

    @Column(name = "USER_VALUE", nullable = false, length = VARCHAR_MAX_VALUE)
    private String value;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="CREATED_BY", nullable = false)
    private SmockinUser createdBy;

}
