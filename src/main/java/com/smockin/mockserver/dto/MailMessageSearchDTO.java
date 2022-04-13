package com.smockin.mockserver.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MailMessageSearchDTO {
    private String sender;
    private String subject;
    private String dateReceived;
}
