package com.CampusCare.complaint;

import org.springframework.data.domain.Page;

import java.util.List;

public record ComplaintPageResponse(
        List<ComplaintResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static ComplaintPageResponse from(Page<ComplaintResponse> page) {
        return new ComplaintPageResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
