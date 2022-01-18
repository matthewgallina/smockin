package com.smockin.admin.persistence.entity;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by mgallina.
 */
@Entity
@Table(name = "MAIL_MOCK_MSG")
@Data
public class MailMockMessage extends Identifier {

    @Column(name = "MAIL_SENDER", nullable = false, length = 200)
    private String from;

    @Column(name = "MAIL_SUBJECT", nullable = false, length = 500)
    private String subject;

    @Column(name = "MAIL_BODY", nullable = false, length = Integer.MAX_VALUE)
    private String body;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "DATE_RECEIVED", nullable = false)
    private Date dateReceived;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="MAIL_MOCK_ID", nullable = false)
    private MailMock mailMock;

}
