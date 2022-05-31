package com.smockin.admin.dto.response;

import com.smockin.admin.dto.CallAnalyticDTO;
import lombok.Data;

@Data
public class CallAnalyticsResponseLiteDTO extends CallAnalyticDTO {

    private String externalId;
    private int restCallCount;
    private int s3ActionCount;
    private int mailReceivedCount;

    public CallAnalyticsResponseLiteDTO(final String externalId,
                                        final int restCallCount,
                                        final int s3ActionCount,
                                        final int mailReceivedCount,
                                        final String name) {
        super(name);
        this.externalId = externalId;
        this.restCallCount = restCallCount;
        this.s3ActionCount = s3ActionCount;
        this.mailReceivedCount = mailReceivedCount;
    }

}
