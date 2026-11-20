package com.jobportal.kaamsathi.common.util;

import org.springframework.data.domain.PageRequest;

/**
 * One rule for every paged list: ?page (0-based) & ?size from the URL.
 * Missing / bad → defaults, too big → MAX (no error, the list just stays small).
 */
public final class Paging {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 50;

    private Paging() {
    }

    public static PageRequest of(Integer page, Integer size) {
        int p = (page == null || page < 0) ? 0 : page;
        int s = (size == null || size < 1) ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        return PageRequest.of(p, s);
    }
}
