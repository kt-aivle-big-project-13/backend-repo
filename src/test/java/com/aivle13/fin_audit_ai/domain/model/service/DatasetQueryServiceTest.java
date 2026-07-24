package com.aivle13.fin_audit_ai.domain.model.service;

import com.aivle13.fin_audit_ai.domain.model.dto.response.DatasetSummaryResponse;
import com.aivle13.fin_audit_ai.domain.model.entity.AiModelEntity;
import com.aivle13.fin_audit_ai.domain.model.entity.DatasetEntity;
import com.aivle13.fin_audit_ai.domain.model.repository.AiModelRepository;
import com.aivle13.fin_audit_ai.domain.model.repository.DatasetRepository;
import com.aivle13.fin_audit_ai.domain.model.type.DataSource;
import com.aivle13.fin_audit_ai.domain.model.type.DatasetPurpose;
import com.aivle13.fin_audit_ai.domain.model.type.ModelDomain;
import com.aivle13.fin_audit_ai.domain.model.type.ModelType;
import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import com.aivle13.fin_audit_ai.global.exception.model.ModelNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class DatasetQueryServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long MODEL_ID = 100L;

    @Mock
    private AiModelRepository aiModelRepository;
    @Mock
    private DatasetRepository datasetRepository;
    @Mock
    private UserEntity user;

    @InjectMocks
    private DatasetQueryService datasetQueryService;

    private AiModelEntity model() {
        return AiModelEntity.create(user, "credit-model", ModelType.XGBOOST, ModelDomain.CREDIT_SCORING,
                "models/model.json", "1.0.0");
    }

    private DatasetEntity dataset(AiModelEntity model) {
        return DatasetEntity.create(model, DataSource.CUSTOMER, "datasets/audit.csv", 100, "age,gender,income");
    }

    @Test
    void purpose를_지정하지_않으면_계열_내_전체_데이터셋을_반환한다() {
        AiModelEntity model = model();
        DatasetEntity dataset = dataset(model);
        given(aiModelRepository.findByIdAndUser_Id(MODEL_ID, USER_ID)).willReturn(Optional.of(model));
        given(datasetRepository.findByModel_User_IdAndModel_ModelGroupIdOrderByCreatedAtDesc(
                USER_ID, model.getModelGroupId())).willReturn(List.of(dataset));

        List<DatasetSummaryResponse> result = datasetQueryService.list(USER_ID, MODEL_ID, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).datasetId()).isEqualTo(dataset.getId());
    }

    @Test
    void purpose를_지정하면_해당_purpose로만_필터링해서_조회한다() {
        AiModelEntity model = model();
        DatasetEntity validationDataset = dataset(model);
        validationDataset.markAsValidation();
        given(aiModelRepository.findByIdAndUser_Id(MODEL_ID, USER_ID)).willReturn(Optional.of(model));
        given(datasetRepository.findByModel_User_IdAndModel_ModelGroupIdAndPurposeOrderByCreatedAtDesc(
                USER_ID, model.getModelGroupId(), DatasetPurpose.VALIDATION)).willReturn(List.of(validationDataset));

        List<DatasetSummaryResponse> result = datasetQueryService.list(USER_ID, MODEL_ID, DatasetPurpose.VALIDATION);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).purpose()).isEqualTo("VALIDATION");
    }

    @Test
    void 모델이_없거나_소유자가_아니면_예외가_발생한다() {
        given(aiModelRepository.findByIdAndUser_Id(MODEL_ID, USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> datasetQueryService.list(USER_ID, MODEL_ID, null))
                .isInstanceOf(ModelNotFoundException.class);
    }
}
