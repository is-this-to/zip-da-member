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
}
