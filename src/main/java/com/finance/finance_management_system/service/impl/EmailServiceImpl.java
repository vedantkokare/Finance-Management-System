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
    public boolean sendPasswordResetEmail(String toEmail, String resetUrl) {
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
                return true;
            } else {
                log.warn("SMTP email sender not configured. Email simulation mode active. Please use the link above in console to reset password.");
                return false;
            }
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean sendLoginCredentialsEmail(String toEmail, String tempPassword) {
        log.info("==================================================================");
        log.info("LOGIN CREDENTIALS RECOVERY REQUEST FOR EMAIL: {}", toEmail);
        log.info("TEMPORARY PASSWORD GENERATED: {}", tempPassword);
        log.info("==================================================================");

        try {
            if (senderEmail != null && !senderEmail.isEmpty() && !senderEmail.contains("your-email")) {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(senderEmail);
                message.setTo(toEmail);
                message.setSubject("Your Login Credentials - FinTrack");
                message.setText("Hello,\n\nYou requested your login credentials for FinTrack.\n\n"
                        + "Here are your temporary login credentials (VALID FOR 10 MINUTES):\n"
                        + "Email: " + toEmail + "\n"
                        + "Temporary Password: " + tempPassword + "\n\n"
                        + "You can login immediately using these credentials. Please go to Account Settings immediately after logging in to set a permanent password.\n\n"
                        + "Best regards,\nFinTrack Team");
                mailSender.send(message);
                log.info("Login credentials successfully sent to {}", toEmail);
                return true;
            } else {
                log.warn("SMTP email sender not configured. Simulation mode active.");
                return false;
            }
        } catch (Exception e) {
            log.error("Failed to send login credentials email to {}: {}", toEmail, e.getMessage());
            return false;
        }
    }
}
