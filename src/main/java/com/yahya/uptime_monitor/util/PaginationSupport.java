package com.yahya.uptime_monitor.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class PaginationSupport {

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    private PaginationSupport() {
    }

    public static boolean isRequested(Integer page, Integer size) {
        return page != null || size != null;
    }

    public static void validate(Integer page, Integer size) {
        if (page != null && page < 0) {
            throw badRequest("Page must be zero or greater");
        }

        if (size != null && (size < 1 || size > MAX_PAGE_SIZE)) {
            throw badRequest("Page size must be between 1 and " + MAX_PAGE_SIZE);
        }
    }

    public static Pageable withSort(Pageable pageable, Sort sort) {
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
