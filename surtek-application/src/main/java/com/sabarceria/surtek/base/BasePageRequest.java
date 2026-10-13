package com.sabarceria.surtek.base;

import lombok.Data;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Data

public class BasePageRequest {
    private String search;

    private Integer page;

    private Integer size;

    public PageRequest pageRequest() {
        if (size == null || size == 0) {
            size = 10;
        }
        if (page == null || page == 0) {
            page = 0;
        } else {
            page = page - 1;
        }
        return PageRequest.of(page, size, Sort.by("id").descending());
    }
}
