package com.smockin.admin.dto.response;

import com.smockin.admin.dto.MailMockDTO;
import com.smockin.admin.persistence.enums.RecordStatusEnum;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
public class MailMockResponseLiteDTO extends MailMockDTO {

    private String externalId;
    private Date dateCreated;
    private long messageCount;

    public MailMockResponseLiteDTO(final String externalId,
                                   final Date dateCreated,
                                   final long messageCount,
                                   final String address,
                                   final RecordStatusEnum status,
                                   final boolean saveReceivedMail
                               ) {
        super(address, status, saveReceivedMail);
        this.externalId = externalId;
        this.dateCreated = dateCreated;
        this.messageCount = messageCount;
    }

}
