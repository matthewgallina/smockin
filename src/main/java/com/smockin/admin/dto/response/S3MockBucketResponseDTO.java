package com.smockin.admin.dto.response;

import com.smockin.admin.persistence.enums.RecordStatusEnum;
import com.smockin.admin.persistence.enums.S3SyncModeEnum;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by mgallina.
 */
@Data
@NoArgsConstructor
public class S3MockBucketResponseDTO extends S3MockBucketResponseLiteDTO {

    private List<S3MockDirResponseDTO> children = new ArrayList<>();
    private List<S3MockFileResponseDTO> files = new ArrayList<>();

    public S3MockBucketResponseDTO(final String extId,
                                   final String bucket,
                                   final RecordStatusEnum status,
                                   final S3SyncModeEnum syncMode,
                                   final Date dateCreated,
                                   final String createdBy) {
        super(extId, bucket, status, syncMode, dateCreated, createdBy);
    }

}
