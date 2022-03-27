package com.smockin.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PagingResponseDTO<R> {

    private long totalRecords;
    private int pageStart;
    private int recordsPerPage;
    private List<R> pageData;

}
