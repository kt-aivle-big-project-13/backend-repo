package com.aivle13.fin_audit_ai.domain.demo.config;

import com.aivle13.fin_audit_ai.domain.report.type.ReportFormat;
import com.aivle13.fin_audit_ai.domain.report.type.ReportType;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Map;

/**
 * 시연용 게스트 계정 설정.
 *
 * <p>기본값은 꺼짐이다. 운영에서 켜면 누구나 계정 없이 데이터를 만들 수 있으므로,
 * 시연 환경에서만 명시적으로 켠다.
 *
 * @param modelS3Key          데모 모델 파일. 게스트마다 모델 행은 따로 만들지만 파일은
 *                            이 하나를 함께 가리켜 복사하지 않는다.
 * @param datasetS3Key        데모 감사 데이터셋 파일. 같은 이유로 공유한다.
 * @param sensitiveAttributes 데모 데이터셋에 미리 선택해 둘 민감정보 컬럼. 선택돼 있어야
 *                            게스트가 곧바로 감사를 시작할 수 있다.
 * @param guestTtl            이 시간이 지난 게스트 계정은 정리 대상이다.
 * @param reports             미리 올려 둔 데모 리포트 파일. 종류·포맷별로 지정한 것만
 *                            심는다. 지정하지 않은 것은 기존대로 다운로드 시점에
 *                            생성되므로, 준비한 만큼만 넣어 두면 된다.
 */
@ConfigurationProperties(prefix = "app.demo")
public record DemoProperties(
        boolean enabled,
        String modelS3Key,
        String datasetS3Key,
        String sensitiveAttributes,
        int datasetRowCount,
        String datasetColumns,
        Duration guestTtl,
        Map<ReportType, Map<ReportFormat, String>> reports
) {

    public Map<ReportType, Map<ReportFormat, String>> reports() {
        return reports == null ? Map.of() : reports;
    }
}
