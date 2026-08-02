package com.aivle13.fin_audit_ai.domain.objection.service;

import com.aivle13.fin_audit_ai.domain.objection.dto.response.LetterBodyResponse;
import com.aivle13.fin_audit_ai.domain.objection.dto.response.ObjectionDetailResponse;
import com.aivle13.fin_audit_ai.domain.objection.dto.response.ObjectionDocumentResponse;
import com.aivle13.fin_audit_ai.domain.objection.dto.response.ObjectionSummaryResponse;
import com.aivle13.fin_audit_ai.domain.objection.entity.ObjectionEntity;
import com.aivle13.fin_audit_ai.domain.objection.repository.ObjectionRepository;
import com.aivle13.fin_audit_ai.domain.objection.repository.ObjectionSpecifications;
import com.aivle13.fin_audit_ai.domain.objection.type.ObjectionDecision;
import com.aivle13.fin_audit_ai.domain.objection.type.ObjectionStatus;
import com.aivle13.fin_audit_ai.global.dto.PageResponse;
import com.aivle13.fin_audit_ai.global.exception.BusinessException;
import com.aivle13.fin_audit_ai.global.exception.ErrorCode;
import com.aivle13.fin_audit_ai.global.exception.objection.ObjectionAlreadyProcessedException;
import com.aivle13.fin_audit_ai.global.exception.objection.ObjectionNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ObjectionQueryService {

    private final ObjectionRepository objectionRepository;

    public PageResponse<ObjectionSummaryResponse> list(int page, int size, String status, String keyword, String sort) {
        boolean oldestFirst = "oldest".equalsIgnoreCase(sort);

        // 정렬은 Specification 안에서 orderBy로 직접 구성하므로 Pageable에는 Sort를 넘기지 않는다.
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size);

        Specification<ObjectionEntity> spec = ObjectionSpecifications.keywordContains(keyword)
                .and(ObjectionSpecifications.statusIn(toEntityStatuses(status)))
                .and(ObjectionSpecifications.orderBySubmittedAt(oldestFirst));

        Page<ObjectionEntity> result = objectionRepository.findAll(spec, pageable);
        List<ObjectionSummaryResponse> content = result.getContent().stream()
                .map(ObjectionSummaryResponse::of)
                .toList();

        return PageResponse.of(result, content);
    }

    public ObjectionDetailResponse getDetail(Long objectionId) {
        ObjectionEntity objection = objectionRepository.findById(objectionId)
                .orElseThrow(ObjectionNotFoundException::new);

        return ObjectionDetailResponse.of(objection);
    }

    // 처리 결과(decision)에 따른 판단 근거 설명 + 고객 안내문 초안을 생성해 반환한다.
    // 이미 발송된 건은 발송 당시 확정된 내용을 그대로 돌려준다.
    public ObjectionDocumentResponse getDocument(Long objectionId, ObjectionDecision requestedDecision) {
        ObjectionEntity objection = objectionRepository.findById(objectionId)
                .orElseThrow(ObjectionNotFoundException::new);

        boolean finalized = objection.getStatus() == ObjectionStatus.DELIVERED && objection.getDecision() != null;
        ObjectionDecision decision = finalized
                ? objection.getDecision()
                : (requestedDecision != null ? requestedDecision : ObjectionDecision.REJECT_MAINTAIN);

        String letterBody = finalized && objection.getDraftContent() != null
                ? objection.getDraftContent()
                : buildLetterBody(objection, decision);

        return new ObjectionDocumentResponse(
                objection.getId(),
                objection.getObjectionNo(),
                objection.getCustomerName(),
                decision,
                buildExplanation(objection),
                buildLetterTitle(decision),
                letterBody
        );
    }

    public LetterBodyResponse regenerateLetter(Long objectionId, ObjectionDecision decision) {
        ObjectionEntity objection = objectionRepository.findById(objectionId)
                .orElseThrow(ObjectionNotFoundException::new);

        if (objection.getStatus() == ObjectionStatus.DELIVERED) {
            throw new ObjectionAlreadyProcessedException();
        }

        ObjectionDecision effectiveDecision = decision != null ? decision : ObjectionDecision.REJECT_MAINTAIN;

        return new LetterBodyResponse(buildRegeneratedLetter(objection, effectiveDecision));
    }

    private List<ObjectionStatus> toEntityStatuses(String status) {
        if (status == null || status.isBlank()) {
            return List.of();
        }
        if ("COMPLETED".equalsIgnoreCase(status)) {
            return List.of(ObjectionStatus.DELIVERED);
        }
        if ("WAITING".equalsIgnoreCase(status)) {
            return List.of(ObjectionStatus.DRAFT, ObjectionStatus.REVIEWING, ObjectionStatus.APPROVED);
        }
        throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }

    private String buildExplanation(ObjectionEntity objection) {
        return (objection.getStaffNote() != null && !objection.getStaffNote().isBlank())
                ? objection.getStaffNote()
                : "연계된 판단 근거 정보가 없습니다.";
    }

    private String buildLetterTitle(ObjectionDecision decision) {
        return decision == ObjectionDecision.REEXAMINATION
                ? "신용평가 재심사 안내"
                : "신용평가 결과 이의제기 회신 안내";
    }

    private String buildLetterBody(ObjectionEntity objection, ObjectionDecision decision) {
        if (decision == ObjectionDecision.REEXAMINATION) {
            return "안녕하세요, 고객님. 접수하신 이의제기 내용을 검토한 결과 추가 자료를 바탕으로 재심사를 진행할 예정입니다.";
        }

        return "안녕하세요, 고객님. 신청하신 심사 결과를 안내드립니다.\n\n"
                + buildExplanation(objection)
                + "\n\n관련하여 궁금하신 사항은 이의제기 화면을 통해 다시 문의해주시면 담당자가 성실히 답변드리겠습니다. 감사합니다.";
    }

    private String buildRegeneratedLetter(ObjectionEntity objection, ObjectionDecision decision) {
        if (decision == ObjectionDecision.REEXAMINATION) {
            return "안녕하세요, 고객님. 접수하신 이의제기 내용을 다시 검토했습니다.\n\n검토 결과 추가 자료를 바탕으로 재심사를 진행할 예정입니다. "
                    + "필요한 자료와 후속 절차는 담당자가 별도로 안내드리겠습니다.\n\n감사합니다.";
        }

        return "안녕하세요, 고객님. 접수하신 이의제기 내용을 검토한 결과를 안내드립니다.\n\n"
                + buildExplanation(objection)
                + "\n\n위 사유를 종합적으로 검토하여 기존 심사 결과를 유지하기로 결정했습니다. 추가 문의사항은 이의제기 화면을 통해 남겨주시기 바랍니다.\n\n감사합니다.";
    }
}