package com.smockin.admin.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by mgallina.
 */
@Entity
@Table(name = "MAIL_MOCK_MSG_ATCH_CONT")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MailMockMessageAttachmentContent extends Identifier {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="MAIL_MOCK_MSG_ATCH_ID", nullable = false)
    private MailMockMessageAttachment mailMockMessageAttachment;

    @Column(name="CONTENT", nullable=false, length=VARCHAR_MAX_VALUE)
    private String content;

}
