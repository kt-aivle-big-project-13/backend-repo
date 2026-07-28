package com.aivle13.fin_audit_ai.domain.audit.service.selfcheck;

import com.aivle13.fin_audit_ai.domain.audit.dto.request.selfcheck.SelfCheckAnswerSaveRequest;
import com.aivle13.fin_audit_ai.domain.audit.dto.response.selfcheck.SelfCheckAnswerResponse;
import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.SelfCheckAnswerEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.SelfCheckAnswerRepository;
import com.aivle13.fin_audit_ai.domain.audit.type.AuditStatus;
import com.aivle13.fin_audit_ai.domain.audit.type.SelfCheckItemCode;
import com.aivle13.fin_audit_ai.global.exception.model.AuditNotCompletedException;
import com.aivle13.fin_audit_ai.global.exception.model.AuditNotFoundException;
import com.aivle13.fin_audit_ai.global.exception.model.InvalidSelfCheckAnswersException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SelfCheckAnswerService {

    private final AuditRepository auditRepository;
    private final SelfCheckAnswerRepository selfCheckAnswerRepository;

    @Transactional
    public SelfCheckAnswerResponse save(Long userId, Long auditId, SelfCheckAnswerSaveRequest request) {
        AuditEntity audit = findAudit(userId, auditId);

        // 자율점검(STEP4)은 SHAP·Fairlearn 분석이 끝난 뒤 화면에 결과와 함께 노출되는 단계라,
        // 분석이 아직 안 끝난 감사(PENDING/IN_PROGRESS)에는 제출을 막는다.
        if (audit.getStatus() == AuditStatus.PENDING || audit.getStatus() == AuditStatus.IN_PROGRESS) {
            throw new AuditNotCompletedException();
        }

        validateAnswers(request);

        List<SelfCheckAnswerEntity> entities = request.answers().stream()
                .map(item -> SelfCheckAnswerEntity.of(audit, item.itemCode(), item.answer()))
                .toList();

        selfCheckAnswerRepository.deleteAllByAudit_Id(auditId);
        List<SelfCheckAnswerEntity> saved = selfCheckAnswerRepository.saveAll(entities);

        return SelfCheckAnswerResponse.of(auditId, saved);
    }

    public SelfCheckAnswerResponse get(Long userId, Long auditId) {
        findAudit(userId, auditId);

        List<SelfCheckAnswerEntity> answers = selfCheckAnswerRepository.findAllByAudit_Id(auditId);

        return SelfCheckAnswerResponse.of(auditId, answers);
    }

    private AuditEntity findAudit(Long userId, Long auditId) {
        return auditRepository.findByIdAndUser_Id(auditId, userId)
                .orElseThrow(AuditNotFoundException::new);
    }

    // 5개 항목을 중복 없이 모두 제출했는지 검증한다. 일부만 제출하거나 같은 항목을
    // 중복 제출하면 upsert(전체 삭제 후 재삽입) 특성상 나머지 항목이 조용히 사라지므로
    // 여기서 막는다.
    private void validateAnswers(SelfCheckAnswerSaveRequest request) {
        Set<SelfCheckItemCode> submitted = request.answers().stream()
                .map(SelfCheckAnswerSaveRequest.Item::itemCode)
                .collect(Collectors.toSet());

        if (submitted.size() != request.answers().size()
                || !submitted.equals(EnumSet.allOf(SelfCheckItemCode.class))) {
            throw new InvalidSelfCheckAnswersException();
        }
    }
}
