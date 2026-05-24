package com.finance.finance_management_system.service.impl;

import com.finance.finance_management_system.entity.Account;
import com.finance.finance_management_system.entity.Transaction;
import com.finance.finance_management_system.entity.TransactionType;
import com.finance.finance_management_system.entity.User;
import com.finance.finance_management_system.repository.AccountRepository;
import com.finance.finance_management_system.repository.TransactionRepository;
import com.finance.finance_management_system.service.AccountService;
import com.finance.finance_management_system.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final AccountService accountService;

    @Override
    public List<Transaction> getTransactionsByUser(User user) {
        return transactionRepository.findByUserOrderByTransactionDateDescIdDesc(user);
    }

    @Override
    public List<Transaction> getTransactionsByAccount(Long accountId, User user) {
        Account account = accountRepository.findByIdAndUser(accountId, user)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        return transactionRepository.findByAccountOrderByTransactionDateDescIdDesc(account);
    }

    @Override
    public List<Transaction> getRecentTransactions(User user) {
        return transactionRepository.findTop5ByUserOrderByTransactionDateDescIdDesc(user);
    }

    @Override
    public Transaction addTransaction(Transaction transaction, Long accountId, User user) {
        return addTransaction(transaction, accountId, user, null, null, null);
    }

    @Override
    public Transaction addTransaction(Transaction transaction, Long accountId, User user, BigDecimal deficitAmount, String fundingSource, String fundingSourceDetails) {
        Account account = accountRepository.findByIdAndUser(accountId, user)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        // If there's a deficit that is funded externally, record that transaction first
        if (deficitAmount != null && deficitAmount.compareTo(BigDecimal.ZERO) > 0 && fundingSource != null && !fundingSource.isEmpty()) {
            String detailsSuffix = (fundingSourceDetails != null && !fundingSourceDetails.trim().isEmpty()) 
                    ? " (" + fundingSourceDetails.trim() + ")" : "";
            
            Transaction fundingTx = Transaction.builder()
                    .amount(deficitAmount)
                    .type(TransactionType.INCOME)
                    .category(fundingSource)
                    .description("Covered deficit for " + (transaction.getDescription() != null && !transaction.getDescription().isEmpty() ? transaction.getDescription() : transaction.getCategory()) + detailsSuffix)
                    .transactionDate(transaction.getTransactionDate() != null ? transaction.getTransactionDate() : LocalDate.now())
                    .account(account)
                    .user(user)
                    .build();
            
            // Adjust balance for the funding transaction
            account.setBalance(account.getBalance().add(deficitAmount));
            transactionRepository.save(fundingTx);
        }

        transaction.setUser(user);
        transaction.setAccount(account);
        if (transaction.getTransactionDate() == null) {
            transaction.setTransactionDate(LocalDate.now());
        }

        // Update account balance
        if (transaction.getType() == TransactionType.INCOME) {
            account.setBalance(account.getBalance().add(transaction.getAmount()));
        } else {
            account.setBalance(account.getBalance().subtract(transaction.getAmount()));
        }

        accountRepository.save(account);
        return transactionRepository.save(transaction);
    }

    @Override
    public void addSplitTransactions(Transaction mainTransaction, List<Long> accountIds, List<BigDecimal> amounts, User user, BigDecimal deficitAmount, String fundingSource, String fundingSourceDetails) {
        // If there's a deficit, record the loan/borrow transaction first on the primary account
        if (deficitAmount != null && deficitAmount.compareTo(BigDecimal.ZERO) > 0 && fundingSource != null && !fundingSource.isEmpty()) {
            Account primaryAccount = accountRepository.findByIdAndUser(mainTransaction.getAccount() != null ? mainTransaction.getAccount().getId() : accountIds.get(0), user)
                    .orElseThrow(() -> new IllegalArgumentException("Primary account not found"));
            
            String detailsSuffix = (fundingSourceDetails != null && !fundingSourceDetails.trim().isEmpty()) 
                    ? " (" + fundingSourceDetails.trim() + ")" : "";
            
            Transaction fundingTx = Transaction.builder()
                    .amount(deficitAmount)
                    .type(TransactionType.INCOME)
                    .category(fundingSource)
                    .description("Covered deficit for split expense: " + (mainTransaction.getDescription() != null && !mainTransaction.getDescription().isEmpty() ? mainTransaction.getDescription() : mainTransaction.getCategory()) + detailsSuffix)
                    .transactionDate(mainTransaction.getTransactionDate() != null ? mainTransaction.getTransactionDate() : LocalDate.now())
                    .account(primaryAccount)
                    .user(user)
                    .build();
            
            primaryAccount.setBalance(primaryAccount.getBalance().add(deficitAmount));
            transactionRepository.save(fundingTx);
            accountRepository.save(primaryAccount);
        }

        // Now save a transaction for each split
        for (int i = 0; i < accountIds.size(); i++) {
            Long accId = accountIds.get(i);
            BigDecimal amount = amounts.get(i);
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) continue;

            Account account = accountRepository.findByIdAndUser(accId, user)
                    .orElseThrow(() -> new IllegalArgumentException("Account not found"));

            String suffix = " (Split: " + account.getName() + ")";
            String desc = (mainTransaction.getDescription() != null ? mainTransaction.getDescription() : "") + suffix;
            if (desc.length() > 255) {
                desc = desc.substring(0, 252) + "...";
            }

            Transaction splitTx = Transaction.builder()
                    .amount(amount)
                    .type(mainTransaction.getType())
                    .category(mainTransaction.getCategory())
                    .description(desc)
                    .transactionDate(mainTransaction.getTransactionDate() != null ? mainTransaction.getTransactionDate() : LocalDate.now())
                    .account(account)
                    .user(user)
                    .build();

            if (mainTransaction.getType() == TransactionType.INCOME) {
                account.setBalance(account.getBalance().add(amount));
            } else {
                account.setBalance(account.getBalance().subtract(amount));
            }

            transactionRepository.save(splitTx);
            accountRepository.save(account);
        }
    }

    @Override
    public Transaction updateTransaction(Long transactionId, Transaction updatedTransaction, Long accountId, User user) {
        Transaction existing = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));
        
        if (!existing.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized access to transaction");
        }

        // 1. Reverse the old transaction amount on the old account
        Account oldAccount = existing.getAccount();
        if (existing.getType() == TransactionType.INCOME) {
            oldAccount.setBalance(oldAccount.getBalance().subtract(existing.getAmount()));
        } else {
            oldAccount.setBalance(oldAccount.getBalance().add(existing.getAmount()));
        }
        accountRepository.save(oldAccount);

        // 2. Fetch new account
        Account newAccount = accountRepository.findByIdAndUser(accountId, user)
                .orElseThrow(() -> new IllegalArgumentException("New account not found"));

        // 3. Update existing transaction details
        existing.setAmount(updatedTransaction.getAmount());
        existing.setCategory(updatedTransaction.getCategory());
        existing.setDescription(updatedTransaction.getDescription());
        existing.setTransactionDate(updatedTransaction.getTransactionDate() != null ? updatedTransaction.getTransactionDate() : LocalDate.now());
        existing.setAccount(newAccount);
        existing.setType(updatedTransaction.getType());

        // 4. Apply new transaction amount to new account
        if (existing.getType() == TransactionType.INCOME) {
            newAccount.setBalance(newAccount.getBalance().add(existing.getAmount()));
        } else {
            newAccount.setBalance(newAccount.getBalance().subtract(existing.getAmount()));
        }
        accountRepository.save(newAccount);

        return transactionRepository.save(existing);
    }

    @Override
    public void deleteTransaction(Long id, User user) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        if (!transaction.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized access to transaction");
        }

        Account account = transaction.getAccount();
        if (transaction.getType() == TransactionType.INCOME) {
            account.setBalance(account.getBalance().subtract(transaction.getAmount()));
        } else {
            account.setBalance(account.getBalance().add(transaction.getAmount()));
        }
        accountRepository.save(account);
        transactionRepository.delete(transaction);
    }

    @Override
    public BigDecimal getTotalBalance(User user) {
        List<Account> accounts = accountService.getAccountsByUser(user);
        return accounts.stream()
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public BigDecimal getTotalIncome(User user) {
        BigDecimal income = transactionRepository.sumAmountByUserAndType(user, TransactionType.INCOME);
        return income != null ? income : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getTotalExpenses(User user) {
        BigDecimal expenses = transactionRepository.sumAmountByUserAndType(user, TransactionType.EXPENSE);
        return expenses != null ? expenses : BigDecimal.ZERO;
    }

    @Override
    public ByteArrayInputStream exportTransactionsToCsv(Long accountId, User user) {
        Account account = accountRepository.findByIdAndUser(accountId, user)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        List<Transaction> transactions = transactionRepository.findByAccountOrderByTransactionDateDescIdDesc(account);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
            
            // Header
            writer.write("Date,Type,Category,Amount,Description\n");
            
            for (Transaction t : transactions) {
                String desc = t.getDescription() != null ? t.getDescription().replace("\"", "\"\"") : "";
                if (desc.contains(",") || desc.contains("\"") || desc.contains("\n")) {
                    desc = "\"" + desc + "\"";
                }
                writer.write(String.format("%s,%s,%s,%s,%s\n",
                        t.getTransactionDate(),
                        t.getType().name(),
                        t.getCategory(),
                        t.getAmount().toString(),
                        desc));
            }
            writer.flush();
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Fail to export data to CSV file: " + e.getMessage());
        }
    }

    @Override
    public int importTransactionsFromCsv(MultipartFile file, Long targetAccountId, User user) throws Exception {
        Account targetAccount = accountRepository.findByIdAndUser(targetAccountId, user)
                .orElseThrow(() -> new IllegalArgumentException("Target account not found"));

        int count = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean isHeader = true;
            
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                if (isHeader) {
                    isHeader = false; // skip CSV header
                    continue;
                }
                
                // Parse CSV line (simple CSV parsing, splitting by comma outside quotes)
                String[] tokens = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                if (tokens.length < 4) {
                    continue;
                }
                
                LocalDate date = LocalDate.parse(tokens[0].trim());
                TransactionType type = TransactionType.valueOf(tokens[1].trim().toUpperCase());
                String category = tokens[2].trim();
                BigDecimal amount = new BigDecimal(tokens[3].trim());
                
                String description = "";
                if (tokens.length >= 5) {
                    description = tokens[4].trim();
                    if (description.startsWith("\"") && description.endsWith("\"")) {
                        description = description.substring(1, description.length() - 1).replace("\"\"", "\"");
                    }
                }
                
                Transaction transaction = Transaction.builder()
                        .transactionDate(date)
                        .type(type)
                        .category(category)
                        .amount(amount)
                        .description(description)
                        .user(user)
                        .account(targetAccount)
                        .build();
                
                // Update balance
                if (type == TransactionType.INCOME) {
                    targetAccount.setBalance(targetAccount.getBalance().add(amount));
                } else {
                    targetAccount.setBalance(targetAccount.getBalance().subtract(amount));
                }
                
                transactionRepository.save(transaction);
                count++;
            }
            
            accountRepository.save(targetAccount);
        }
        return count;
    }

    @Override
    public Map<String, BigDecimal> getMonthlyIncomeChartData(User user) {
        List<Object[]> results = transactionRepository.getMonthlyTransactionTotalsByUserAndType(user, TransactionType.INCOME);
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        for (Object[] row : results) {
            map.put((String) row[0], (BigDecimal) row[1]);
        }
        return map;
    }

    @Override
    public Map<String, BigDecimal> getMonthlyExpenseChartData(User user) {
        List<Object[]> results = transactionRepository.getMonthlyTransactionTotalsByUserAndType(user, TransactionType.EXPENSE);
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        for (Object[] row : results) {
            map.put((String) row[0], (BigDecimal) row[1]);
        }
        return map;
    }
}
