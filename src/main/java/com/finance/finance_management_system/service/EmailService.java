package com.finance.finance_management_system.service;

public interface EmailService {
    boolean sendPasswordResetEmail(String toEmail, String resetUrl);
    boolean sendLoginCredentialsEmail(String toEmail, String tempPassword);
}
