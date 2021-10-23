package com.smockin.admin.dto.response;

import com.smockin.admin.persistence.enums.RecordStatusEnum;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Created by mgallina.
 */
@Data
@NoArgsConstructor
public class S3MockResponseDTO extends S3MockResponseLiteDTO {

    private List<S3MockResponseDTO> children = new ArrayList<>();
    private List<S3MockFileResponseDTO> files = new ArrayList<>();

    public S3MockResponseDTO(final String extId,
                             final String bucket,
                             final String userCtxPath,
                             final RecordStatusEnum status,
                             final Date dateCreated,
                             final String createdBy,
                             final Optional<String> parentExtId) {
        super(extId, bucket, userCtxPath, status, dateCreated, createdBy, parentExtId);
    }

}
