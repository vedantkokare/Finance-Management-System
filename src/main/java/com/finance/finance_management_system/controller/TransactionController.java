package com.finance.finance_management_system.controller;

import com.finance.finance_management_system.entity.Account;
import com.finance.finance_management_system.entity.Transaction;
import com.finance.finance_management_system.entity.User;
import com.finance.finance_management_system.security.CustomUserDetails;
import com.finance.finance_management_system.service.AccountService;
import com.finance.finance_management_system.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final AccountService accountService;

    @GetMapping("/transactions")
    public String viewTransactions(@AuthenticationPrincipal CustomUserDetails userDetails,
                                   @RequestParam(value = "accountId", required = false) Long accountId,
                                   Model model) {
        User user = userDetails.getUser();
        List<Account> accounts = accountService.getAccountsByUser(user);
        List<Transaction> transactions;

        if (accountId != null) {
            transactions = transactionService.getTransactionsByAccount(accountId, user);
            model.addAttribute("selectedAccountId", accountId);
        } else {
            transactions = transactionService.getTransactionsByUser(user);
        }

        BigDecimal totalBalance = transactionService.getTotalBalance(user);
        BigDecimal totalIncome = transactionService.getTotalIncome(user);
        BigDecimal totalExpenses = transactionService.getTotalExpenses(user);

        model.addAttribute("accounts", accounts);
        model.addAttribute("transactions", transactions);
        model.addAttribute("totalBalance", totalBalance);
        model.addAttribute("totalIncome", totalIncome);
        model.addAttribute("totalExpenses", totalExpenses);
        model.addAttribute("newTransaction", new Transaction());

        return "expenses/index";
    }

    @PostMapping("/transactions/add")
    public String addTransaction(@AuthenticationPrincipal CustomUserDetails userDetails,
                                 @ModelAttribute("newTransaction") Transaction transaction,
                                 @RequestParam("accountId") Long accountId,
                                 RedirectAttributes redirectAttributes) {
        User user = userDetails.getUser();
        try {
            transactionService.addTransaction(transaction, accountId, user);
            redirectAttributes.addFlashAttribute("success", "Transaction added successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error adding transaction: " + e.getMessage());
        }
        return "redirect:/transactions";
    }

    @GetMapping("/transactions/export")
    @ResponseBody
    public ResponseEntity<Resource> exportTransactions(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                       @RequestParam("accountId") Long accountId) {
        User user = userDetails.getUser();
        try {
            ByteArrayInputStream in = transactionService.exportTransactionsToCsv(accountId, user);
            InputStreamResource file = new InputStreamResource(in);
            
            Account account = accountService.getAccountByIdAndUser(accountId, user);
            String filename = (account != null ? account.getName().toLowerCase().replace(" ", "_") : "account") + "_transactions.csv";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                    .contentType(MediaType.parseMediaType("application/csv"))
                    .body(file);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/transactions/import")
    public String importTransactions(@AuthenticationPrincipal CustomUserDetails userDetails,
                                     @RequestParam("file") MultipartFile file,
                                     @RequestParam("targetAccountId") Long targetAccountId,
                                     RedirectAttributes redirectAttributes) {
        User user = userDetails.getUser();
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please select a CSV file to upload.");
            return "redirect:/transactions";
        }

        try {
            int count = transactionService.importTransactionsFromCsv(file, targetAccountId, user);
            redirectAttributes.addFlashAttribute("success", "Successfully imported " + count + " transactions!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error importing transactions: " + e.getMessage());
        }
        return "redirect:/transactions";
    }
}
