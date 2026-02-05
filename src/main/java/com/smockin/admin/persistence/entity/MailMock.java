package com.smockin.admin.persistence.entity;

import com.smockin.admin.persistence.enums.RecordStatusEnum;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mgallina.
 */
@Entity
@Table(name = "MAIL_MOCK")
@Data
@NoArgsConstructor
public class MailMock extends Identifier {

    @Column(name = "ADDRESS", nullable = false, length = 120, unique = true)
    private String address;

    @Column(name = "SAVE_REC_MAIL", nullable = false)
    private boolean saveReceivedMail;

    @Enumerated(EnumType.STRING)
    @Column(name = "REC_STATUS", nullable = false, length = 15)
    private RecordStatusEnum status;

    // Anonymous buckets created by connected S3 clients will be created under the default super admin user
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="CREATED_BY", nullable = false)
    private SmockinUser createdBy;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "mailMock", orphanRemoval = true)
    private List<MailMockMessage> messages = new ArrayList<>();

    public MailMock(final String address, final RecordStatusEnum status, final SmockinUser createdBy, final boolean saveReceivedMail) {
        this.address = address;
        this.status = status;
        this.createdBy = createdBy;
        this.saveReceivedMail = saveReceivedMail;
    }

}
