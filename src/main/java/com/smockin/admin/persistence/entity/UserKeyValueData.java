package com.smockin.admin.persistence.entity;

import lombok.Data;

import javax.persistence.*;

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
