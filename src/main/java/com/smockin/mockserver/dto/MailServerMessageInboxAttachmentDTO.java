package com.smockin.mockserver.dto;

import lombok.Data;

import java.util.Optional;

@Data
public class MailServerMessageInboxAttachmentDTO extends MailServerMessageInboxAttachmentLiteDTO {

    private String content;

    public MailServerMessageInboxAttachmentDTO(Optional<String> extId, String name, String mimeType, String content) {
        super(extId, name, mimeType);
        this.content = content;
    }
}
