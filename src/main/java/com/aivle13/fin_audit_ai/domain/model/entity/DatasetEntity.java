package com.aivle13.fin_audit_ai.domain.model.entity;

import com.aivle13.fin_audit_ai.domain.model.type.DataSource;
import com.aivle13.fin_audit_ai.domain.model.type.DatasetPurpose;
import com.aivle13.fin_audit_ai.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 모델에 업로드된 감사 데이터셋. Fairlearn 파이프라인의 audit_dataset_file과
 * 동일하게, 실제 라벨·민감변수·모델 입력 피처가 한 파일에 합쳐진 형태로 받는다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "datasets")
public class DatasetEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dataset_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "model_id")
    private AiModelEntity model;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_source", nullable = false, length = 20)
    private DataSource dataSource;

    // 감사 데이터셋 S3 키 (dataSource=DUMMY면 없음)
    @Column(name = "dataset_file_key", length = 255)
    private String datasetFileKey;

    @Column(name = "row_count", nullable = false)
    private int rowCount;

    // 콤마로 구분된 컬럼명 목록
    @Column(nullable = false, length = 1000)
    private String columns;

    // 공정성 분석 대상 민감정보 컬럼명 (콤마 구분). 데이터셋 버전마다 컬럼 구성이 달라질 수 있어
    // 모델이 아닌 이 데이터셋 자체에 귀속시킨다. 선택 전엔 null
    @Column(name = "sensitive_attributes", length = 1000)
    private String sensitiveAttributes;

    // 이 데이터셋으로 감사가 시작된 적이 있는지. 감사는 시작 시점의 민감정보를 스냅샷으로
    // 복사해 불변으로 남기지만(AuditEntity.sensitiveFeatures), 스냅샷과 원본이 어긋나면
    // 조회 화면에서 혼란을 줄 수 있어 감사에 쓰인 데이터셋은 민감정보 수정 자체를 막는다
    @Column(name = "audited", nullable = false)
    private boolean audited = false;

    // 이 데이터셋이 감사 측정용인지, 임계값 보정용(valid_processed.csv)인지 구분.
    // 업로드 API는 아직 AUDIT만 만들 수 있고, VALIDATION 업로드 경로는 후속 작업.
    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 20)
    private DatasetPurpose purpose = DatasetPurpose.AUDIT;

    public static DatasetEntity create(AiModelEntity model, DataSource dataSource, String datasetFileKey,
                                        int rowCount, String columns) {
        DatasetEntity dataset = new DatasetEntity();
        dataset.model = model;
        dataset.dataSource = dataSource;
        dataset.datasetFileKey = datasetFileKey;
        dataset.rowCount = rowCount;
        dataset.columns = columns;
        return dataset;
    }

    public void updateSensitiveAttributes(String sensitiveAttributes) {
        this.sensitiveAttributes = sensitiveAttributes;
    }

    public void markAudited() {
        this.audited = true;
    }
}
