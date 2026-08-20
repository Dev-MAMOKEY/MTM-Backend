package com.likelion.mtm.global.common;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 페이징 응답 공통 형식.
 * 팀 컨벤션상 Spring의 Page를 그대로 노출하지 않는다 — Page는 직렬화 결과가 스프링 버전에 따라
 * 달라질 수 있고, 클라이언트가 쓰지 않는 필드까지 딸려 나간다.
 *
 * @param content       현재 페이지의 항목들
 * @param page          현재 페이지 번호 (0부터 시작)
 * @param size          페이지당 항목 수
 * @param totalElements 전체 항목 수
 * @param totalPages    전체 페이지 수
 * @param first         첫 페이지 여부
 * @param last          마지막 페이지 여부
 */
public record PageResponseDTO<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    /**
     * 이미 DTO로 변환된 목록과 원본 Page의 페이지 정보를 합친다.
     * 엔티티를 DTO로 바꾸는 과정에서 추가 조회가 필요한 경우(예: 이미지 URL)
     * Page.map()으로는 처리하기 어려워 content를 따로 받는다.
     */
    public static <T> PageResponseDTO<T> from(List<T> content, Page<?> page) {
        return new PageResponseDTO<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
