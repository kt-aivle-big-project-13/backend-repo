package com.aivle13.fin_audit_ai.domain.model.entity;

import com.aivle13.fin_audit_ai.domain.model.type.DataSource;
import com.aivle13.fin_audit_ai.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 모델에 업로드된 검증 데이터셋(X, y).
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

    // X 데이터 S3 키 (dataSource=DUMMY면 없음)
    @Column(name = "feature_file_key", length = 255)
    private String featureFileKey;

    // y 데이터 S3 키 (dataSource=DUMMY면 없음)
    @Column(name = "label_file_key", length = 255)
    private String labelFileKey;

    @Column(name = "row_count", nullable = false)
    private int rowCount;

    // 콤마로 구분된 컬럼명 목록
    @Column(nullable = false, length = 1000)
    private String columns;

    public static DatasetEntity create(AiModelEntity model, DataSource dataSource, String featureFileKey,
                                        String labelFileKey, int rowCount, String columns) {
        DatasetEntity dataset = new DatasetEntity();
        dataset.model = model;
        dataset.dataSource = dataSource;
        dataset.featureFileKey = featureFileKey;
        dataset.labelFileKey = labelFileKey;
        dataset.rowCount = rowCount;
        dataset.columns = columns;
        return dataset;
    }
}
