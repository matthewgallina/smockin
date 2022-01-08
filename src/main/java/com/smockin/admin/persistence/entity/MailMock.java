package com.smockin.admin.persistence.entity;

import com.smockin.admin.persistence.enums.RecordStatusEnum;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import javax.persistence.*;

/**
 * Created by mgallina.
 */
@Entity
@Table(name = "MAIL_MOCK")
@Getter
@Setter
@NoArgsConstructor
public class MailMock extends Identifier {

    @Column(name = "ADDRESS", nullable = false, length = 120, unique = true)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "REC_STATUS", nullable = false, length = 15)
    private RecordStatusEnum status;

    // Anonymous buckets created by connected S3 clients will be created under the default super admin user
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="CREATED_BY", nullable = false)
    private SmockinUser createdBy;

    public MailMock(final String address, final RecordStatusEnum status, final SmockinUser createdBy) {
        this.address = address;
        this.status = status;
        this.createdBy = createdBy;
    }

}
