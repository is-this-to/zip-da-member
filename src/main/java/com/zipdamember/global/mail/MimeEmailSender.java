package com.zipdamember.global.mail;

import com.zipdamember.global.error.custom.BusinessException;
import com.zipdamember.global.response.constant.CustomResponseCode;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class MimeEmailSender implements EmailSender {
    // JavaMailSender: Spring이 제공하는 메일 발송 도구
    private final JavaMailSender mailSender;
    private final String senderEmail;

    public MimeEmailSender(JavaMailSender mailSender, @Value("${spring.mail.username}") String senderEmail
    ) {
        this.mailSender = mailSender;
        this.senderEmail = senderEmail;
    }

    @Override
    public void sendVerificationCode(String email, String verificationCode) {
        try {
            //MimeMessage: 전송할 이메일 전체를 표현하는 객체
            //  - 발신자, 수신자, 제목, 본문, HTML 여부, 첨부파일
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            //MimeMessageHelper: MimeMessage만 사용하면 설정 코드가 복잡 -> Helper를 사용하여 setter처럼 작성 가능
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    false, // multipart 메일 사용하지 않음 -> 첨부 파일 없음
                    StandardCharsets.UTF_8.name() // 문자 인코딩 UTF-8로 설정
            );

            helper.setFrom(senderEmail);
            helper.setTo(email);
            helper.setSubject("[ZIPDA] 이메일 인증번호 안내");
            helper.setText(createHtmlBody(verificationCode), true);

            mailSender.send(mimeMessage); // mail 실제로 전송
        } catch (MessagingException | MailException exception) {
            throw new BusinessException(
                    CustomResponseCode.EMAIL_SEND_ERROR,
                    "이메일 인증번호 발송에 실패했습니다.",
                    exception
            );
        }
    }

    @Override
    public void sendPasswordVerificationCode(String email, String verificationCode) {
        sendCodeEmail(
                email,
                verificationCode,
                "[ZIPDA] 비밀번호 변경 인증번호 안내",
                "비밀번호 변경을 위한 인증번호입니다."
        );
    }

    @Override
    public void sendPasswordResetLink(String email, String resetLink) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    false,
                    StandardCharsets.UTF_8.name()
            );
            helper.setFrom(senderEmail);
            helper.setTo(email);
            helper.setSubject("[ZIPDA] 비밀번호 재설정 안내");
            helper.setText(createPasswordResetBody(resetLink), true);
            mailSender.send(mimeMessage);
        } catch (MessagingException | MailException exception) {
            throw new BusinessException(
                    CustomResponseCode.EMAIL_SEND_ERROR,
                    "비밀번호 재설정 메일 발송에 실패했습니다.",
                    exception
            );
        }
    }

    private String createHtmlBody(String verificationCode) {
        return """
                <!doctype html>
                <html lang="ko">
                <body style="margin:0;padding:0;background-color:#f5f7f2;font-family:Arial,sans-serif;color:#263021;">
                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0"
                           style="background-color:#f5f7f2;padding:32px 16px;">
                        <tr>
                            <td align="center">
                                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0"
                                       style="max-width:560px;background-color:#ffffff;border-radius:16px;padding:40px;">
                                    <tr>
                                        <td style="color:#6f8750;font-size:24px;font-weight:700;padding-bottom:24px;">
                                            ZIPDA
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="font-size:22px;font-weight:700;padding-bottom:12px;">
                                            이메일 인증번호를 확인해주세요
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="color:#5d6658;font-size:15px;line-height:1.7;padding-bottom:28px;">
                                            회원가입을 위한 인증번호입니다.<br>
                                            아래 인증번호를 회원가입 화면에 입력해주세요.
                                        </td>
                                    </tr>
                                    <tr>
                                        <td align="center"
                                            style="background-color:#eef4e5;border-radius:12px;color:#4f6638;font-size:32px;font-weight:700;letter-spacing:10px;padding:22px;">
                                            %s
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="color:#7a8175;font-size:14px;line-height:1.7;padding-top:24px;">
                                            인증번호는 발송 시점부터 <strong>5분 동안</strong> 유효합니다.<br>
                                            본인이 요청하지 않았다면 이 메일을 무시해주세요.
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """.formatted(verificationCode);
    }

    private void sendCodeEmail(String email, String code, String subject, String description) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage, false, StandardCharsets.UTF_8.name()
            );
            helper.setFrom(senderEmail);
            helper.setTo(email);
            helper.setSubject(subject);
            helper.setText("""
                    <html lang="ko"><body style="font-family:Arial,sans-serif;padding:32px;color:#263021;">
                    <h2 style="color:#6f8750;">이메일 인증번호를 확인해주세요</h2>
                    <p>%s</p>
                    <div style="font-size:32px;font-weight:700;letter-spacing:10px;padding:22px;background:#eef4e5;">%s</div>
                    <p>인증번호는 발송 시점부터 5분 동안 유효합니다.</p>
                    </body></html>
                    """.formatted(description, code), true);
            mailSender.send(mimeMessage);
        } catch (MessagingException | MailException exception) {
            throw new BusinessException(
                    CustomResponseCode.EMAIL_SEND_ERROR,
                    "이메일 인증번호 발송에 실패했습니다.",
                    exception
            );
        }
    }

    private String createPasswordResetBody(String resetLink) {
        return """
                <!doctype html>
                <html lang="ko">
                <body style="font-family:Arial,sans-serif;color:#263021;padding:32px;">
                  <h2 style="color:#6f8750;">ZIPDA 비밀번호 재설정</h2>
                  <p>아래 버튼을 눌러 비밀번호를 재설정해 주세요.</p>
                  <p><a href="%s" style="display:inline-block;padding:12px 20px;background:#718355;color:#fff;text-decoration:none;border-radius:8px;">비밀번호 재설정</a></p>
                  <p style="color:#7a8175;">본인이 요청하지 않았다면 이 메일을 무시해 주세요.</p>
                </body>
                </html>
                """.formatted(resetLink);
    }
}
