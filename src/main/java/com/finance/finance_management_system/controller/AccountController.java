package com.finance.finance_management_system.controller;

import com.finance.finance_management_system.entity.Account;
import com.finance.finance_management_system.entity.AccountType;
import com.finance.finance_management_system.entity.User;
import com.finance.finance_management_system.security.CustomUserDetails;
import com.finance.finance_management_system.service.AccountService;
import com.finance.finance_management_system.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final TransactionService transactionService;

    @GetMapping("/accounts")
    public String viewAccounts(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        User user = userDetails.getUser();
        List<Account> accounts = accountService.getAccountsByUser(user);
        BigDecimal totalBalance = transactionService.getTotalBalance(user);

        model.addAttribute("accounts", accounts);
        model.addAttribute("totalBalance", totalBalance);
        model.addAttribute("newAccount", new Account());
        return "accounts/index";
    }

    @PostMapping("/accounts")
    public String addAccount(@AuthenticationPrincipal CustomUserDetails userDetails,
                             @ModelAttribute("newAccount") Account account,
                             RedirectAttributes redirectAttributes) {
        User user = userDetails.getUser();
        
        if (account.getBalance() == null) {
            account.setBalance(BigDecimal.ZERO);
        }
        account.setAccountType(AccountType.BANK); // Users create BANK accounts
        
        accountService.createAccount(account, user);
        
        redirectAttributes.addFlashAttribute("success", "Bank Account created successfully!");
        return "redirect:/accounts";
    }
}
