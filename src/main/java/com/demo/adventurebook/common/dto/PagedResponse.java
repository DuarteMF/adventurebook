package com.demo.adventurebook.common.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Slimmed-down paged response that exposes only the fields a client actually needs,
 * instead of the verbose Spring {@link Page} serialization.
 *
 * @param <T> the type of items in the page
 */
public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static <T> PagedResponse<T> of(Page<T> page) {
        return new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
