package com.finance.finance_management_system.repository;

import com.finance.finance_management_system.entity.Account;
import com.finance.finance_management_system.entity.Transaction;
import com.finance.finance_management_system.entity.TransactionType;
import com.finance.finance_management_system.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByUserOrderByTransactionDateDescIdDesc(User user);
    List<Transaction> findByAccountOrderByTransactionDateDescIdDesc(Account account);
    List<Transaction> findByUserAndAccountOrderByTransactionDateDescIdDesc(User user, Account account);
    List<Transaction> findTop5ByUserOrderByTransactionDateDescIdDesc(User user);
    
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.user = :user AND t.type = :type")
    BigDecimal sumAmountByUserAndType(@Param("user") User user, @Param("type") TransactionType type);

    @Query("SELECT DATE_FORMAT(t.transactionDate, '%Y-%m'), SUM(t.amount) " +
           "FROM Transaction t WHERE t.user = :user AND t.type = :type " +
           "GROUP BY DATE_FORMAT(t.transactionDate, '%Y-%m') " +
           "ORDER BY DATE_FORMAT(t.transactionDate, '%Y-%m') ASC")
    List<Object[]> getMonthlyTransactionTotalsByUserAndType(@Param("user") User user, @Param("type") TransactionType type);
}
