package com.finance.finance_management_system.service.impl;

import com.finance.finance_management_system.dto.UserRegistrationDto;
import com.finance.finance_management_system.entity.AuthProvider;
import com.finance.finance_management_system.entity.PasswordResetToken;
import com.finance.finance_management_system.entity.Role;
import com.finance.finance_management_system.entity.User;
import com.finance.finance_management_system.repository.PasswordResetTokenRepository;
import com.finance.finance_management_system.repository.UserRepository;
import com.finance.finance_management_system.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void registerUser(UserRegistrationDto registrationDto) {
        User user = User.builder()
                .firstName(registrationDto.getFirstName())
                .lastName(registrationDto.getLastName())
                .email(registrationDto.getEmail())
                .password(passwordEncoder.encode(registrationDto.getPassword()))
                .role(Role.ROLE_USER)
                .provider(AuthProvider.LOCAL)
                .build();
        
        userRepository.save(user);
    }

    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public String createPasswordResetTokenForUser(User user) {
        passwordResetTokenRepository.deleteByUser(user);
        
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiryDate(LocalDateTime.now().plusHours(1))
                .build();
        
        passwordResetTokenRepository.save(resetToken);
        return token;
    }

    @Override
    public String validatePasswordResetToken(String token) {
        Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository.findByToken(token);
        if (tokenOpt.isEmpty()) {
            return "invalidToken";
        }
        
        PasswordResetToken resetToken = tokenOpt.get();
        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            passwordResetTokenRepository.delete(resetToken);
            return "expired";
        }
        
        return null;
    }

    @Override
    public void updatePassword(User user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        passwordResetTokenRepository.deleteByUser(user);
    }
}
