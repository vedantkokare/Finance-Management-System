package com.finance.finance_management_system.service;

import com.finance.finance_management_system.entity.Transaction;
import com.finance.finance_management_system.entity.User;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface TransactionService {
    List<Transaction> getTransactionsByUser(User user);
    List<Transaction> getTransactionsByAccount(Long accountId, User user);
    List<Transaction> getRecentTransactions(User user);
    Transaction addTransaction(Transaction transaction, Long accountId, User user);
    Transaction addTransaction(Transaction transaction, Long accountId, User user, BigDecimal deficitAmount, String fundingSource, String fundingSourceDetails);
    void addSplitTransactions(Transaction mainTransaction, List<Long> accountIds, List<BigDecimal> amounts, User user, BigDecimal deficitAmount, String fundingSource, String fundingSourceDetails);
    Transaction updateTransaction(Long transactionId, Transaction updatedTransaction, Long accountId, User user);
    void deleteTransaction(Long id, User user);
    
    BigDecimal getTotalBalance(User user);
    BigDecimal getTotalIncome(User user);
    BigDecimal getTotalExpenses(User user);
    
    ByteArrayInputStream exportTransactionsToCsv(Long accountId, User user);
    int importTransactionsFromCsv(MultipartFile file, Long targetAccountId, User user) throws Exception;
    
    Map<String, BigDecimal> getMonthlyIncomeChartData(User user);
    Map<String, BigDecimal> getMonthlyExpenseChartData(User user);
}
