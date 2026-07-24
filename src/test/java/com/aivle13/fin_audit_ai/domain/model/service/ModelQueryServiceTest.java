package com.aivle13.fin_audit_ai.domain.model.service;

import com.aivle13.fin_audit_ai.domain.model.dto.response.ModelSummaryResponse;
import com.aivle13.fin_audit_ai.domain.model.entity.AiModelEntity;
import com.aivle13.fin_audit_ai.domain.model.repository.AiModelRepository;
import com.aivle13.fin_audit_ai.domain.model.type.ModelDomain;
import com.aivle13.fin_audit_ai.domain.model.type.ModelType;
import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ModelQueryServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private AiModelRepository aiModelRepository;
    @Mock
    private UserEntity user;

    @InjectMocks
    private ModelQueryService modelQueryService;

    private AiModelEntity model(String modelGroupId, String version) {
        return AiModelEntity.create(user, "credit-model", ModelType.XGBOOST, ModelDomain.CREDIT_SCORING,
                "models/model.json", version, modelGroupId);
    }

    @Test
    void 같은_모델_계열의_여러_버전_중_최신_버전만_반환한다() {
        AiModelEntity latest = model("group-a", "2.0.0");
        AiModelEntity older = model("group-a", "1.0.0");
        AiModelEntity otherGroup = model("group-b", "5.0.0");
        given(aiModelRepository.findByUser_IdOrderByCreatedAtDesc(USER_ID))
                .willReturn(List.of(latest, older, otherGroup));

        List<ModelSummaryResponse> result = modelQueryService.list(USER_ID);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(ModelSummaryResponse::currentVersion)
                .containsExactly("2.0.0", "5.0.0");
    }
}
