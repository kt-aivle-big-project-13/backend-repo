package com.aivle13.fin_audit_ai.domain.audit.service.fairness;

import com.aivle13.fin_audit_ai.domain.audit.dto.response.fairness.FairnessResultResponse;
import com.aivle13.fin_audit_ai.domain.audit.dto.response.fairness.FairnessRunResponse;
import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.FairnessGroupStatEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.FairnessResultEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.FairnessGroupStatRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.FairnessResultRepository;
import com.aivle13.fin_audit_ai.domain.audit.type.AuditStatus;
import com.aivle13.fin_audit_ai.domain.audit.type.FairnessMetricCode;
import com.aivle13.fin_audit_ai.domain.audit.type.FairnessStatus;
import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import com.aivle13.fin_audit_ai.global.exception.model.AuditFailedException;
import com.aivle13.fin_audit_ai.global.exception.model.AuditNotCompletedException;
import com.aivle13.fin_audit_ai.global.exception.model.AuditNotFoundException;
import com.aivle13.fin_audit_ai.global.exception.model.FairnessResultNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class FairnessResultServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long AUDIT_ID = 21L;

    @Mock
    private AuditRepository auditRepository;

    @Mock
    private FairnessResultRepository fairnessResultRepository;

    @Mock
    private FairnessGroupStatRepository fairnessGroupStatRepository;

    @Mock
    private AuditEntity audit;

    @Mock
    private UserEntity user;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private FairnessResultService self;

    @InjectMocks
    private FairnessResultService fairnessResultService;

    @Captor
    private ArgumentCaptor<List<FairnessResultEntity>> resultCaptor;

    @Captor
    private ArgumentCaptor<List<FairnessGroupStatEntity>> groupStatCaptor;

    @Test
    void returnsFairnessResults() {
        given(auditRepository.findByIdAndUser_Id(AUDIT_ID, USER_ID))
                .willReturn(Optional.of(audit));
        given(audit.getStatus()).willReturn(AuditStatus.COMPLIANT);

        List<FairnessResultEntity> results = List.of(
                createResult("CODE_GENDER", FairnessMetricCode.DEMOGRAPHIC_PARITY,
                        "0.0500", "0.1000", FairnessStatus.PASS)
        );

        given(fairnessResultRepository.findAllByAudit_Id(AUDIT_ID))
                .willReturn(results);
        given(fairnessGroupStatRepository.findAllByAudit_Id(AUDIT_ID))
                .willReturn(List.of());

        FairnessResultResponse response =
                fairnessResultService.getFairness(USER_ID, AUDIT_ID, null);

        assertThat(response.auditId()).isEqualTo(AUDIT_ID);
        assertThat(response.method()).isEqualTo("FAIRLEARN");
        assertThat(response.results()).hasSize(1);
    }

    @Test
    void twoArgOverloadDelegatesToSelfProxyForCaching() {
        FairnessResultResponse expected = FairnessResultResponse.of(AUDIT_ID, List.of(
                createResult("CODE_GENDER", FairnessMetricCode.DEMOGRAPHIC_PARITY,
                        "0.0500", "0.1000", FairnessStatus.PASS)
        ));

        given(self.getFairness(USER_ID, AUDIT_ID, null)).willReturn(expected);

        FairnessResultResponse response = fairnessResultService.getFairness(USER_ID, AUDIT_ID);

        assertThat(response).isSameAs(expected);
        verify(self).getFairness(USER_ID, AUDIT_ID, null);
        verifyNoInteractions(auditRepository, fairnessResultRepository);
    }

    @Test
    void filtersByAttributeWhenProvided() {
        given(auditRepository.findByIdAndUser_Id(AUDIT_ID, USER_ID))
                .willReturn(Optional.of(audit));
        given(audit.getStatus()).willReturn(AuditStatus.COMPLIANT);

        given(fairnessResultRepository.findAllByAudit_IdAndAttribute(AUDIT_ID, "CODE_GENDER"))
                .willReturn(List.of(
                        createResult("CODE_GENDER", FairnessMetricCode.DEMOGRAPHIC_PARITY,
                                "0.0500", "0.1000", FairnessStatus.PASS)
                ));
        given(fairnessGroupStatRepository.findAllByAudit_IdAndAttribute(AUDIT_ID, "CODE_GENDER"))
                .willReturn(List.of());

        FairnessResultResponse response =
                fairnessResultService.getFairness(USER_ID, AUDIT_ID, "CODE_GENDER");

        assertThat(response.results()).hasSize(1);
        assertThat(response.results().get(0).attribute()).isEqualTo("CODE_GENDER");
        verify(fairnessResultRepository).findAllByAudit_IdAndAttribute(AUDIT_ID, "CODE_GENDER");
    }

    @Test
    void throwsWhenAuditDoesNotExist() {
        given(auditRepository.findByIdAndUser_Id(AUDIT_ID, USER_ID))
                .willReturn(Optional.empty());

        assertThatThrownBy(() ->
                fairnessResultService.getFairness(USER_ID, AUDIT_ID, null)
        ).isInstanceOf(AuditNotFoundException.class);
    }

    @Test
    void throwsWhenAuditIsInProgress() {
        given(auditRepository.findByIdAndUser_Id(AUDIT_ID, USER_ID))
                .willReturn(Optional.of(audit));
        given(audit.getStatus()).willReturn(AuditStatus.IN_PROGRESS);

        assertThatThrownBy(() ->
                fairnessResultService.getFairness(USER_ID, AUDIT_ID, null)
        ).isInstanceOf(AuditNotCompletedException.class);

        verifyNoInteractions(fairnessResultRepository);
    }

    @Test
    void throwsWhenNoResultsExist() {
        given(auditRepository.findByIdAndUser_Id(AUDIT_ID, USER_ID))
                .willReturn(Optional.of(audit));
        given(audit.getStatus()).willReturn(AuditStatus.COMPLIANT);

        given(fairnessResultRepository.findAllByAudit_Id(AUDIT_ID))
                .willReturn(List.of());

        assertThatThrownBy(() ->
                fairnessResultService.getFairness(USER_ID, AUDIT_ID, null)
        ).isInstanceOf(FairnessResultNotFoundException.class);
    }

    @Test
    void savesThreeMetricsPerAttributeWithJudgedStatus() {
        given(auditRepository.findById(AUDIT_ID))
                .willReturn(Optional.of(audit));
        given(audit.getUser())
                .willReturn(user);
        given(user.getId())
                .willReturn(USER_ID);

        FairnessRunResponse response = new FairnessRunResponse(
                "21",
                "테스트 감사",
                new FairnessRunResponse.ThresholdInfo(
                        new BigDecimal("0.9"), "target_approval_rate", "validation", "valid_processed.csv"
                ),
                1000,
                new BigDecimal("0.91"),
                "validation_dataset",
                Map.of(
                        // 격차 지표 밴드: PASS ≤ 0.20, REVIEW ≤ 0.40, 초과 FAIL.
                        // Proportional Parity 는 ≥ 0.80 이면 PASS(80% Rule).
                        "CODE_GENDER", new FairnessRunResponse.AttributeFairness(
                                "CODE_GENDER",
                                "REVIEW",
                                new BigDecimal("0.05"),   // DP  → PASS
                                new BigDecimal("0.30"),   // EO  → REVIEW
                                new BigDecimal("-0.50"),  // EOdds → FAIL
                                new BigDecimal("0.85"),   // Proportional → PASS
                                new BigDecimal("0.05"),   // FPR → PASS
                                new BigDecimal("0.30"),   // FDR → REVIEW
                                new BigDecimal("-0.50"),  // FOR → FAIL
                                List.of(),
                                List.of(),
                                null
                        )
                ),
                Map.of(),
                null,
                List.of()
        );

        fairnessResultService.saveFairnessResult(AUDIT_ID, response);

        verify(fairnessResultRepository).deleteAllByAudit_Id(AUDIT_ID);
        verify(fairnessResultRepository).saveAll(resultCaptor.capture());

        assertThat(resultCaptor.getValue())
                .extracting(
                        FairnessResultEntity::getAttribute,
                        FairnessResultEntity::getMetricCode,
                        FairnessResultEntity::getStatus
                )
                .containsExactlyInAnyOrder(
                        tuple("CODE_GENDER", FairnessMetricCode.DEMOGRAPHIC_PARITY, FairnessStatus.PASS),
                        tuple("CODE_GENDER", FairnessMetricCode.EQUAL_OPPORTUNITY, FairnessStatus.REVIEW),
                        tuple("CODE_GENDER", FairnessMetricCode.EQUALIZED_ODDS, FairnessStatus.FAIL),
                        tuple("CODE_GENDER", FairnessMetricCode.FPR_PARITY, FairnessStatus.PASS),
                        tuple("CODE_GENDER", FairnessMetricCode.FDR_PARITY, FairnessStatus.REVIEW),
                        tuple("CODE_GENDER", FairnessMetricCode.FOR_PARITY, FairnessStatus.FAIL),
                        tuple("CODE_GENDER", FairnessMetricCode.PROPORTIONAL_PARITY, FairnessStatus.PASS)
                );
    }

    @Test
    void savesGroupStatsAndPerformancePerAttribute() {
        given(auditRepository.findById(AUDIT_ID)).willReturn(Optional.of(audit));
        given(audit.getUser()).willReturn(user);
        given(user.getId()).willReturn(USER_ID);

        FairnessRunResponse response = new FairnessRunResponse(
                "21", "테스트 감사", null, 0, null, null,
                Map.of(
                        "CODE_GENDER", new FairnessRunResponse.AttributeFairness(
                                "CODE_GENDER", "COMPUTED",
                                new BigDecimal("0.05"), null, null, new BigDecimal("0.85"),
                                null, null, null,
                                List.of(
                                        new FairnessRunResponse.GroupStat(
                                                "M", 780, new BigDecimal("0.90"), new BigDecimal("0.06"),
                                                431, 270, 55, 24, new BigDecimal("0.7352")),
                                        new FairnessRunResponse.GroupStat(
                                                "F", 720, new BigDecimal("0.84"), new BigDecimal("0.07"),
                                                400, 250, 50, 20, null)
                                ),
                                List.of(), null
                        )
                ),
                Map.of(),
                new FairnessRunResponse.Performance(
                        new BigDecimal("0.733"), new BigDecimal("0.6367"), null),
                List.of()
        );

        fairnessResultService.saveFairnessResult(AUDIT_ID, response);

        // 집단별 confusion matrix 가 그대로 저장 엔티티로 옮겨진다.
        verify(fairnessGroupStatRepository).deleteAllByAudit_Id(AUDIT_ID);
        verify(fairnessGroupStatRepository).saveAll(groupStatCaptor.capture());
        assertThat(groupStatCaptor.getValue())
                .extracting(
                        FairnessGroupStatEntity::getAttribute,
                        FairnessGroupStatEntity::getGroupName,
                        FairnessGroupStatEntity::getTp,
                        FairnessGroupStatEntity::getFp,
                        FairnessGroupStatEntity::getTn,
                        FairnessGroupStatEntity::getFn)
                .containsExactlyInAnyOrder(
                        tuple("CODE_GENDER", "M", 431, 270, 55, 24),
                        tuple("CODE_GENDER", "F", 400, 250, 50, 20)
                );

        // 감사셋 전체 성능이 audit 에 기록된다.
        verify(audit).applyPerformance(new BigDecimal("0.733"), new BigDecimal("0.6367"));
    }

    @Test
    void throwsWhenFairnessByAttributeIsEmpty() {
        given(auditRepository.findById(AUDIT_ID))
                .willReturn(Optional.of(audit));

        FairnessRunResponse response = new FairnessRunResponse(
                "21", "테스트 감사", null, 0, null, null,
                Map.of(), Map.of(), null, List.of()
        );

        assertThatThrownBy(() ->
                fairnessResultService.saveFairnessResult(AUDIT_ID, response)
        ).isInstanceOf(AuditFailedException.class);
    }

    @Test
    void skipsMetricsThatAreNullInsteadOfFailingWholeAudit() {
        given(auditRepository.findById(AUDIT_ID))
                .willReturn(Optional.of(audit));
        given(audit.getUser())
                .willReturn(user);
        given(user.getId())
                .willReturn(USER_ID);

        // 일부 집단에 정상/연체 고객이 아예 없어 EQUAL_OPPORTUNITY 등은 계산 불가(null)로
        // 온 상황을 재현 — DEMOGRAPHIC_PARITY 와 PROPORTIONAL_PARITY 만 값이 있다.
        FairnessRunResponse response = new FairnessRunResponse(
                "21", "테스트 감사", null, 0, null, null,
                Map.of(
                        "CODE_GENDER", new FairnessRunResponse.AttributeFairness(
                                "CODE_GENDER", "COMPUTED",
                                new BigDecimal("0.05"), null, null,
                                new BigDecimal("0.85"), null, null, null,
                                List.of(), List.of(), "일부 집단에 정상 또는 연체 고객이 없어 해당 지표를 계산할 수 없음"
                        )
                ),
                Map.of(),
                null,
                List.of()
        );

        fairnessResultService.saveFairnessResult(AUDIT_ID, response);

        verify(fairnessResultRepository).deleteAllByAudit_Id(AUDIT_ID);
        verify(fairnessResultRepository).saveAll(resultCaptor.capture());

        assertThat(resultCaptor.getValue())
                .extracting(
                        FairnessResultEntity::getAttribute,
                        FairnessResultEntity::getMetricCode,
                        FairnessResultEntity::getStatus
                )
                .containsExactlyInAnyOrder(
                        tuple("CODE_GENDER", FairnessMetricCode.DEMOGRAPHIC_PARITY, FairnessStatus.PASS),
                        tuple("CODE_GENDER", FairnessMetricCode.PROPORTIONAL_PARITY, FairnessStatus.PASS)
                );
    }

    @Test
    void throwsWhenAttributeItselfIsNull() {
        given(auditRepository.findById(AUDIT_ID))
                .willReturn(Optional.of(audit));

        FairnessRunResponse response = new FairnessRunResponse(
                "21", "테스트 감사", null, 0, null, null,
                Collections.singletonMap("CODE_GENDER", null),
                Map.of(),
                null,
                List.of()
        );

        assertThatThrownBy(() ->
                fairnessResultService.saveFairnessResult(AUDIT_ID, response)
        ).isInstanceOf(AuditFailedException.class);
    }

    private FairnessResultEntity createResult(
            String attribute,
            FairnessMetricCode metricCode,
            String value,
            String threshold,
            FairnessStatus status
    ) {
        return FairnessResultEntity.of(
                audit,
                attribute,
                metricCode,
                new BigDecimal(value),
                new BigDecimal(threshold),
                status
        );
    }
}