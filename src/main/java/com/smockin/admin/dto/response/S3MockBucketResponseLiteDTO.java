package com.smockin.admin.dto.response;

import com.smockin.admin.dto.S3MockBucketDTO;
import com.smockin.admin.persistence.enums.RecordStatusEnum;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * Created by mgallina.
 */
@Data
@NoArgsConstructor
public class S3MockBucketResponseLiteDTO extends S3MockBucketDTO {

    private String extId;
    private Date dateCreated;
    private String createdBy;

    public S3MockBucketResponseLiteDTO(final String extId,
                                       final String bucket,
                                       final RecordStatusEnum status,
                                       final Date dateCreated,
                                       final String createdBy) {
        super(bucket, status);
        this.extId = extId;
        this.dateCreated = dateCreated;
        this.createdBy = createdBy;
    }

}
