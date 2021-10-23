package com.smockin.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by mgallina.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class S3MockFileResponseDTO  {

    private String extId;
    private String name;
    private String mimeType;
    private String content;

}
