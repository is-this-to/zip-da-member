package com.zipdamember.global.mail;

public interface EmailSender {

    void sendVerificationCode(String email, String verificationCode);
}
