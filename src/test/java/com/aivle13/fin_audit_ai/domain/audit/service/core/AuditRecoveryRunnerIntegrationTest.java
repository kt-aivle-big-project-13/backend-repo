package com.aivle13.fin_audit_ai.domain.audit.service.core;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.audit.type.AuditStatus;
import com.aivle13.fin_audit_ai.domain.audit.type.ThresholdMethod;
import com.aivle13.fin_audit_ai.domain.model.entity.AiModelEntity;
import com.aivle13.fin_audit_ai.domain.model.entity.DatasetEntity;
import com.aivle13.fin_audit_ai.domain.model.repository.AiModelRepository;
import com.aivle13.fin_audit_ai.domain.model.repository.DatasetRepository;
import com.aivle13.fin_audit_ai.domain.model.type.DataSource;
import com.aivle13.fin_audit_ai.domain.model.type.ModelDomain;
import com.aivle13.fin_audit_ai.domain.model.type.ModelType;
import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import com.aivle13.fin_audit_ai.domain.user.repository.UserRepository;
import com.aivle13.fin_audit_ai.domain.user.type.UserRole;
import com.aivle13.fin_audit_ai.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class AuditRecoveryRunnerIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AiModelRepository aiModelRepository;

    @Autowired
    private DatasetRepository datasetRepository;

    @Autowired
    private AuditRepository auditRepository;

    @Autowired
    private AuditRecoveryRunner auditRecoveryRunner;

    private AiModelEntity model;
    private DatasetEntity dataset;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        user = userRepository.save(
                UserEntity.create("테스트기관", "홍길동", "audit-recovery-test@example.com", "hash", UserRole.AUDITOR)
        );
        model = aiModelRepository.save(
                AiModelEntity.create(user, "credit-model", ModelType.XGBOOST, ModelDomain.CREDIT_SCORING, "models/model-key.pkl", "1.0.0")
        );
        dataset = datasetRepository.save(
                DatasetEntity.create(model, DataSource.CUSTOMER, "datasets/test-key.csv", 100, "age,gender,income")
        );
    }

    private AuditEntity newAudit(String name) {
        return AuditEntity.create(model, dataset, user, name, "age,gender",
                null, ThresholdMethod.MANUAL, null, BigDecimal.valueOf(0.5), null);
    }

    @Test
    @DisplayName("기동 시 IN_PROGRESS로 고아가 된 감사를 FAILED로 전환한다")
    void marksOrphanedInProgressAuditAsFailed() {
        AuditEntity orphaned = newAudit("고아 감사");
        orphaned.markInProgress();
        auditRepository.save(orphaned);

        AuditEntity pending = newAudit("대기 중인 감사");
        auditRepository.save(pending);

        auditRecoveryRunner.run(new DefaultApplicationArguments());

        assertThat(auditRepository.findById(orphaned.getId()).orElseThrow().getStatus())
                .isEqualTo(AuditStatus.FAILED);
        assertThat(auditRepository.findById(pending.getId()).orElseThrow().getStatus())
                .isEqualTo(AuditStatus.PENDING);
    }

    @Test
    @DisplayName("고아 상태인 감사가 없으면 아무것도 바꾸지 않는다")
    void doesNothingWhenNoOrphanedAuditsExist() {
        AuditEntity pending = newAudit("대기 중인 감사");
        auditRepository.save(pending);

        auditRecoveryRunner.run(new DefaultApplicationArguments());

        assertThat(auditRepository.findById(pending.getId()).orElseThrow().getStatus())
                .isEqualTo(AuditStatus.PENDING);
    }
}
