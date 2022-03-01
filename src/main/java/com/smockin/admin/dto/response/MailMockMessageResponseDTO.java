package com.smockin.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;

/**
 * Created by mgallina.
 */
@Data
@AllArgsConstructor
public class MailMockMessageResponseDTO {

    private String extId;
    private String from;
    private String subject;
    private String body;
    private Date dateReceived;
    private int attachmentsCount;

}
