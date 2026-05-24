package com.finance.finance_management_system.repository;

import com.finance.finance_management_system.entity.Account;
import com.finance.finance_management_system.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findByUser(User user);
    Optional<Account> findByIdAndUser(Long id, User user);
    boolean existsByUser(User user);
}
