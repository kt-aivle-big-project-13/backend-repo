package com.aivle13.fin_audit_ai.domain.model.entity;

import com.aivle13.fin_audit_ai.domain.model.type.ModelStatus;
import com.aivle13.fin_audit_ai.domain.model.type.ModelType;
import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import com.aivle13.fin_audit_ai.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "model_type", nullable = false, length = 20)
    private ModelType modelType;

    @Column(nullable = false)
    private Integer version = 1;

    // STORAGE_ROOT 기준 상대경로
    @Column(name = "artifact_path", nullable = false, length = 255)
    private String artifactPath;

    // 사전진단 후 확정되므로 nullable
    @Column(name = "is_high_impact")
    private Boolean highImpact;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ModelStatus status;
}
