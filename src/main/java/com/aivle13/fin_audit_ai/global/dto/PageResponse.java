package com.aivle13.fin_audit_ai.global.dto;

import org.springframework.data.domain.Page;

import java.util.List;

// 프론트엔드가 1부터 시작하는 페이지 번호를 그대로 쓸 수 있도록 page는 1-base로 변환해 내려준다.
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
    public static <T> PageResponse<T> of(Page<?> page, List<T> content) {
        return new PageResponse<>(
                content,
                page.getNumber() + 1,
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext()
        );
    }
}