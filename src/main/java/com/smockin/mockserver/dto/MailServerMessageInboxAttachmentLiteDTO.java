package com.smockin.mockserver.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Optional;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MailServerMessageInboxAttachmentLiteDTO {

    private Optional<String> extId;
    private String name;
    private String mimeType;

}
