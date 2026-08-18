package com.aivle13.fin_audit_ai.domain.demo.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 시연 모드를 켰는데 필요한 설정이 빠진 채로 뜨는 것을 막는다.
 *
 * <p>모델·데이터셋 S3 키가 없으면 게스트에게 파일이 없는 모델이 생긴다. 화면은 정상으로
 * 보이지만 감사를 시작하는 순간에야 실패해, 시연 도중에 드러난다. 기동에서 막는 편이 낫다.
 *
 * <p>꺼져 있으면 검사하지 않는다. 시연 설정이 없는 환경이 대부분이고 그때는 이 값들이
 * 쓰이지 않는다.
 */
@Component
@RequiredArgsConstructor
public class DemoPropertiesValidator {

    private final DemoProperties demoProperties;

    @PostConstruct
    public void validate() {
        if (!demoProperties.enabled()) {
            return;
        }

        requireText(demoProperties.modelS3Key(), "app.demo.model-s3-key (DEMO_MODEL_S3_KEY)");
        requireText(demoProperties.datasetS3Key(), "app.demo.dataset-s3-key (DEMO_DATASET_S3_KEY)");
        requireText(
                demoProperties.sensitiveAttributes(),
                "app.demo.sensitive-attributes (DEMO_SENSITIVE_ATTRIBUTES)"
        );
    }

    private void requireText(String value, String propertyName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(
                    "시연 모드(app.demo.enabled=true)를 켜려면 %s 를 지정해야 합니다."
                            .formatted(propertyName)
            );
        }
    }
}
