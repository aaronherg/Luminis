package com.aarondevs.luminis.Parameters.Response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaginationResponse<T> {
    private Integer page;
    private Integer size;
    private Long totalRecords;
    private Integer totalPages;
    private Boolean hasPrevious;
    private Boolean hasNext;
    private List<T> records;
}
