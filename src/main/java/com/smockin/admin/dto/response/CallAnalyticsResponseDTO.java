package com.smockin.admin.dto.response;

import com.smockin.admin.dto.CallAnalyticDTO;
import lombok.Data;

import java.util.List;

@Data
public class CallAnalyticsResponseDTO extends CallAnalyticDTO {

    private String externalId;
    private List<CallAnalyticLogResponseDTO> logs;

    public CallAnalyticsResponseDTO(final String externalId,
                                    final String name,
                                    final List<CallAnalyticLogResponseDTO> logs) {
        super(name);
        this.externalId = externalId;
        this.logs = logs;
    }

}
