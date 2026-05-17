package com.finance.finance_management_system.controller;

import com.finance.finance_management_system.entity.AuthProvider;
import com.finance.finance_management_system.entity.PasswordResetToken;
import com.finance.finance_management_system.entity.User;
import com.finance.finance_management_system.repository.PasswordResetTokenRepository;
import com.finance.finance_management_system.service.EmailService;
import com.finance.finance_management_system.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class PasswordResetController {

    private final UserService userService;
    private final EmailService emailService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam("email") String email, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        User user = userService.findByEmail(email);
        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "No account found with that email address.");
            return "redirect:/forgot-password";
        }

        if (user.getProvider() == AuthProvider.GOOGLE) {
            redirectAttributes.addFlashAttribute("error", "This email is registered via Google Login. Please click 'Continue with Google' on the login page.");
            return "redirect:/forgot-password";
        }

        String token = userService.createPasswordResetTokenForUser(user);
        
        String appUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
        String resetUrl = appUrl + "/reset-password?token=" + token;

        emailService.sendPasswordResetEmail(user.getEmail(), resetUrl);

        redirectAttributes.addFlashAttribute("success", "A password reset link has been sent to your email.");
        return "redirect:/forgot-password";
    }

    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam("token") String token, Model model, RedirectAttributes redirectAttributes) {
        String result = userService.validatePasswordResetToken(token);
        if (result != null) {
            redirectAttributes.addFlashAttribute("error", "The password reset link is invalid or expired.");
            return "redirect:/login";
        }

        model.addAttribute("token", token);
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam("token") String token,
                                       @RequestParam("password") String password,
                                       @RequestParam("confirmPassword") String confirmPassword,
                                       RedirectAttributes redirectAttributes) {
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Passwords do not match.");
            return "redirect:/reset-password?token=" + token;
        }

        if (password.length() < 6) {
            redirectAttributes.addFlashAttribute("error", "Password must be at least 6 characters.");
            return "redirect:/reset-password?token=" + token;
        }

        String result = userService.validatePasswordResetToken(token);
        if (result != null) {
            redirectAttributes.addFlashAttribute("error", "The password reset link is invalid or expired.");
            return "redirect:/login";
        }

        Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository.findByToken(token);
        if (tokenOpt.isPresent()) {
            User user = tokenOpt.get().getUser();
            userService.updatePassword(user, password);
        }

        redirectAttributes.addFlashAttribute("resetSuccess", "Your password has been successfully reset. You can now login.");
        return "redirect:/login";
    }
}
