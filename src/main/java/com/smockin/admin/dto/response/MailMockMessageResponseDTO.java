package com.smockin.admin.dto.response;

import com.smockin.admin.persistence.entity.Identifier;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;

/**
 * Created by mgallina.
 */
@Data
@AllArgsConstructor
public class MailMockMessageResponseDTO extends Identifier {

    private String extId;
    private String from;
    private String subject;
    private String body;
    private Date dateReceived;

}
