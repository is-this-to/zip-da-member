package com.zipdamember.global.mail;

public interface EmailSender {

    void sendVerificationCode(String email, String verificationCode);

    void sendPasswordVerificationCode(String email, String verificationCode);

    void sendPasswordResetLink(String email, String resetLink);
}
