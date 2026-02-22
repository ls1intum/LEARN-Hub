package com.learnhub.usermanagement.service;

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

    public void sendTeacherCredentials(String to, String firstName, String password) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromName + " <" + fromAddress + ">");
        message.setTo(to);
        message.setSubject("Your LEARN-Hub Teacher Account");
        message.setText(String.format(
            "Hello %s,\n\n" +
            "Welcome to LEARN-Hub! Your teacher account has been created.\n\n" +
            "Your login credentials are:\n" +
            "Email: %s\n" +
            "Password: %s\n\n" +
            "Please keep this information secure and change your password after your first login.\n\n" +
            "Best regards,\n" +
            "The LEARN-Hub Team",
            firstName, to, password
        ));
        
        mailSender.send(message);
    }

    public void sendPasswordReset(String to, String firstName, String newPassword) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromName + " <" + fromAddress + ">");
        message.setTo(to);
        message.setSubject("Your Password Has Been Reset - LEARN-Hub");
        message.setText(String.format(
            "Hello %s,\n\n" +
            "Your password has been successfully reset.\n\n" +
            "Your new login credentials are:\n" +
            "Email: %s\n" +
            "Password: %s\n\n" +
            "Please keep this information secure and consider changing your password after logging in.\n\n" +
            "If you didn't request this password reset, please contact support immediately.\n\n" +
            "Best regards,\n" +
            "The LEARN-Hub Team",
            firstName, to, newPassword
        ));
        
        mailSender.send(message);
    }
}
