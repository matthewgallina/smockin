package com.smockin.mockserver.dto;

import com.smockin.admin.persistence.enums.RestMethodEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@AllArgsConstructor
@EqualsAndHashCode
public class LiveBlockPath {

    private RestMethodEnum method;
    private String path;
    private String ownerUserId;

}
