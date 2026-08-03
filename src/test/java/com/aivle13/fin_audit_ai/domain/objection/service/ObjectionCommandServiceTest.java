package com.aivle13.fin_audit_ai.domain.objection.service;

import com.aivle13.fin_audit_ai.domain.model.entity.AiModelEntity;
import com.aivle13.fin_audit_ai.domain.model.repository.AiModelRepository;
import com.aivle13.fin_audit_ai.domain.objection.entity.ObjectionEntity;
import com.aivle13.fin_audit_ai.domain.objection.repository.ObjectionRepository;
import com.aivle13.fin_audit_ai.domain.user.repository.UserRepository;
import com.aivle13.fin_audit_ai.global.exception.model.ModelNotFoundException;
import com.aivle13.fin_audit_ai.global.mail.MailService;
import com.aivle13.fin_audit_ai.global.s3.validator.AuditFileValidator;
import com.aivle13.fin_audit_ai.domain.objection.dto.request.ObjectionDispatchRequest;
import com.aivle13.fin_audit_ai.domain.objection.type.ObjectionDecision;
import com.aivle13.fin_audit_ai.global.exception.objection.ObjectionNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ObjectionCommandServiceTest {

    @Mock
    private ObjectionRepository objectionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditFileValidator fileValidator;

    @Mock
    private MailService mailService;

    @Mock
    private AiModelRepository aiModelRepository;

    @InjectMocks
    private ObjectionCommandService objectionCommandService;

    @Test
    void importsCsvAndLinksOwnedModel() {
        Long userId = 1L;
        Long modelId = 10L;
        AiModelEntity model = mock(AiModelEntity.class);
        MockMultipartFile file = csvFile();

        when(aiModelRepository.findByIdAndUser_Id(modelId, userId))
                .thenReturn(Optional.of(model));
        when(objectionRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        objectionCommandService.importFromCsv(
                userId,
                modelId,
                file
        );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ObjectionEntity>> captor =
                ArgumentCaptor.forClass(List.class);

        verify(objectionRepository).saveAll(captor.capture());

        List<ObjectionEntity> saved = captor.getValue();

        assertThat(saved).hasSize(1);
        assertThat(saved.getFirst().getModel()).isSameAs(model);
        verify(fileValidator).validateCsvFile(
                file,
                "이의제기 신용감사 결과"
        );
    }

    @Test
    void rejectsModelNotOwnedByUser() {
        Long userId = 1L;
        Long otherUsersModelId = 20L;
        MockMultipartFile file = csvFile();

        when(aiModelRepository.findByIdAndUser_Id(
                otherUsersModelId,
                userId
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                objectionCommandService.importFromCsv(
                        userId,
                        otherUsersModelId,
                        file
                )
        ).isInstanceOf(ModelNotFoundException.class);

        verifyNoInteractions(fileValidator);
        verify(objectionRepository, never()).saveAll(anyList());
    }

    @Test
    void otherUserCannotDispatchObjectionResponse() {
        Long otherUserId = 99L;
        Long objectionId = 1L;

        ObjectionDispatchRequest request =
                new ObjectionDispatchRequest(
                        ObjectionDecision.REJECT_MAINTAIN,
                        "신용평가 결과 이의제기 회신 안내",
                        "고객 안내문 내용",
                        "customer@example.com"
                );

        when(objectionRepository
                .findByIdAndModel_User_IdForUpdate(
                        objectionId,
                        otherUserId
                ))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                objectionCommandService.dispatch(
                        otherUserId,
                        objectionId,
                        request
                )
        ).isInstanceOf(ObjectionNotFoundException.class);

        verifyNoInteractions(
                userRepository,
                mailService
        );
    }

    private MockMultipartFile csvFile() {
        String csv = String.join(
                "\n",
                "고객_이름,이의제기_번호,거절_금융기준,제목,내용,주요_판단_근거_변수,담당자_판단_근거,작성일시",
                "김다진,#2026-0001,부채상환능력 기준 초과,왜 거절됐나요?,거절 이유를 확인하고 싶습니다.,"
                        + "부채비율 82%; 최근 연체 이력 2건/6개월,부채비율과 연체 이력을 종합 검토했습니다.,2026-07-31 9:54"
        );

        return new MockMultipartFile(
                "file",
                "objections.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)
        );
    }
}