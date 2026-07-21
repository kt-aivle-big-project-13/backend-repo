package com.aivle13.fin_audit_ai.domain.model.entity;

import com.aivle13.fin_audit_ai.domain.model.type.DataSource;
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
}
