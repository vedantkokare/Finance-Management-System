package com.finance.finance_management_system.service.impl;

import com.finance.finance_management_system.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:#{null}}")
    private String senderEmail;

    @Override
    public void sendPasswordResetEmail(String toEmail, String resetUrl) {
        log.info("==================================================================");
        log.info("PASSWORD RESET REQUEST FOR EMAIL: {}", toEmail);
        log.info("CLICK THE FOLLOWING LINK TO RESET PASSWORD: {}", resetUrl);
        log.info("==================================================================");

        try {
            if (senderEmail != null && !senderEmail.isEmpty() && !senderEmail.contains("your-email")) {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(senderEmail);
                message.setTo(toEmail);
                message.setSubject("Password Reset Request - FinTrack");
                message.setText("Hello,\n\nYou have requested to reset your password. Click the link below to reset it:\n\n"
                        + resetUrl + "\n\nIf you did not request this, please ignore this email.\n\nBest regards,\nFinTrack Team");
                mailSender.send(message);
                log.info("Password reset email successfully sent to {}", toEmail);
            } else {
                log.warn("SMTP email sender not configured. Email simulation mode active. Please use the link above in console to reset password.");
            }
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
        }
    }
}
