package com.finance.finance_management_system.service;

import com.finance.finance_management_system.entity.Account;
import com.finance.finance_management_system.entity.User;

import java.util.List;

public interface AccountService {
    List<Account> getAccountsByUser(User user);
    Account createAccount(Account account, User user);
    Account getAccountByIdAndUser(Long id, User user);
    Account ensureDefaultCashWallet(User user);
}
