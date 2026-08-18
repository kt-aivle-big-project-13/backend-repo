package com.aivle13.fin_audit_ai.domain.demo.service;

import com.aivle13.fin_audit_ai.domain.audit.entity.AuditEntity;
import com.aivle13.fin_audit_ai.domain.audit.repository.AuditRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.FairnessGroupStatRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.FairnessResultRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.ShapFeatureImportanceRepository;
import com.aivle13.fin_audit_ai.domain.audit.repository.XaiResultRepository;
import com.aivle13.fin_audit_ai.domain.audit.type.core.AuditStatus;
import com.aivle13.fin_audit_ai.domain.audit.type.core.ThresholdMethod;
import com.aivle13.fin_audit_ai.domain.demo.config.DemoProperties;
import com.aivle13.fin_audit_ai.domain.model.entity.AiModelEntity;
import com.aivle13.fin_audit_ai.domain.model.entity.DatasetEntity;
import com.aivle13.fin_audit_ai.domain.model.repository.AiModelRepository;
import com.aivle13.fin_audit_ai.domain.model.repository.DatasetRepository;
import com.aivle13.fin_audit_ai.domain.model.type.DataSource;
import com.aivle13.fin_audit_ai.domain.model.type.ModelDomain;
import com.aivle13.fin_audit_ai.domain.model.type.ModelType;
import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 게스트에게 바로 둘러볼 거리를 만들어 준다.
 *
 * <p>모델·데이터셋 행은 게스트마다 새로 만든다. 감사 진행 중 판정이 모델 단위라
 * ({@code AuditStartService}), 하나를 공유하면 한 명이 감사를 시작한 동안 나머지가
 * 시작하지 못한다.
 *
 * <p>반면 S3 객체는 공용 데모 파일 하나를 함께 가리킨다. 게스트마다 파일을 복사하면
 * 저장 비용만 늘고 얻는 것이 없다.
 *
 * <p>완료된 감사도 함께 넣는다. 실제 분석을 돌리지 않고도 결과·리포트 화면을 볼 수 있어야
 * AI 서버 동시 실행 한도에 걸리지 않는다.
 */
@Component
@RequiredArgsConstructor
public class DemoDataProvisioner {

    private static final String DEMO_DATASET_FILE_NAME = "audit_dataset.csv";

    private final DemoProperties demoProperties;
    private final AiModelRepository aiModelRepository;
    private final DatasetRepository datasetRepository;
    private final AuditRepository auditRepository;
    private final FairnessResultRepository fairnessResultRepository;
    private final FairnessGroupStatRepository fairnessGroupStatRepository;
    private final XaiResultRepository xaiResultRepository;
    private final ShapFeatureImportanceRepository shapFeatureImportanceRepository;

    public void provision(UserEntity guest) {
        AiModelEntity model = createModel(guest);
        DatasetEntity dataset = createDataset(model);

        createCompletedAudit(guest, model, dataset);
    }

    private AiModelEntity createModel(UserEntity guest) {
        AiModelEntity model = AiModelEntity.create(
                guest,
                DemoAuditFixture.MODEL_NAME,
                ModelType.XGBOOST,
                ModelDomain.CREDIT_SCORING,
                demoProperties.modelS3Key(),
                DemoAuditFixture.MODEL_VERSION
        );

        return aiModelRepository.save(model);
    }

    private DatasetEntity createDataset(AiModelEntity model) {
        DatasetEntity dataset = DatasetEntity.create(
                model,
                DataSource.CUSTOMER,
                demoProperties.datasetS3Key(),
                DEMO_DATASET_FILE_NAME,
                demoProperties.datasetRowCount(),
                demoProperties.datasetColumns()
        );

        // 민감정보가 선택돼 있지 않으면 감사 시작이 400 으로 막힌다. 게스트가 업로드부터
        // 다시 하지 않고 곧바로 감사를 시작할 수 있게 미리 채워 둔다.
        dataset.updateSensitiveAttributes(demoProperties.sensitiveAttributes());

        return datasetRepository.save(dataset);
    }

    /**
     * 이미 끝난 감사 한 건.
     *
     * <p>데이터셋을 {@code audited} 로 표시하지 않는다. 표시하면 민감정보 수정이 막혀
     * 시연에서 그 화면을 보여줄 수 없고, 이 감사는 실제로 분석을 돌린 것이 아니라
     * 데이터셋을 묶어 둘 이유도 없다.
     */
    private void createCompletedAudit(
            UserEntity guest,
            AiModelEntity model,
            DatasetEntity dataset
    ) {
        AuditEntity audit = AuditEntity.create(
                model,
                dataset,
                guest,
                DemoAuditFixture.AUDIT_NAME,
                demoProperties.sensitiveAttributes(),
                null,
                ThresholdMethod.MANUAL,
                null,
                new BigDecimal("0.3200"),
                null
        );

        audit.applyPerformance(new BigDecimal("0.7460"), new BigDecimal("0.8830"));

        // 성별 지표 두 개가 REVIEW 라 전체 판정도 주의로 둔다. 전부 통과인 결과보다
        // 무엇을 보는 서비스인지 드러나서 시연에 낫다.
        audit.complete(4, AuditStatus.WARNING);

        AuditEntity saved = auditRepository.save(audit);

        fairnessResultRepository.saveAll(DemoAuditFixture.fairnessResults(saved));
        fairnessGroupStatRepository.saveAll(DemoAuditFixture.groupStats(saved));
        xaiResultRepository.saveAll(DemoAuditFixture.xaiResults(saved));
        shapFeatureImportanceRepository.saveAll(DemoAuditFixture.shapFeatures(saved));
    }
}
