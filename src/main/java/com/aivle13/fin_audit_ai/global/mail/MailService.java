package com.aivle13.fin_audit_ai.global.mail;

import com.aivle13.fin_audit_ai.global.exception.mail.EmailSendFailedException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private final JavaMailSender mailSender;
    private final String frontendBaseUrl;
    private final String senderEmail;

    public MailService(
            JavaMailSender mailSender,
            @Value("${app.frontend-base-url}")
            String frontendBaseUrl,
            @Value("${spring.mail.username}")
            String senderEmail
    ) {
        this.mailSender = mailSender;
        this.frontendBaseUrl = frontendBaseUrl;
        this.senderEmail = senderEmail;
    }

    public void sendPasswordResetMail(
            String receiverEmail,
            String token
    ) {
        String resetUrl =
                frontendBaseUrl
                        + "/reset-password?token="
                        + token;

        try {
            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(
                            message,
                            false,
                            "UTF-8"
                    );

            helper.setFrom(senderEmail);
            helper.setTo(receiverEmail);
            helper.setSubject(
                    "[FinAuditAI] 비밀번호 재설정 안내"
            );

            String html = """
                    <div style="
                        max-width: 600px;
                        margin: 0 auto;
                        padding: 32px;
                        font-family: Arial, sans-serif;
                        color: #202939;
                    ">
                        <h1 style="
                            margin-bottom: 24px;
                            font-size: 28px;
                        ">
                            비밀번호 재설정
                        </h1>

                        <p style="
                            line-height: 1.7;
                            font-size: 15px;
                        ">
                            FinAuditAI 계정의 비밀번호 재설정을
                            요청하셨습니다.
                        </p>

                        <p style="
                            line-height: 1.7;
                            font-size: 15px;
                        ">
                            아래 버튼을 눌러 새로운 비밀번호를
                            설정해주세요.
                        </p>

                        <a href="%s"
                           style="
                               display: inline-block;
                               margin-top: 16px;
                               padding: 14px 24px;
                               border-radius: 4px;
                               background-color: #3268e8;
                               color: #ffffff;
                               text-decoration: none;
                               font-weight: bold;
                           ">
                            새 비밀번호 설정하기
                        </a>

                        <p style="
                            margin-top: 28px;
                            color: #667085;
                            font-size: 13px;
                            line-height: 1.6;
                        ">
                            해당 링크는 30분 동안 유효합니다.<br>
                            본인이 요청하지 않았다면 이 메일을
                            무시해주세요.
                        </p>
                    </div>
                    """.formatted(resetUrl);

            helper.setText(html, true);

            mailSender.send(message);

        } catch (MessagingException | MailException e) {
            throw new EmailSendFailedException(e);
        }
    }
}