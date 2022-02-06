package com.smockin.admin.persistence.entity;

import lombok.Data;

import javax.persistence.*;

/**
 * Created by mgallina.
 */
@Entity
@Table(name = "MAIL_MOCK_MSG_ATCH_CONT")
@Data
public class MailMockMessageAttachmentContent extends Identifier {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="MAIL_MOCK_MSG_ATCH_ID", nullable = false)
    private MailMockMessageAttachment mailMockMessageAttachment;

    @Column(name="CONTENT", nullable=false, length=Integer.MAX_VALUE)
    private String content;

}
