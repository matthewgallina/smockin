package com.smockin.admin.dto;

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
public class S3MockDirDTO {

    private String name;
    private Optional<String> bucketExtId;
    private Optional<String> parentDirExtId;

}
