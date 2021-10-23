package com.smockin.admin.dto;

import com.smockin.admin.persistence.enums.RecordStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Optional;

/**
 * Created by mgallina.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class S3MockDTO {

    private String bucket;
    private RecordStatusEnum status;
    private Optional<String> parentExtId;

}
