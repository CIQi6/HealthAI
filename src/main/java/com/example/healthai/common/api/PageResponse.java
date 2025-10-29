package com.example.healthai.common.api;

import java.util.Collections;
import java.util.List;

public record PageResponse<T>(List<T> content, long totalElements, int page, int size) {

    public PageResponse {
        if (content == null) {
            content = Collections.emptyList();
        }
        if (size <= 0) {
            size = 1;
        }
    }

    public long totalPages() {
        if (size <= 0) {
            return 0;
        }
        return (totalElements + size - 1) / size;
    }
}
