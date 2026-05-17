package com.finance.finance_management_system.service;

public interface EmailService {
    void sendPasswordResetEmail(String toEmail, String resetUrl);
}
