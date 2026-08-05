package com.aivle13.fin_audit_ai.domain.diagnosis.service;

import com.aivle13.fin_audit_ai.domain.diagnosis.repository.DiagnosisAnswerRepository;
import com.aivle13.fin_audit_ai.domain.diagnosis.repository.PreDiagnosisRepository;
import com.aivle13.fin_audit_ai.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// TODO(#252): board/dashboard/diagnosis 테스트 스켈레톤. @Disabled를 지우고 본문을 채운다.
@ExtendWith(MockitoExtension.class)
class DiagnosisServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PreDiagnosisRepository preDiagnosisRepository;

    @Mock
    private DiagnosisAnswerRepository diagnosisAnswerRepository;

    @InjectMocks
    private DiagnosisService diagnosisService;

    @Test
    @Disabled("TODO: 구현 예정")
    void 사전진단을_시작하면_IN_PROGRESS_상태로_생성된다() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    void 존재하지_않는_사용자면_시작시_예외() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    void 모델을_사전진단에_연결한다() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    void 다른_사용자의_사전진단에_모델_연결시_예외() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    void GATE_문항_중_하나라도_해당되면_HIGH_IMPACT다() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    void GATE_문항이_모두_해당없음이면_정량진단_필요_상태다() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    void GATE_문항이_아닌_코드가_섞이면_예외() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    void GATE_문항이_중복되면_예외() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    void IN_PROGRESS가_아니면_정성진단_제출시_예외() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    void 가중점수_합이_임계값_이상이면_HIGH_IMPACT로_확정된다() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    void 가중점수_합이_임계값_미만이면_NOT_APPLICABLE로_확정된다() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    void NEEDS_QUANTITATIVE가_아니면_정량진단_제출시_예외() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    void 사전진단_결과를_조회한다() {
    }

    @Test
    @Disabled("TODO: 구현 예정")
    void 존재하지_않는_사전진단_조회시_예외() {
    }
}
