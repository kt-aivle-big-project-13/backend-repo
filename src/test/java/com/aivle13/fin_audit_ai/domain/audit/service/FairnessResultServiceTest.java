package com.aivle13.fin_audit_ai.domain.audit.service;

import com.aivle13.fin_audit_ai.domain.audit.dto.response.FairnessResultResponse;
import com.aivle13.fin_audit_ai.domain.audit.dto.response.FairnessRunResponse;
import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.FairnessResultEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
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
                        "CODE_GENDER", new FairnessRunResponse.AttributeFairness(
                                "CODE_GENDER",
                                "REVIEW",
                                new BigDecimal("0.05"),
                                new BigDecimal("0.15"),
                                new BigDecimal("-0.30"),
                                List.of(),
                                List.of(),
                                null
                        )
                ),
                Map.of(),
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
                        tuple("CODE_GENDER", FairnessMetricCode.EQUALIZED_ODDS, FairnessStatus.FAIL)
                );
    }

    @Test
    void throwsWhenFairnessByAttributeIsEmpty() {
        given(auditRepository.findById(AUDIT_ID))
                .willReturn(Optional.of(audit));

        FairnessRunResponse response = new FairnessRunResponse(
                "21", "테스트 감사", null, 0, null, null,
                Map.of(), Map.of(), List.of()
        );

        assertThatThrownBy(() ->
                fairnessResultService.saveFairnessResult(AUDIT_ID, response)
        ).isInstanceOf(AuditFailedException.class);
    }

    @Test
    void throwsWhenAttributeMetricValueIsNull() {
        given(auditRepository.findById(AUDIT_ID))
                .willReturn(Optional.of(audit));

        FairnessRunResponse response = new FairnessRunResponse(
                "21", "테스트 감사", null, 0, null, null,
                Map.of(
                        "CODE_GENDER", new FairnessRunResponse.AttributeFairness(
                                "CODE_GENDER", "PASS", null, null, null, List.of(), List.of(), null
                        )
                ),
                Map.of(),
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
