package com.smockin.admin.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LiveLoggingAction<P> {

    private String type;
    private P payload;

}
