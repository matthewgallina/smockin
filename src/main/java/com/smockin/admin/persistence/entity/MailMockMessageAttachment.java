package com.smockin.admin.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Created by mgallina.
 */
@Entity
@Table(name = "MAIL_MOCK_MSG_ATCH")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MailMockMessageAttachment extends Identifier {

    @Column(name = "FILE_NAME", nullable = false, length = 200)
    private String name;

    @Column(name = "MIME_TYPE", nullable = false, length = 50)
    private String mimeType;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "mailMockMessageAttachment", orphanRemoval = true)
    private MailMockMessageAttachmentContent mailMockMessageAttachmentContent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="MAIL_MOCK_MSG_ID", nullable = false)
    private MailMockMessage mailMockMessage;

}
