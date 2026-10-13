package com.sabarceria.surtek.base;

import org.springframework.data.domain.Page;

import java.util.List;

public class PageResponse<T> {

    private String message;

    private Boolean success;

    private List<T> data;

    private Integer page;

    private Integer perPage;

    public PageResponse(Page<T> page) {

    }

    public PageResponse(Page<?> src, List<T> destDts) {

    }
}
