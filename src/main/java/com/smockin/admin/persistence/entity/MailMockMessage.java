package com.smockin.admin.persistence.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by mgallina.
 */
@Entity
@Table(name = "MAIL_MOCK_MSG")
@Getter
@Setter
public class MailMockMessage extends Identifier {

    @Column(name = "MAIL_SENDER", nullable = false, length = 200)
    private String from;

    @Column(name = "MAIL_SUBJECT", nullable = false, length = 500)
    private String subject;

    @Column(name = "MAIL_BODY", nullable = false, length = VARCHAR_MAX_VALUE)
    private String body;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "DATE_RECEIVED", nullable = false)
    private Date dateReceived;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="MAIL_MOCK_ID", nullable = false)
    private MailMock mailMock;

    @OneToMany(cascade = CascadeType.ALL,
               fetch = FetchType.LAZY,
               mappedBy = "mailMockMessage",
               orphanRemoval = true)
    private List<MailMockMessageAttachment> attachments = new ArrayList<>();

}
