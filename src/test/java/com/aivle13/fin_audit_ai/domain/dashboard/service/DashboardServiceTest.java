package com.aivle13.fin_audit_ai.domain.dashboard.service;

import com.aivle13.fin_audit_ai.domain.dashboard.repository.DashboardQueryRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// TODO(#252): board/dashboard/diagnosis 테스트 스켈레톤. @Disabled를 지우고 본문을 채운다.
// DashboardService는 하나의 응답으로 여러 집계를 조립하므로, 아래처럼 조립 단위별로 케이스를 나눠서 검증한다.
@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private DashboardQueryRepository dashboardQueryRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    @Disabled("TODO: 구현 예정")
    void 상단_요약의_규정준수율을_정상_모델_비율로_계산한다() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    void 분석된_모델이_없으면_규정준수율은_0이다() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    void 최신_감사의_종합판정별_모델_수를_분포로_반환한다() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    void 공정성과_SHAP_문제_개수를_합산해_검토_필요_모델_상위_5개를_선정한다() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    void 같은_모델의_공정성과_SHAP_문제는_더_심각한_상태로_합쳐진다() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    void 공정성_지표별_상태_분포를_모든_지표_코드에_대해_반환한다() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    void 최근_완료_감사_5건을_핵심_위험_신호와_함께_반환한다() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    void 이상_신호가_없는_감사는_이상_신호_없음으로_표시된다() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    void 분석_결과가_없는_감사는_분석_결과_없음으로_표시된다() {
    }
}
