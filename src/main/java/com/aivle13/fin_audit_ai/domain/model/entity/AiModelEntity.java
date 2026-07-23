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

    // 사전진단 후 확정되므로 nullable
    @Column(name = "is_high_impact")
    private Boolean highImpact;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ModelStatus status;

    public static AiModelEntity create(UserEntity user, String modelName, ModelType modelType, ModelDomain domain, String artifactPath, String version) {
        AiModelEntity model = new AiModelEntity();
        model.user = user;
        model.modelName = modelName;
        model.modelType = modelType;
        model.domain = domain;
        model.artifactPath = artifactPath;
        model.version = version;
        model.status = ModelStatus.ACTIVE;
        model.modelGroupId = UUID.randomUUID().toString();
        return model;
    }
}
