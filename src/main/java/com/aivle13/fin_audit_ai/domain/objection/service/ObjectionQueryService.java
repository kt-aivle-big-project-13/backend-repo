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
import com.aivle13.fin_audit_ai.domain.objection.dto.response.ObjectionModelEvidenceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ObjectionQueryService {

    private final ObjectionRepository objectionRepository;
    private final ObjectionModelEvidenceService objectionModelEvidenceService;
    private final ObjectionLetterGenerationService
            objectionLetterGenerationService;

    public PageResponse<ObjectionSummaryResponse> list(Long userId, int page, int size, String status, String keyword, String sort) {
        boolean oldestFirst = "oldest".equalsIgnoreCase(sort);

        // 정렬은 Specification 안에서 orderBy로 직접 구성하므로 Pageable에는 Sort를 넘기지 않는다.
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size);

        Specification<ObjectionEntity> spec =
                ObjectionSpecifications.ownedBy(userId)
                        .and(ObjectionSpecifications.keywordContains(keyword))
                        .and(ObjectionSpecifications.statusIn(
                                toEntityStatuses(status)
                        ))
                        .and(ObjectionSpecifications.orderBySubmittedAt(
                                oldestFirst
                        ));

        Page<ObjectionEntity> result = objectionRepository.findAll(spec, pageable);
        List<ObjectionSummaryResponse> content = result.getContent().stream()
                .map(ObjectionSummaryResponse::of)
                .toList();

        return PageResponse.of(result, content);
    }

    public ObjectionDetailResponse getDetail(
            Long userId,
            Long objectionId
    ) {
        ObjectionEntity objection = objectionRepository
                .findByIdAndModel_User_Id(
                        objectionId,
                        userId
                )
                .orElseThrow(ObjectionNotFoundException::new);

        List<ObjectionModelEvidenceResponse> globalModelEvidence =
                objectionModelEvidenceService.findLatestTopEvidence(
                        objection.getModel()
                );

        return ObjectionDetailResponse.of(
                objection,
                globalModelEvidence
        );
    }

    // 처리 결과(decision)에 따른 판단 근거 설명 + 고객 안내문 초안을 생성해 반환한다.
    // 이미 발송된 건은 발송 당시 확정된 내용을 그대로 돌려준다.
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ObjectionDocumentResponse getDocument(
            Long userId,
            Long objectionId,
            ObjectionDecision requestedDecision
    ) {
        ObjectionEntity objection = objectionRepository
                .findByIdAndModel_User_Id(objectionId, userId)
                .orElseThrow(ObjectionNotFoundException::new);

        boolean finalized =
                objection.getStatus() == ObjectionStatus.DELIVERED
                        && objection.getDecision() != null;

        ObjectionDecision decision = finalized
                ? objection.getDecision()
                : requestedDecision != null
                ? requestedDecision
                : ObjectionDecision.REJECT_MAINTAIN;

        String letterBody;

        if (finalized && objection.getDraftContent() != null) {
            letterBody = objection.getDraftContent();
        } else {
            List<ObjectionModelEvidenceResponse> globalModelEvidence =
                    objectionModelEvidenceService.findLatestTopEvidence(
                            objection.getModel()
                    );

            letterBody = objectionLetterGenerationService.generate(
                    objection,
                    decision,
                    globalModelEvidence,
                    false
            );
        }

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

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public LetterBodyResponse regenerateLetter(
            Long userId,
            Long objectionId,
            ObjectionDecision decision
    ) {
        ObjectionEntity objection = objectionRepository
                .findByIdAndModel_User_Id(objectionId, userId)
                .orElseThrow(ObjectionNotFoundException::new);

        if (objection.getStatus() == ObjectionStatus.DELIVERED) {
            throw new ObjectionAlreadyProcessedException();
        }

        ObjectionDecision effectiveDecision = decision != null
                ? decision
                : ObjectionDecision.REJECT_MAINTAIN;

        List<ObjectionModelEvidenceResponse> globalModelEvidence =
                objectionModelEvidenceService.findLatestTopEvidence(
                        objection.getModel()
                );

        String letterBody = objectionLetterGenerationService.generate(
                objection,
                effectiveDecision,
                globalModelEvidence,
                true
        );

        return new LetterBodyResponse(letterBody);
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
}