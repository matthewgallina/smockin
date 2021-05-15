package com.smockin.mockserver.dto;

import com.smockin.admin.persistence.enums.RestMethodEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Optional;

@Data
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class BlockedPathToRelease {

    private Optional<RestMethodEnum> method;
    private String pathPattern;

}
