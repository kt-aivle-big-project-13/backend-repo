package com.aivle13.fin_audit_ai.domain.model.entity;

import com.aivle13.fin_audit_ai.domain.model.type.ModelDomain;
import com.aivle13.fin_audit_ai.domain.model.type.ModelStatus;
import com.aivle13.fin_audit_ai.domain.model.type.ModelType;
import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import com.aivle13.fin_audit_ai.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 감사 대상으로 등록된 AI 모델.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "ai_models")
public class AiModelEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "model_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    // 같은 모델의 다른 버전들을 묶는 식별자. 버전 간 modelName이 바뀌어도 계열을 유지하기 위해
    // 문자열 매칭 대신 별도 값으로 관리한다. 새 모델이면 새로 발급, 기존 모델의 새 버전이면 이어받는다.
    @Column(name = "model_group_id", nullable = false, length = 36)
    private String modelGroupId;

    @Enumerated(EnumType.STRING)
    @Column(name = "model_type", nullable = false, length = 20)
    private ModelType modelType;

    @Column(nullable = false, length = 20)
    private String version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ModelDomain domain;

    // STORAGE_ROOT 기준 상대경로
    @Column(name = "artifact_path", nullable = false, length = 255)
    private String artifactPath;

    // 사용자가 업로드한 원본 모델 파일명 (artifactPath는 S3 키라 화면 표시용으로 별도 보관)
    @Column(name = "original_file_name", length = 255)
    private String originalFileName;

    // 사전진단 후 확정되므로 nullable
    @Column(name = "is_high_impact")
    private Boolean highImpact;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ModelStatus status;

    public static AiModelEntity create(UserEntity user, String modelName, ModelType modelType, ModelDomain domain, String artifactPath, String version) {
        return create(user, modelName, modelType, domain, artifactPath, null, version, UUID.randomUUID().toString());
    }

    // 기존 모델의 새 버전으로 등록할 때, 그 모델의 modelGroupId를 그대로 이어받기 위한 생성자
    public static AiModelEntity create(UserEntity user, String modelName, ModelType modelType, ModelDomain domain,
                                        String artifactPath, String version, String modelGroupId) {
        return create(user, modelName, modelType, domain, artifactPath, null, version, modelGroupId);
    }

    public static AiModelEntity create(UserEntity user, String modelName, ModelType modelType, ModelDomain domain,
                                        String artifactPath, String originalFileName, String version, String modelGroupId) {
        AiModelEntity model = new AiModelEntity();
        model.user = user;
        model.modelName = modelName;
        model.modelType = modelType;
        model.domain = domain;
        model.artifactPath = artifactPath;
        model.originalFileName = originalFileName;
        model.version = version;
        model.status = ModelStatus.ACTIVE;
        model.modelGroupId = modelGroupId;
        return model;
    }

    // 이 모델로 시작한 감사가 전부 취소돼 실사용 이력이 없을 때 호출한다. 모델 row 자체는
    // 지우지 않는다 — 취소된 감사가 이 모델을 참조하므로 지우면 감사 이력이 깨진다.
    // 대신 ARCHIVED로 돌려 모델명 중복 검증·목록 조회에서 빠지게 해서, 같은 이름으로
    // 다시 등록할 수 있게 한다.
    public void archive() {
        this.status = ModelStatus.ARCHIVED;
    }

    // 취소됐던 감사를 재시도할 때, 그 사이 archive()로 보관 처리됐을 수 있는 모델을 다시 활성화한다.
    public void activate() {
        this.status = ModelStatus.ACTIVE;
    }
}
