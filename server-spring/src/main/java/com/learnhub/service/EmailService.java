package com.learnhub.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${email.from-address}")
    private String fromAddress;

    @Value("${email.from-name}")
    private String fromName;

    public void sendVerificationCode(String to, String code, String firstName) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromName + " <" + fromAddress + ">");
        message.setTo(to);
        message.setSubject("Your LEARN-Hub Verification Code");
        message.setText(String.format(
            "Hello %s,\n\n" +
            "Your verification code is: %s\n\n" +
            "This code will expire in 10 minutes.\n\n" +
            "If you didn't request this code, please ignore this email.\n\n" +
            "Best regards,\n" +
            "The LEARN-Hub Team",
            firstName, code
        ));
        
        mailSender.send(message);
    }

    public void sendPasswordResetCode(String to, String code, String firstName) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromName + " <" + fromAddress + ">");
        message.setTo(to);
        message.setSubject("Password Reset Code for LEARN-Hub");
        message.setText(String.format(
            "Hello %s,\n\n" +
            "Your password reset code is: %s\n\n" +
            "This code will expire in 10 minutes.\n\n" +
            "If you didn't request this code, please ignore this email.\n\n" +
            "Best regards,\n" +
            "The LEARN-Hub Team",
            firstName, code
        ));
        
        mailSender.send(message);
    }
}
