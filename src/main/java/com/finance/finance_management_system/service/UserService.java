package com.finance.finance_management_system.service;

import com.finance.finance_management_system.dto.UserRegistrationDto;
import com.finance.finance_management_system.entity.User;

public interface UserService {
    void registerUser(UserRegistrationDto registrationDto);

    User findByEmail(String email);

    boolean existsByEmail(String email);

    String createPasswordResetTokenForUser(User user);

    String validatePasswordResetToken(String token);

    void updatePassword(User user, String newPassword);

    String generateAndSetTemporaryPassword(User user);
}
