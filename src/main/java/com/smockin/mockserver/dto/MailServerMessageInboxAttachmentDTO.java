package com.smockin.mockserver.dto;

import lombok.Data;

import java.util.Optional;

@Data
public class MailServerMessageInboxAttachmentDTO extends MailServerMessageInboxAttachmentLiteDTO {

    private String base64Content;

    public MailServerMessageInboxAttachmentDTO(Optional<String> extId, String name, String mimeType, String base64Content) {
        super(extId, name, mimeType);
        this.base64Content = base64Content;
    }
}
