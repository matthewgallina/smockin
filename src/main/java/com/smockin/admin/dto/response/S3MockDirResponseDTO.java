package com.smockin.admin.dto.response;

import com.smockin.admin.dto.S3MockDirDTO;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Created by mgallina.
 */
@Data
@NoArgsConstructor
public class S3MockDirResponseDTO extends S3MockDirDTO {

    private String extId;
    private List<S3MockDirResponseDTO> children = new ArrayList<>();
    private List<S3MockFileResponseDTO> files = new ArrayList<>();

    public S3MockDirResponseDTO(final String extId,
                                final String name,
                                final Optional<String> bucketExtId,
                                final Optional<String> parentDirExtId) {
        super(name, bucketExtId, parentDirExtId);
        this.extId = extId;
    }

}
