package com.aivle13.fin_audit_ai.domain.demo;

import com.aivle13.fin_audit_ai.domain.demo.config.DemoProperties;
import com.aivle13.fin_audit_ai.domain.demo.config.DemoPropertiesValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 시연 설정 검증.
 *
 * <p>S3 키가 빠진 채로 시연 모드가 켜지면 게스트에게 파일 없는 모델이 생긴다. 화면은
 * 정상으로 보이다가 감사를 시작하는 순간 실패하므로 기동에서 막아야 한다.
 */
class DemoPropertiesValidatorTest {

    private static final String MODEL_KEY = "demo/credit_model.json";
    private static final String DATASET_KEY = "demo/audit_dataset.csv";
    private static final String SENSITIVE = "CODE_GENDER,AGE_GROUP";

    @Test
    @DisplayName("시연 모드가 꺼져 있으면 설정이 비어 있어도 통과한다")
    void allowsEmptyKeysWhenDisabled() {
        DemoPropertiesValidator validator =
                validatorOf(properties(false, "", "", ""));

        assertThatCode(validator::validate).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("시연 모드를 켜면 모델 S3 키가 필요하다")
    void requiresModelKeyWhenEnabled() {
        DemoPropertiesValidator validator =
                validatorOf(properties(true, "", DATASET_KEY, SENSITIVE));

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DEMO_MODEL_S3_KEY");
    }

    @Test
    @DisplayName("시연 모드를 켜면 데이터셋 S3 키가 필요하다")
    void requiresDatasetKeyWhenEnabled() {
        DemoPropertiesValidator validator =
                validatorOf(properties(true, MODEL_KEY, "", SENSITIVE));

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DEMO_DATASET_S3_KEY");
    }

    @Test
    @DisplayName("시연 모드를 켜면 민감정보 컬럼이 필요하다")
    void requiresSensitiveAttributesWhenEnabled() {
        DemoPropertiesValidator validator =
                validatorOf(properties(true, MODEL_KEY, DATASET_KEY, " "));

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DEMO_SENSITIVE_ATTRIBUTES");
    }

    @Test
    @DisplayName("필요한 설정이 모두 있으면 통과한다")
    void allowsFullyConfiguredDemo() {
        DemoPropertiesValidator validator =
                validatorOf(properties(true, MODEL_KEY, DATASET_KEY, SENSITIVE));

        assertThatCode(validator::validate).doesNotThrowAnyException();
    }

    private DemoPropertiesValidator validatorOf(DemoProperties properties) {
        return new DemoPropertiesValidator(properties);
    }

    private DemoProperties properties(
            boolean enabled,
            String modelS3Key,
            String datasetS3Key,
            String sensitiveAttributes
    ) {
        return new DemoProperties(
                enabled,
                modelS3Key,
                datasetS3Key,
                sensitiveAttributes,
                10000,
                "SK_ID_CURR,TARGET",
                Duration.ofHours(24),
                Map.of()
        );
    }
}
