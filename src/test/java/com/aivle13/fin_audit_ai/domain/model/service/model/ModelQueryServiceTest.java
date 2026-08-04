package com.aivle13.fin_audit_ai.domain.model.service.model;

import com.aivle13.fin_audit_ai.domain.model.dto.response.model.ModelSummaryResponse;
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
import org.springframework.test.util.ReflectionTestUtils;

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
        given(aiModelRepository.findByUser_IdOrderByCreatedAtDescIdDesc(USER_ID))
                .willReturn(List.of(latest, older, otherGroup));

        List<ModelSummaryResponse> result = modelQueryService.list(USER_ID);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(ModelSummaryResponse::currentVersion)
                .containsExactly("2.0.0", "5.0.0");
    }

    @Test
    void 보관된_모델은_목록에서_제외한다() {
        AiModelEntity active = model("group-a", "1.0.0");
        AiModelEntity archived = model("group-b", "1.0.0");
        archived.archive();
        given(aiModelRepository.findByUser_IdOrderByCreatedAtDescIdDesc(USER_ID))
                .willReturn(List.of(active, archived));

        List<ModelSummaryResponse> result = modelQueryService.list(USER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).currentVersion()).isEqualTo("1.0.0");
    }

    @Test
    void 생성_시각이_동률이면_id가_더_큰_모델을_최신_버전으로_반환한다() {
        AiModelEntity newer = model("group-a", "2.0.0");
        AiModelEntity older = model("group-a", "1.0.0");
        ReflectionTestUtils.setField(newer, "id", 2L);
        ReflectionTestUtils.setField(older, "id", 1L);
        // 리포지토리가 createdAt desc, id desc로 정렬해 돌려준다고 가정하므로
        // 여기서도 id가 큰(더 최신) 모델을 먼저 반환하도록 스텁한다.
        given(aiModelRepository.findByUser_IdOrderByCreatedAtDescIdDesc(USER_ID))
                .willReturn(List.of(newer, older));

        List<ModelSummaryResponse> result = modelQueryService.list(USER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).currentVersion()).isEqualTo("2.0.0");
    }
}
