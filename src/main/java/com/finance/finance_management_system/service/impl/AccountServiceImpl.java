package com.finance.finance_management_system.service.impl;

import com.finance.finance_management_system.entity.Account;
import com.finance.finance_management_system.entity.AccountType;
import com.finance.finance_management_system.entity.User;
import com.finance.finance_management_system.repository.AccountRepository;
import com.finance.finance_management_system.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;

    @Override
    public List<Account> getAccountsByUser(User user) {
        ensureDefaultCashWallet(user);
        return accountRepository.findByUser(user);
    }

    @Override
    public Account createAccount(Account account, User user) {
        account.setUser(user);
        if (account.getBalance() == null) {
            account.setBalance(BigDecimal.ZERO);
        }
        return accountRepository.save(account);
    }

    @Override
    public Account getAccountByIdAndUser(Long id, User user) {
        return accountRepository.findByIdAndUser(id, user).orElse(null);
    }

    @Override
    public Account ensureDefaultCashWallet(User user) {
        List<Account> accounts = accountRepository.findByUser(user);
        boolean hasCashWallet = accounts.stream()
                .anyMatch(a -> a.getAccountType() == AccountType.CASH && "Cash Wallet".equalsIgnoreCase(a.getName()));
        
        if (!hasCashWallet) {
            Account cashWallet = Account.builder()
                    .name("Cash Wallet")
                    .accountType(AccountType.CASH)
                    .balance(BigDecimal.ZERO)
                    .user(user)
                    .build();
            return accountRepository.save(cashWallet);
        }
        return accounts.stream()
                .filter(a -> a.getAccountType() == AccountType.CASH && "Cash Wallet".equalsIgnoreCase(a.getName()))
                .findFirst().orElse(null);
    }
}
