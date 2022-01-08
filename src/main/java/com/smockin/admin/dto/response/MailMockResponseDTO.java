package com.smockin.admin.dto.response;

import com.smockin.admin.dto.MailMockDTO;
import com.smockin.admin.persistence.enums.RecordStatusEnum;
import lombok.Data;

import java.util.Date;

@Data
public class MailMockResponseDTO extends MailMockDTO {

    private String externalId;
    private Date dateCreated;

    public MailMockResponseDTO(final String externalId,
                               final Date dateCreated,
                               final String address,
                               final RecordStatusEnum status
                               ) {
        super(address, status);
        this.externalId = externalId;
        this.dateCreated = dateCreated;
    }

}
