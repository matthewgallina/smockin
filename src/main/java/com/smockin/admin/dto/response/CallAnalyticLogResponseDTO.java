package com.smockin.admin.dto.response;

import com.smockin.admin.persistence.enums.ServerTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;

@Data
@AllArgsConstructor
public class CallAnalyticLogResponseDTO {

    private ServerTypeEnum serverType;
    private String path;
    private String result;
    private Date received;

}
