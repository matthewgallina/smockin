package com.smockin.admin.dto;

import com.smockin.admin.persistence.enums.RecordStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MailMockDTO {

    private String address;
    private RecordStatusEnum status;

}
