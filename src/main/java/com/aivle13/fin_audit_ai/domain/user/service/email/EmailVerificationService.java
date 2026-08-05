package com.aivle13.fin_audit_ai.domain.user.service.email;

import com.aivle13.fin_audit_ai.domain.user.dto.response.email.EmailVerificationConfirmResponse;
import com.aivle13.fin_audit_ai.domain.user.dto.response.email.EmailVerificationResponse;
import com.aivle13.fin_audit_ai.domain.user.repository.UserRepository;
import com.aivle13.fin_audit_ai.global.exception.user.common.DuplicateEmailException;
import com.aivle13.fin_audit_ai.global.exception.user.auth.InvalidVerificationCodeException;
import com.aivle13.fin_audit_ai.global.mail.MailService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Locale;

@Service
public class EmailVerificationService {

    // 발송된 인증번호 저장용 키
    private static final String CODE_KEY_PREFIX =
            "email-verification-code:";

    // 인증 완료 상태 저장용 키
    private static final String VERIFIED_KEY_PREFIX =
            "email-verification-verified:";

    // 인증번호 유효시간
    private static final Duration CODE_EXPIRATION =
            Duration.ofMinutes(5);

    // 인증 완료 후 회원가입까지 허용할 시간
    private static final Duration VERIFIED_EXPIRATION =
            Duration.ofMinutes(10);

    private final StringRedisTemplate redisTemplate;
    private final MailService mailService;
    private final UserRepository userRepository;
    private final SecureRandom secureRandom;

    public EmailVerificationService(
            StringRedisTemplate redisTemplate,
            MailService mailService,
            UserRepository userRepository
    ) {
        this.redisTemplate = redisTemplate;
        this.mailService = mailService;
        this.userRepository = userRepository;
        this.secureRandom = new SecureRandom();
    }

    // 이메일 인증번호 발송
    public EmailVerificationResponse sendVerificationCode(
            String email
    ) {
        String normalizedEmail =
                normalizeEmail(email);

        // 이미 가입된 이메일이면 발송하지 않음
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateEmailException();
        }

        String verificationCode =
                generateVerificationCode();

        String codeKey =
                createCodeKey(normalizedEmail);

        // 기존 인증 완료 상태가 있다면 초기화
        redisTemplate.delete(
                createVerifiedKey(normalizedEmail)
        );

        // 인증번호를 Redis에 5분간 저장
        redisTemplate.opsForValue().set(
                codeKey,
                verificationCode,
                CODE_EXPIRATION
        );

        try {
            mailService.sendVerificationCodeMail(
                    normalizedEmail,
                    verificationCode
            );
        } catch (RuntimeException exception) {

            // 메일 발송 실패 시 저장한 인증번호 삭제
            redisTemplate.delete(codeKey);

            throw exception;
        }

        return EmailVerificationResponse.success();
    }

    // 사용자가 입력한 인증번호 확인
    public EmailVerificationConfirmResponse confirmVerificationCode(
            String email,
            String code
    ) {
        String normalizedEmail =
                normalizeEmail(email);

        String codeKey =
                createCodeKey(normalizedEmail);

        String savedCode =
                redisTemplate.opsForValue().get(codeKey);

        String inputCode =
                code.trim();

        // 저장된 코드가 없거나 입력 코드와 다르면 실패
        if (savedCode == null
                || !savedCode.equals(inputCode)) {

            throw new InvalidVerificationCodeException();
        }

        // 인증번호 재사용 방지를 위해 삭제
        redisTemplate.delete(codeKey);

        // 해당 이메일이 인증을 완료했다는 상태 저장
        redisTemplate.opsForValue().set(
                createVerifiedKey(normalizedEmail),
                "true",
                VERIFIED_EXPIRATION
        );

        return EmailVerificationConfirmResponse.success();
    }

    // 회원가입 전에 이메일 인증 완료 여부 확인
    public void validateVerifiedEmail(
            String email
    ) {
        String normalizedEmail =
                normalizeEmail(email);

        String verified =
                redisTemplate.opsForValue().get(
                        createVerifiedKey(normalizedEmail)
                );

        if (!"true".equals(verified)) {
            throw new InvalidVerificationCodeException();
        }
    }

    // 회원가입 성공 후 인증 완료 상태 삭제
    public void consumeVerification(
            String email
    ) {
        String normalizedEmail =
                normalizeEmail(email);

        redisTemplate.delete(
                createVerifiedKey(normalizedEmail)
        );
    }

    // 숫자 6자리 인증번호 생성
    private String generateVerificationCode() {
        int number =
                secureRandom.nextInt(1_000_000);

        return String.format("%06d", number);
    }

    private String createCodeKey(
            String email
    ) {
        return CODE_KEY_PREFIX + email;
    }

    private String createVerifiedKey(
            String email
    ) {
        return VERIFIED_KEY_PREFIX + email;
    }

    private String normalizeEmail(
            String email
    ) {
        return email
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}