package com.aivle13.fin_audit_ai.domain.objection.service;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.entity.ShapFeatureImportanceEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.ShapFeatureImportanceRepository;
import com.aivle13.fin_audit_ai.domain.audit.type.AuditStatus;
import com.aivle13.fin_audit_ai.domain.model.entity.AiModelEntity;
import com.aivle13.fin_audit_ai.domain.objection.dto.response.ObjectionModelEvidenceResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ObjectionModelEvidenceServiceTest {

    @Mock
    private AuditRepository auditRepository;

    @Mock
    private ShapFeatureImportanceRepository
            shapFeatureImportanceRepository;

    @InjectMocks
    private ObjectionModelEvidenceService
            objectionModelEvidenceService;

    @Test
    void returnsLatestAuditGlobalShapTopEvidence() {
        AiModelEntity model = mock(AiModelEntity.class);
        AuditEntity audit = mock(AuditEntity.class);
        ShapFeatureImportanceEntity first = feature(
                1,
                "EXT_SOURCE_2",
                "0.136000",
                "0.240000",
                "RISK_DECREASE"
        );
        ShapFeatureImportanceEntity second = feature(
                2,
                "AMT_CREDIT",
                "0.099000",
                "0.180000",
                "RISK_INCREASE"
        );

        when(model.getId()).thenReturn(7L);
        when(audit.getId()).thenReturn(12L);
        when(auditRepository
                .findLatestByModelIdAndStatuses(
                        eq(7L),
                        anyList(),
                        eq(PageRequest.of(0, 1))
                ))
                .thenReturn(List.of(audit));
        when(shapFeatureImportanceRepository
                .findTop5ByAudit_IdAndSensitiveFalseOrderByRankAsc(12L))
                .thenReturn(List.of(first, second));

        List<ObjectionModelEvidenceResponse> result =
                objectionModelEvidenceService.findLatestTopEvidence(model);

        assertThat(result).hasSize(2);

        assertThat(result.get(0).rank()).isEqualTo(1);
        assertThat(result.get(0).feature())
                .isEqualTo("EXT_SOURCE_2");
        assertThat(result.get(0).displayName())
                .isEqualTo("외부 평가 지표 2");
        assertThat(result.get(0).direction())
                .isEqualTo("RISK_DECREASE");

        assertThat(result.get(1).rank()).isEqualTo(2);
        assertThat(result.get(1).feature())
                .isEqualTo("AMT_CREDIT");
        assertThat(result.get(1).displayName())
                .isEqualTo("신청 신용 금액");
        assertThat(result.get(1).direction())
                .isEqualTo("RISK_INCREASE");

        verify(auditRepository)
                .findLatestByModelIdAndStatuses(
                        eq(7L),
                        argThat(statuses ->
                                statuses.containsAll(List.of(
                                        AuditStatus.COMPLIANT,
                                        AuditStatus.WARNING,
                                        AuditStatus.NON_COMPLIANT,
                                        AuditStatus.UNCONFIRMED
                                ))
                        ),
                        eq(PageRequest.of(0, 1))
                );
    }

    @Test
    void returnsEmptyWhenCompletedAuditDoesNotExist() {
        AiModelEntity model = mock(AiModelEntity.class);

        when(model.getId()).thenReturn(7L);
        when(auditRepository
                .findLatestByModelIdAndStatuses(
                        eq(7L),
                        anyList(),
                        eq(PageRequest.of(0, 1))
                ))
                .thenReturn(List.of());

        List<ObjectionModelEvidenceResponse> result =
                objectionModelEvidenceService.findLatestTopEvidence(model);

        assertThat(result).isEmpty();
        verifyNoInteractions(shapFeatureImportanceRepository);
    }

    @Test
    void returnsEmptyWhenModelIsNotLinked() {
        List<ObjectionModelEvidenceResponse> result =
                objectionModelEvidenceService.findLatestTopEvidence(null);

        assertThat(result).isEmpty();
        verifyNoInteractions(
                auditRepository,
                shapFeatureImportanceRepository
        );
    }

    private ShapFeatureImportanceEntity feature(
            int rank,
            String feature,
            String meanAbsShap,
            String contributionRatio,
            String direction
    ) {
        ShapFeatureImportanceEntity entity =
                mock(ShapFeatureImportanceEntity.class);

        when(entity.getRank()).thenReturn(rank);
        when(entity.getFeature()).thenReturn(feature);
        when(entity.getMeanAbsShap())
                .thenReturn(new BigDecimal(meanAbsShap));
        when(entity.getContributionRatio())
                .thenReturn(new BigDecimal(contributionRatio));
        when(entity.getDirection()).thenReturn(direction);

        return entity;
    }
}