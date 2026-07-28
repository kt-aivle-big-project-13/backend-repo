package com.aivle13.fin_audit_ai.domain.board.dto.request;

import jakarta.validation.constraints.NotBlank;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public record PostUpdateRequest(
        @NotBlank(message = "제목을 입력해주세요.")
        String title,

        @NotBlank(message = "내용을 입력해주세요.")
        String content,

        // 새로 추가할 첨부파일
        List<MultipartFile> files,

        // 삭제할 기존 첨부파일 id 목록
        List<Long> deleteAttachmentIds
) {
}