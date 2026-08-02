package com.aivle13.fin_audit_ai.global.mail;

import com.aivle13.fin_audit_ai.global.exception.mail.EmailSendFailedException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

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

    // 회원가입 이메일 인증번호 발송
    public void sendVerificationCodeMail(
            String receiverEmail,
            String verificationCode
    ) {
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
                    "[FinAuditAI] 회원가입 이메일 인증번호"
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
                            이메일 인증
                        </h1>

                        <p style="
                            line-height: 1.7;
                            font-size: 15px;
                        ">
                            FinAuditAI 회원가입을 위한
                            이메일 인증번호입니다.
                        </p>

                        <div style="
                            margin-top: 24px;
                            margin-bottom: 24px;
                            padding: 20px;
                            border-radius: 8px;
                            background-color: #f2f4f7;
                            text-align: center;
                        ">
                            <strong style="
                                font-size: 32px;
                                letter-spacing: 8px;
                                color: #3268e8;
                            ">
                                %s
                            </strong>
                        </div>

                        <p style="
                            color: #667085;
                            font-size: 13px;
                            line-height: 1.6;
                        ">
                            인증번호는 5분 동안 유효합니다.<br>
                            본인이 요청하지 않았다면
                            이 메일을 무시해주세요.
                        </p>
                    </div>
                    """.formatted(verificationCode);

            helper.setText(html, true);

            mailSender.send(message);

        } catch (MessagingException | MailException exception) {
            throw new EmailSendFailedException(exception);
        }
    }

    // 비밀번호 재설정 이메일 발송
    public void sendPasswordResetMail(
            String receiverEmail,
            String token
    ) {
        String resetUrl =
                frontendBaseUrl
                        + "/reset-password?token="
                        + token;

        try {
            MimeMessage message =
                    mailSender.createMimeMessage();

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
                            본인이 요청하지 않았다면
                            이 메일을 무시해주세요.
                        </p>
                    </div>
                    """.formatted(resetUrl);

            helper.setText(html, true);

            mailSender.send(message);

        } catch (MessagingException | MailException e) {
            throw new EmailSendFailedException(e);
        }
    }

    // 법령 개정 알림 이메일 발송
    public void sendLawRevisionMail(
            String receiverEmail,
            String revisionTitle
    ) {
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
                    "[FinAuditAI] 법령 개정 안내"
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
                            법령 개정 안내
                        </h1>

                        <p style="
                            line-height: 1.7;
                            font-size: 15px;
                        ">
                            관련 법령·고시가 개정되었습니다.
                        </p>

                        <div style="
                            margin-top: 24px;
                            margin-bottom: 24px;
                            padding: 20px;
                            border-radius: 8px;
                            background-color: #f2f4f7;
                        ">
                            <strong style="font-size: 16px;">
                                %s
                            </strong>
                        </div>

                        <p style="
                            color: #667085;
                            font-size: 13px;
                            line-height: 1.6;
                        ">
                            자세한 내용은 마이페이지 알림에서 확인해주세요.
                        </p>
                    </div>
                    """.formatted(HtmlUtils.htmlEscape(revisionTitle));

            helper.setText(html, true);

            mailSender.send(message);

        } catch (MessagingException | MailException e) {
            throw new EmailSendFailedException(e);
        }
    }

    // 이의제기 대응문서(고객 안내문) 발송
    public void sendObjectionResponseMail(
            String receiverEmail,
            String customerName,
            String letterTitle,
            String letterBody
    ) {
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

            // letterTitle은 담당자가 직접 입력할 수 있는 값이라, 개행 문자가 섞여 있으면
            // Subject 헤더 인젝션으로 이어질 수 있어 헤더에 넣기 전에 개행을 제거한다.
            String sanitizedTitle = sanitizeHeaderValue(letterTitle);

            helper.setSubject(
                    "[FinAuditAI] " + sanitizedTitle
            );

            // 담당자가 직접입력으로 수정 가능한 값들이라 이스케이프 후 줄바꿈만 <br>로 변환한다.
            String escapedBody = HtmlUtils.htmlEscape(letterBody)
                    .replace("\n", "<br>");

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
                            %s
                        </h1>

                        <p style="
                            margin-bottom: 20px;
                            line-height: 1.7;
                            font-size: 15px;
                        ">
                            %s님, 안녕하세요.
                        </p>

                        <div style="
                            margin-top: 16px;
                            padding: 20px;
                            border-radius: 8px;
                            background-color: #f2f4f7;
                            line-height: 1.8;
                            font-size: 14px;
                        ">
                            %s
                        </div>

                        <p style="
                            margin-top: 28px;
                            color: #667085;
                            font-size: 13px;
                            line-height: 1.6;
                        ">
                            본 메일은 신용정보법 제36조의2에 따른 이의제기 처리 결과 안내입니다.<br>
                            문의사항은 이의제기 접수 화면을 통해 남겨주시기 바랍니다.
                        </p>
                    </div>
                    """.formatted(
                    HtmlUtils.htmlEscape(sanitizedTitle),
                    HtmlUtils.htmlEscape(customerName),
                    escapedBody
            );

            helper.setText(html, true);

            mailSender.send(message);

        } catch (MessagingException | MailException e) {
            throw new EmailSendFailedException(e);
        }
    }

    // 이메일 헤더(Subject 등)에 CRLF가 섞여 헤더 인젝션이 발생하지 않도록 개행 문자를 제거한다.
    private String sanitizeHeaderValue(String value) {
        return value.replaceAll("[\r\n]+", " ").trim();
    }
}