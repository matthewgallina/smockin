package com.smockin.admin.dto.response;

import com.smockin.admin.persistence.enums.RecordStatusEnum;
import lombok.Data;

import java.util.Date;

@Data
public class MailMockResponseDTO extends MailMockResponseLiteDTO {

    private PagingResponseDTO<MailMockMessageResponseDTO> messages;

    public MailMockResponseDTO(final String externalId,
                               final Date dateCreated,
                               final int messageCount,
                               final String address,
                               final RecordStatusEnum status,
                               final boolean saveReceivedMail) {
        super(externalId, dateCreated, messageCount, address, status, saveReceivedMail);
    }

}
