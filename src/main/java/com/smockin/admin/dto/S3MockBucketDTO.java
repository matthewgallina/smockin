package com.smockin.admin.dto;

import com.smockin.admin.persistence.enums.RecordStatusEnum;
import com.smockin.admin.persistence.enums.S3SyncModeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by mgallina.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class S3MockBucketDTO {

    private String bucket;
    private RecordStatusEnum status;
    private S3SyncModeEnum syncMode;

}
