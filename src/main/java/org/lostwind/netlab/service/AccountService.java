package org.lostwind.netlab.service;

import org.lostwind.netlab.entity.Account;
import org.lostwind.netlab.mapper.AccountMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AccountService {
    @Autowired
    AccountMapper mapper;

    public Account getAccountById(Integer id) {
        Account account = mapper.selectById(id);
        if (account == null) {
            throw new IllegalArgumentException("当前账号不存在");
        }
        return account;
    }

    public Account getAccountByUsername(String name) {
        Account account = mapper.selectByUsername(name);
        if (account == null) {
            throw new IllegalArgumentException("当前账号不存在");
        }
        return account;
    }
}
