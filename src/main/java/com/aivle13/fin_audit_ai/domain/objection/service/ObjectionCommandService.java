package com.aivle13.fin_audit_ai.domain.objection.service;

import com.aivle13.fin_audit_ai.domain.objection.dto.request.ObjectionDispatchRequest;
import com.aivle13.fin_audit_ai.domain.objection.dto.response.ObjectionDetailResponse;
import com.aivle13.fin_audit_ai.domain.objection.dto.response.ObjectionImportResponse;
import com.aivle13.fin_audit_ai.domain.objection.dto.response.ObjectionSummaryResponse;
import com.aivle13.fin_audit_ai.domain.objection.entity.ObjectionEntity;
import com.aivle13.fin_audit_ai.domain.objection.repository.ObjectionRepository;
import com.aivle13.fin_audit_ai.domain.objection.type.ObjectionStatus;
import com.aivle13.fin_audit_ai.domain.user.entity.UserEntity;
import com.aivle13.fin_audit_ai.domain.user.repository.UserRepository;
import com.aivle13.fin_audit_ai.global.exception.objection.DuplicateObjectionNoException;
import com.aivle13.fin_audit_ai.global.exception.objection.InvalidObjectionFileException;
import com.aivle13.fin_audit_ai.global.exception.objection.ObjectionAlreadyProcessedException;
import com.aivle13.fin_audit_ai.global.exception.objection.ObjectionNotFoundException;
import com.aivle13.fin_audit_ai.global.exception.user.UserNotFoundException;
import com.aivle13.fin_audit_ai.global.mail.MailService;
import com.aivle13.fin_audit_ai.global.s3.validator.AuditFileValidator;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class ObjectionCommandService {

    private static final DateTimeFormatter SUBMITTED_AT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm");

    private static final List<String> REQUIRED_HEADERS = List.of(
            "고객_이름", "이의제기_번호", "거절_금융기준", "제목", "내용", "주요_판단_근거_변수", "담당자_판단_근거", "작성일시"
    );

    private final ObjectionRepository objectionRepository;
    private final UserRepository userRepository;
    private final AuditFileValidator fileValidator;
    private final MailService mailService;

    public ObjectionImportResponse importFromCsv(MultipartFile file) {
        fileValidator.validateCsvFile(file, "이의제기 신용감사 결과");

        List<ObjectionEntity> parsed = parseCsv(file);
        List<ObjectionEntity> saved = objectionRepository.saveAll(parsed);

        List<ObjectionSummaryResponse> summaries = saved.stream()
                .map(ObjectionSummaryResponse::of)
                .toList();

        return new ObjectionImportResponse(summaries.size(), summaries);
    }

    public ObjectionDetailResponse dispatch(Long userId, Long objectionId, ObjectionDispatchRequest request) {
        ObjectionEntity objection = objectionRepository.findById(objectionId)
                .orElseThrow(ObjectionNotFoundException::new);

        if (objection.getStatus() == ObjectionStatus.DELIVERED) {
            throw new ObjectionAlreadyProcessedException();
        }

        UserEntity approver = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        // 메일 발송이 실패하면 이의제기 상태도 바뀌지 않도록, DB 반영보다 먼저 실제로 발송한다.
        mailService.sendObjectionResponseMail(
                request.recipientEmail(),
                objection.getCustomerName(),
                request.letterTitle(),
                request.letterBody()
        );

        LocalDateTime now = LocalDateTime.now();

        objection.recordDraft(request.letterBody());
        objection.approve(approver, request.decision(), now);
        objection.deliver(now, request.recipientEmail());

        return ObjectionDetailResponse.of(objection);
    }

    // 행 단위로 전부 검증한 뒤 한 번에 저장한다. 중간에 하나라도 실패하면 아무것도 저장되지 않는다.
    private List<ObjectionEntity> parseCsv(MultipartFile file) {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .build();

        List<ObjectionEntity> objections = new ArrayList<>();
        Set<String> objectionNosInFile = new HashSet<>();

        try (CSVParser parser = format.parse(new StringReader(readAsUtf8WithoutBom(file)))) {
            if (!parser.getHeaderNames().containsAll(REQUIRED_HEADERS)) {
                throw new InvalidObjectionFileException(
                        "CSV 헤더가 올바르지 않습니다. 필요한 컬럼: " + String.join(", ", REQUIRED_HEADERS)
                                + " / 실제 헤더: " + parser.getHeaderNames());
            }

            long rowNumber = 1; // 1행은 헤더
            for (CSVRecord record : parser) {
                rowNumber++;
                objections.add(toEntity(record, rowNumber, objectionNosInFile));
            }
        } catch (IOException e) {
            throw new InvalidObjectionFileException("CSV 파일을 읽을 수 없습니다: " + file.getOriginalFilename());
        }

        if (objections.isEmpty()) {
            throw new InvalidObjectionFileException("등록할 이의제기 데이터가 없습니다.");
        }

        return objections;
    }

    // 엑셀 등에서 저장한 CSV는 UTF-8 BOM이 파일 앞에 붙어 첫 헤더 컬럼명이 깨지는 경우가 많아 제거한다.
    private String readAsUtf8WithoutBom(MultipartFile file) throws IOException {
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        return content.startsWith("\uFEFF") ? content.substring(1) : content;
    }

    private ObjectionEntity toEntity(CSVRecord record, long rowNumber, Set<String> objectionNosInFile) {
        String objectionNo = requireText(record, "이의제기_번호", rowNumber);
        String customerName = requireText(record, "고객_이름", rowNumber);
        String caseType = requireText(record, "거절_금융기준", rowNumber);
        String title = requireText(record, "제목", rowNumber);
        String content = requireText(record, "내용", rowNumber);
        String shapEvidence = record.get("주요_판단_근거_변수").trim();
        String staffNote = record.get("담당자_판단_근거").trim();
        String submittedAtRaw = requireText(record, "작성일시", rowNumber);

        if (!objectionNosInFile.add(objectionNo)) {
            throw new DuplicateObjectionNoException(rowNumber + "번째 행: 파일 내에 중복된 이의제기 번호입니다 (" + objectionNo + ")");
        }
        if (objectionRepository.existsByObjectionNo(objectionNo)) {
            throw new DuplicateObjectionNoException(rowNumber + "번째 행: 이미 등록된 이의제기 번호입니다 (" + objectionNo + ")");
        }

        LocalDateTime submittedAt;
        try {
            submittedAt = LocalDateTime.parse(submittedAtRaw, SUBMITTED_AT_FORMAT);
        } catch (DateTimeParseException e) {
            throw new InvalidObjectionFileException(
                    rowNumber + "번째 행: 작성일시 형식이 올바르지 않습니다 (예: 2026-07-31 9:54) - " + submittedAtRaw);
        }

        return ObjectionEntity.create(
                objectionNo, null, customerName, caseType, title, content, shapEvidence, staffNote, submittedAt
        );
    }

    private String requireText(CSVRecord record, String column, long rowNumber) {
        String value = record.get(column);
        if (value == null || value.isBlank()) {
            throw new InvalidObjectionFileException(rowNumber + "번째 행: '" + column + "' 값이 비어 있습니다.");
        }
        return value.trim();
    }
}