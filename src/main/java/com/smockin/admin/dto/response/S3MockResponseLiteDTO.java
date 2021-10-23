package com.smockin.admin.dto.response;

import com.smockin.admin.dto.S3MockDTO;
import com.smockin.admin.persistence.enums.RecordStatusEnum;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.Optional;

/**
 * Created by mgallina.
 */
@Data
@NoArgsConstructor
public class S3MockResponseLiteDTO extends S3MockDTO {

    private String extId;
    private Date dateCreated;
    private String createdBy;
    private String userCtxPath;

    public S3MockResponseLiteDTO(final String extId,
                                 final String bucket,
                                 final String userCtxPath,
                                 final RecordStatusEnum status,
                                 final Date dateCreated,
                                 final String createdBy,
                                 final Optional<String> parentExtId) {
        super(bucket, status, parentExtId);
        this.extId = extId;
        this.dateCreated = dateCreated;
        this.createdBy = createdBy;
        this.userCtxPath = userCtxPath;
    }

}
