package com.aivle13.fin_audit_ai.domain.audit.service;

import com.aivle13.fin_audit_ai.domain.audit.dto.response.ExplainabilityResponse;
import com.aivle13.fin_audit_ai.domain.audit.dto.response.XaiMetricResponse;
import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.XaiResultEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.XaiResultRepository;
import com.aivle13.fin_audit_ai.domain.audit.type.AuditStatus;
import com.aivle13.fin_audit_ai.domain.audit.type.XaiMetricCode;
import com.aivle13.fin_audit_ai.domain.audit.type.XaiStatus;
import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.InOrder;
import org.springframework.cache.CacheManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import com.aivle13.fin_audit_ai.global.exception.model.AuditNotFoundException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.aivle13.fin_audit_ai.global.exception.model.AuditNotCompletedException;
import com.aivle13.fin_audit_ai.global.exception.model.ExplainabilityResultNotFoundException;
import com.aivle13.fin_audit_ai.global.exception.model.AuditFailedException;


import com.aivle13.fin_audit_ai.domain.audit.dto.request.ExplainabilityResultRequest;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class ExplainabilityServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long AUDIT_ID = 21L;

    @Mock
    private AuditRepository auditRepository;

    @Mock
    private XaiResultRepository xaiResultRepository;

    @Mock
    private AuditEntity audit;

    @Mock
    private UserEntity user;

    @Mock
    private CacheManager cacheManager;

    @InjectMocks
    private ExplainabilityService explainabilityService;

    @org.mockito.Captor
    private ArgumentCaptor<List<XaiResultEntity>> resultCaptor;

    @Test
    void returnsThreeExplainabilityMetrics() {
        given(auditRepository.findByIdAndUser_Id(AUDIT_ID, USER_ID))
                .willReturn(Optional.of(audit));
        given(audit.getStatus())
                .willReturn(AuditStatus.COMPLIANT);

        List<XaiResultEntity> results = List.of(
                createResult(
                        XaiMetricCode.FIDELITY,
                        "0.4843",
                        "0.5000",
                        XaiStatus.REVIEW
                ),
                createResult(
                        XaiMetricCode.SENSITIVE_CONTRIB,
                        "0.0647",
                        "0.2000",
                        XaiStatus.PASS
                ),
                createResult(
                        XaiMetricCode.GLOBAL_STABILITY,
                        "0.9996",
                        "0.7000",
                        XaiStatus.PASS
                )
        );

        given(xaiResultRepository.findAllByAudit_IdAndMetricCodeIn(
                eq(AUDIT_ID),
                anyCollection()
        )).willReturn(results);

        ExplainabilityResponse response =
                explainabilityService.getExplainability(USER_ID, AUDIT_ID);

        assertThat(response.auditId()).isEqualTo(AUDIT_ID);
        assertThat(response.method()).isEqualTo("SHAP");

        assertThat(response.metrics())
                .extracting(XaiMetricResponse::metricCode)
                .containsExactly(
                        XaiMetricCode.SENSITIVE_CONTRIB,
                        XaiMetricCode.GLOBAL_STABILITY,
                        XaiMetricCode.FIDELITY
                );
    }

    @Test
    void returnsExplainabilityMetricsWhileAuditIsInProgress() {
        given(auditRepository.findByIdAndUser_Id(AUDIT_ID, USER_ID))
                .willReturn(Optional.of(audit));

        given(audit.getStatus())
                .willReturn(AuditStatus.IN_PROGRESS);

        given(xaiResultRepository.findAllByAudit_IdAndMetricCodeIn(
                eq(AUDIT_ID),
                anyCollection()
        )).willReturn(List.of(
                createResult(
                        XaiMetricCode.SENSITIVE_CONTRIB,
                        "0.0647",
                        "0.2000",
                        XaiStatus.PASS
                ),
                createResult(
                        XaiMetricCode.GLOBAL_STABILITY,
                        "0.9996",
                        "0.7000",
                        XaiStatus.PASS
                ),
                createResult(
                        XaiMetricCode.FIDELITY,
                        "0.4843",
                        "0.5000",
                        XaiStatus.REVIEW
                )
        ));

        ExplainabilityResponse response =
                explainabilityService.getExplainability(USER_ID, AUDIT_ID);

        assertThat(response.metrics())
                .extracting(XaiMetricResponse::metricCode)
                .containsExactly(
                        XaiMetricCode.SENSITIVE_CONTRIB,
                        XaiMetricCode.GLOBAL_STABILITY,
                        XaiMetricCode.FIDELITY
                );
    }

    @Test
    void throwsWhenAuditDoesNotExist() {
        given(auditRepository.findByIdAndUser_Id(AUDIT_ID, USER_ID))
                .willReturn(Optional.empty());

        assertThatThrownBy(() ->
                explainabilityService.getExplainability(USER_ID, AUDIT_ID)
        ).isInstanceOf(AuditNotFoundException.class);
    }

    @Test
    void throwsWhenAuditIsInProgress() {
        given(auditRepository.findByIdAndUser_Id(AUDIT_ID, USER_ID))
                .willReturn(Optional.of(audit));

        given(audit.getStatus())
                .willReturn(AuditStatus.IN_PROGRESS);

        given(xaiResultRepository.findAllByAudit_IdAndMetricCodeIn(
                eq(AUDIT_ID),
                anyCollection()
        )).willReturn(List.of());

        assertThatThrownBy(() ->
                explainabilityService.getExplainability(USER_ID, AUDIT_ID)
        ).isInstanceOf(AuditNotCompletedException.class);

        verify(xaiResultRepository)
                .findAllByAudit_IdAndMetricCodeIn(
                        eq(AUDIT_ID),
                        anyCollection()
                );
    }

    @Test
    void throwsWhenInProgressAuditHasPartialMetricSet() {
        given(auditRepository.findByIdAndUser_Id(AUDIT_ID, USER_ID))
                .willReturn(Optional.of(audit));

        given(audit.getStatus())
                .willReturn(AuditStatus.IN_PROGRESS);

        given(xaiResultRepository.findAllByAudit_IdAndMetricCodeIn(
                eq(AUDIT_ID),
                anyCollection()
        )).willReturn(List.of(
                createResult(
                        XaiMetricCode.SENSITIVE_CONTRIB,
                        "0.0647",
                        "0.2000",
                        XaiStatus.PASS
                ),
                createResult(
                        XaiMetricCode.GLOBAL_STABILITY,
                        "0.9996",
                        "0.7000",
                        XaiStatus.PASS
                ),
                createResult(
                        XaiMetricCode.GLOBAL_STABILITY,
                        "0.9900",
                        "0.7000",
                        XaiStatus.PASS
                )
        ));

        assertThatThrownBy(() ->
                explainabilityService.getExplainability(USER_ID, AUDIT_ID)
        ).isInstanceOf(AuditNotCompletedException.class);
    }

    @Test
    void throwsWhenAuditIsPending() {
        given(auditRepository.findByIdAndUser_Id(AUDIT_ID, USER_ID))
                .willReturn(Optional.of(audit));

        given(audit.getStatus())
                .willReturn(AuditStatus.PENDING);

        assertThatThrownBy(() ->
                explainabilityService.getExplainability(USER_ID, AUDIT_ID)
        ).isInstanceOf(AuditNotCompletedException.class);

        verifyNoInteractions(xaiResultRepository);
    }

    @Test
    void throwsWhenAuditHasFailed() {
        given(auditRepository.findByIdAndUser_Id(AUDIT_ID, USER_ID))
                .willReturn(Optional.of(audit));

        given(audit.getStatus())
                .willReturn(AuditStatus.FAILED);

        assertThatThrownBy(() ->
                explainabilityService.getExplainability(USER_ID, AUDIT_ID)
        ).isInstanceOf(AuditFailedException.class);

        verifyNoInteractions(xaiResultRepository);
    }

    @Test
    void throwsWhenRequiredMetricsAreMissing() {
        given(auditRepository.findByIdAndUser_Id(AUDIT_ID, USER_ID))
                .willReturn(Optional.of(audit));

        given(audit.getStatus())
                .willReturn(AuditStatus.WARNING);

        given(xaiResultRepository.findAllByAudit_IdAndMetricCodeIn(
                eq(AUDIT_ID),
                anyCollection()
        )).willReturn(List.of(
                createResult(
                        XaiMetricCode.SENSITIVE_CONTRIB,
                        "0.0647",
                        "0.2000",
                        XaiStatus.PASS
                )
        ));

        assertThatThrownBy(() ->
                explainabilityService.getExplainability(USER_ID, AUDIT_ID)
        ).isInstanceOf(ExplainabilityResultNotFoundException.class);
    }

    @Test
    void throwsWhenMetricRowsContainDuplicates() {
        given(auditRepository.findByIdAndUser_Id(AUDIT_ID, USER_ID))
                .willReturn(Optional.of(audit));

        given(audit.getStatus())
                .willReturn(AuditStatus.COMPLIANT);

        given(xaiResultRepository.findAllByAudit_IdAndMetricCodeIn(
                eq(AUDIT_ID),
                anyCollection()
        )).willReturn(List.of(
                createResult(
                        XaiMetricCode.SENSITIVE_CONTRIB,
                        "0.0647",
                        "0.2000",
                        XaiStatus.PASS
                ),
                createResult(
                        XaiMetricCode.GLOBAL_STABILITY,
                        "0.9996",
                        "0.7000",
                        XaiStatus.PASS
                ),
                createResult(
                        XaiMetricCode.FIDELITY,
                        "0.4843",
                        "0.5000",
                        XaiStatus.REVIEW
                ),
                createResult(
                        XaiMetricCode.FIDELITY,
                        "0.4900",
                        "0.5000",
                        XaiStatus.REVIEW
                )
        ));

        assertThatThrownBy(() ->
                explainabilityService.getExplainability(USER_ID, AUDIT_ID)
        ).isInstanceOf(ExplainabilityResultNotFoundException.class);
    }

    @Test
    void savesExplainabilityResult() {
        given(auditRepository.findById(AUDIT_ID))
                .willReturn(Optional.of(audit));
        given(audit.getUser())
                .willReturn(user);
        given(user.getId())
                .willReturn(USER_ID);

        ExplainabilityResultRequest request =
                new ExplainabilityResultRequest(
                        "COMPLETED",
                        "WARNING",
                        new ExplainabilityResultRequest.KeyMetrics(
                                metric("0.0647", "0.2000", "PASS"),
                                metric("0.9996", "0.7000", "PASS"),
                                metric("0.4843", "0.5000", "WARNING")
                        )
                );

        explainabilityService.saveExplainabilityResult(AUDIT_ID, request);

        InOrder inOrder = inOrder(xaiResultRepository);

        inOrder.verify(xaiResultRepository)
                .deleteAllByAudit_IdAndMetricCodeIn(
                        eq(AUDIT_ID),
                        anyCollection()
                );

        inOrder.verify(xaiResultRepository).flush();

        inOrder.verify(xaiResultRepository)
                .saveAll(resultCaptor.capture());

        assertThat(resultCaptor.getValue())
                .extracting(
                        XaiResultEntity::getMetricCode,
                        XaiResultEntity::getStatus
                )
                .containsExactlyInAnyOrder(
                        tuple(
                                XaiMetricCode.SENSITIVE_CONTRIB,
                                XaiStatus.PASS
                        ),
                        tuple(
                                XaiMetricCode.GLOBAL_STABILITY,
                                XaiStatus.PASS
                        ),
                        tuple(
                                XaiMetricCode.FIDELITY,
                                XaiStatus.REVIEW
                        )
                );
    }

    private ExplainabilityResultRequest.Metric metric(
            String value,
            String threshold,
            String status
    ) {
        return new ExplainabilityResultRequest.Metric(
                "test_metric",
                "Test metric",
                new BigDecimal(value),
                new BigDecimal(threshold),
                status
        );
    }

    private XaiResultEntity createResult(
            XaiMetricCode metricCode,
            String value,
            String threshold,
            XaiStatus status
    ) {
        return XaiResultEntity.of(
                audit,
                metricCode,
                new BigDecimal(value),
                new BigDecimal(threshold),
                status
        );
    }
}