package com.finance.finance_management_system.controller;

import com.finance.finance_management_system.entity.User;
import com.finance.finance_management_system.security.CustomUserDetails;
import com.finance.finance_management_system.service.AccountService;
import com.finance.finance_management_system.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final AccountService accountService;
    private final TransactionService transactionService;

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        User user = userDetails.getUser();
        
        // Ensure default Cash Wallet exists
        accountService.ensureDefaultCashWallet(user);

        // Fetch metrics
        BigDecimal totalBalance = transactionService.getTotalBalance(user);
        BigDecimal totalIncome = transactionService.getTotalIncome(user);
        BigDecimal totalExpenses = transactionService.getTotalExpenses(user);
        BigDecimal totalSavings = totalIncome.subtract(totalExpenses);

        model.addAttribute("totalBalance", totalBalance);
        model.addAttribute("totalIncome", totalIncome);
        model.addAttribute("totalExpenses", totalExpenses);
        model.addAttribute("totalSavings", totalSavings);

        // Recent 5 transactions
        model.addAttribute("recentTransactions", transactionService.getRecentTransactions(user));

        // Chart data
        Map<String, BigDecimal> monthlyIncome = transactionService.getMonthlyIncomeChartData(user);
        Map<String, BigDecimal> monthlyExpense = transactionService.getMonthlyExpenseChartData(user);
        
        model.addAttribute("monthlyIncome", monthlyIncome);
        model.addAttribute("monthlyExpense", monthlyExpense);

        return "dashboard/index";
    }
}
