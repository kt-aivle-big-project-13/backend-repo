package com.aivle13.fin_audit_ai.domain.audit.service.selfcheck;

import com.aivle13.fin_audit_ai.domain.audit.dto.request.selfcheck.SelfCheckAnswerSaveRequest;
import com.aivle13.fin_audit_ai.domain.audit.dto.response.selfcheck.SelfCheckAnswerResponse;
import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.SelfCheckAnswerEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.SelfCheckAnswerRepository;
import com.aivle13.fin_audit_ai.domain.audit.type.selfcheck.SelfCheckItemCode;
import com.aivle13.fin_audit_ai.domain.law.service.mapping.AuditRegulationMappingService;
import com.aivle13.fin_audit_ai.global.exception.model.audit.AuditNotFoundException;
import com.aivle13.fin_audit_ai.global.exception.model.selfcheck.InvalidSelfCheckAnswersException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SelfCheckAnswerService {

    private final AuditRepository auditRepository;
    private final SelfCheckAnswerRepository selfCheckAnswerRepository;
    private final AuditRegulationMappingService auditRegulationMappingService;

    @Transactional
    public SelfCheckAnswerResponse save(Long userId, Long auditId, SelfCheckAnswerSaveRequest request) {
        // 동일 auditId에 대한 동시 저장 요청을 직렬화하기 위해 감사 행에 비관적 쓰기 잠금을 건다.
        // 잠금은 이 트랜잭션이 끝날 때(delete+save 완료 후 커밋)까지 유지되어, 뒤이은 요청은
        // 앞 요청이 끝난 뒤에야 조회를 진행한다.
        AuditEntity audit = findAuditForUpdate(userId, auditId);

        validateAnswers(request);

        List<SelfCheckAnswerEntity> entities = request.answers().stream()
                .map(item -> SelfCheckAnswerEntity.of(audit, item.itemCode(), item.answer()))
                .toList();

        // deleteAllByAudit_Id는 @Modifying이 없는 파생 delete라 영속성 컨텍스트에 삭제만
        // 큐잉되고, Hibernate는 flush 시 삭제보다 삽입을 먼저 내보낸다. flush 없이 바로
        // saveAll을 호출하면 재제출 시(기존 행이 이미 있는 상태) INSERT가 아직 지워지지 않은
        // 기존 행과 (audit_id, item_code) 유니크 제약에서 충돌한다 — 그래서 delete를 먼저
        // DB에 반영시킨 뒤 insert한다.
        selfCheckAnswerRepository.deleteAllByAudit_Id(auditId);
        selfCheckAnswerRepository.flush();
        List<SelfCheckAnswerEntity> saved = selfCheckAnswerRepository.saveAll(entities);

        // 법령 매핑을 같은 트랜잭션 안에서 곧바로(동기) 재계산한다. 예전엔 커밋 후 비동기로
        // 처리했는데, 그러면 이 메서드가 응답을 돌려준 직후 클라이언트가 매칭 조항을 조회했을 때
        // 재계산이 아직 안 끝나 "이전 제출 때의 매칭 결과"가 그대로 보이는 경쟁 상태가 있었다.
        // 매핑표 대입은 가벼운 연산이라 동기로 처리해도 응답 지연이 미미하고, 매핑 계산이
        // 실패하면 자가점검 저장 자체도 함께 롤백되어 더 일관적이다.
        auditRegulationMappingService.mapFromSelfCheckAnswers(auditId);

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

    private AuditEntity findAuditForUpdate(Long userId, Long auditId) {
        return auditRepository.findByIdAndUser_IdForUpdate(auditId, userId)
                .orElseThrow(AuditNotFoundException::new);
    }

    // 21문항 중 일부만 제출해도 된다(미응답 문항은 아예 안 보내는 방식) — 차단하지 않고
    // 프론트에서 경고만 보여준다. 다만 같은 항목을 중복 제출하면 안 된다: 이 메서드는
    // 매번 전체 삭제 후 재삽입(upsert)하므로, 같은 itemCode가 두 번 오면 그중 하나가
    // 조용히 사라진 것처럼 보인다 — 그건 명백한 요청 버그이므로 막는다.
    // 주의: 부분 제출을 허용하므로, 프론트는 매 제출마다 "현재까지 응답한 전체 항목"을
    // 다시 보내야 한다(직전 제출과의 차이분만 보내면 안 보낸 항목이 삭제됨).
    private void validateAnswers(SelfCheckAnswerSaveRequest request) {
        Set<SelfCheckItemCode> submitted = request.answers().stream()
                .map(SelfCheckAnswerSaveRequest.Item::itemCode)
                .collect(Collectors.toSet());

        if (submitted.size() != request.answers().size()) {
            throw new InvalidSelfCheckAnswersException();
        }
    }
}