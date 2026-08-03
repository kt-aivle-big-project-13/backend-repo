package com.aivle13.fin_audit_ai.domain.chat.service;

import com.aivle13.fin_audit_ai.domain.audit.entity.FairnessGroupStatEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.FairnessResultEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.SelfCheckAnswerEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.ShapFeatureImportanceEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.XaiResultEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.FairnessGroupStatRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.FairnessResultRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.SelfCheckAnswerRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.ShapFeatureImportanceRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.XaiResultRepository;
import com.aivle13.fin_audit_ai.global.ai.dto.ChatAnswerRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 감사 수치를 질의 응답 근거로 조립한다.
 *
 * <p>계산하지 않는다. 이미 저장된 값을 인용 가능한 형태로 옮기기만 한다. 각 항목의
 * {@code reference} 는 답변의 인용에 그대로 쓰이므로 사람이 읽고 출처를 알 수 있어야 한다.
 *
 * <p>별도 빈으로 둔 이유는 같은 클래스 자기호출이 프록시를 거치지 않아
 * {@code @Transactional} 이 적용되지 않기 때문이다. 다섯 종류를 한 트랜잭션에서 읽어야
 * 서로 다른 시점의 값이 섞이지 않는다.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatFactAssembler {

    // SHAP 기여도는 순위가 낮아질수록 답변에 쓸모가 적고 프롬프트만 길어진다.
    private static final int SHAP_TOP_N = 10;

    // 프론트 fairnessData.ts의 ATTRIBUTE_LABEL과 맞춘 한글 표기. 아직 지원하지 않는
    // 보호속성이 들어와도 attributeLabel()이 원래 값으로 대체해 예외 없이 동작한다.
    private static final Map<String, String> ATTRIBUTE_LABELS = Map.of(
            "AGE_GROUP", "연령대",
            "CODE_GENDER", "성별"
    );

    private final FairnessResultRepository fairnessResultRepository;
    private final FairnessGroupStatRepository fairnessGroupStatRepository;
    private final XaiResultRepository xaiResultRepository;
    private final ShapFeatureImportanceRepository shapFeatureImportanceRepository;
    private final SelfCheckAnswerRepository selfCheckAnswerRepository;

    public List<ChatAnswerRequest.AuditFact> assemble(Long auditId) {
        List<ChatAnswerRequest.AuditFact> facts = new ArrayList<>();

        addFairnessResults(facts, auditId);
        addGroupStats(facts, auditId);
        addXaiResults(facts, auditId);
        addShapFeatures(facts, auditId);
        addSelfCheckAnswers(facts, auditId);

        return facts;
    }

    private void addFairnessResults(
            List<ChatAnswerRequest.AuditFact> facts,
            Long auditId
    ) {
        for (FairnessResultEntity result
                : fairnessResultRepository.findAllByAudit_Id(auditId)) {
            facts.add(new ChatAnswerRequest.AuditFact(
                    "AUDIT_METRIC",
                    "%s / %s".formatted(
                            result.getMetricCode().label(),
                            attributeLabel(result.getAttribute())
                    ),
                    text(result.getValue()),
                    "임계값 %s, 상태 %s".formatted(
                            text(result.getThreshold()),
                            result.getStatus().label()
                    )
            ));
        }
    }

    private void addGroupStats(
            List<ChatAnswerRequest.AuditFact> facts,
            Long auditId
    ) {
        // 집단별 값은 항목이 많아 한 집단을 한 근거로 묶는다. 나눠 담으면 근거 수만
        // 늘고 답변이 집단 단위로 읽기 어려워진다.
        for (FairnessGroupStatEntity stat
                : fairnessGroupStatRepository.findAllByAudit_Id(auditId)) {
            facts.add(new ChatAnswerRequest.AuditFact(
                    "GROUP_STAT",
                    "%s=%s".formatted(
                            attributeLabel(stat.getAttribute()),
                            stat.getGroupName()
                    ),
                    "승인율 %s".formatted(text(stat.getApprovalRate())),
                    "인원 %d, 실제연체율 %s, TP %d FP %d TN %d FN %d, AUC %s".formatted(
                            stat.getN(),
                            text(stat.getActualDefaultRate()),
                            stat.getTp(),
                            stat.getFp(),
                            stat.getTn(),
                            stat.getFn(),
                            text(stat.getAuc())
                    )
            ));
        }
    }

    private void addXaiResults(
            List<ChatAnswerRequest.AuditFact> facts,
            Long auditId
    ) {
        for (XaiResultEntity result
                : xaiResultRepository.findAllByAudit_Id(auditId)) {
            facts.add(new ChatAnswerRequest.AuditFact(
                    "AUDIT_METRIC",
                    result.getMetricCode().label(),
                    text(result.getValue()),
                    "임계값 %s, 상태 %s".formatted(
                            text(result.getThreshold()),
                            result.getStatus().label()
                    )
            ));
        }
    }

    private void addShapFeatures(
            List<ChatAnswerRequest.AuditFact> facts,
            Long auditId
    ) {
        List<ShapFeatureImportanceEntity> features =
                shapFeatureImportanceRepository
                        .findAllByAudit_IdOrderByRankAsc(auditId);

        for (ShapFeatureImportanceEntity feature
                : features.stream().limit(SHAP_TOP_N).toList()) {
            facts.add(new ChatAnswerRequest.AuditFact(
                    "AUDIT_METRIC",
                    "SHAP 기여도 %d위 %s".formatted(
                            feature.getRank(),
                            feature.getFeature()
                    ),
                    text(feature.getMeanAbsShap()),
                    "기여 비율 %s, 방향 %s".formatted(
                            text(feature.getContributionRatio()),
                            feature.getDirection()
                    )
            ));
        }
    }

    private void addSelfCheckAnswers(
            List<ChatAnswerRequest.AuditFact> facts,
            Long auditId
    ) {
        for (SelfCheckAnswerEntity answer
                : selfCheckAnswerRepository.findAllByAudit_Id(auditId)) {
            facts.add(new ChatAnswerRequest.AuditFact(
                    "AUDIT_METRIC",
                    "자가점검 · %s".formatted(answer.getItemCode().label()),
                    answer.isAnswer() ? "예" : "아니오",
                    null
            ));
        }
    }

    private String text(Object value) {
        return value == null ? "N/A" : value.toString();
    }

    private String attributeLabel(String attribute) {
        return ATTRIBUTE_LABELS.getOrDefault(attribute, attribute);
    }
}