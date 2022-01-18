package com.smockin.mockserver.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;

@Data
@AllArgsConstructor
public class MailServerMessageInboxDTO {

    private String from;
    private Date dateReceived;
    private String subject;
    private String body;

}
