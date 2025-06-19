package com.koroliuk;

import java.util.concurrent.locks.StampedLock;

public class Account {

    private long balance;
    private final StampedLock lock = new StampedLock();

    public Account(long balance) {
        this.balance = balance;
    }

    public long getBalance() {
        return balance;
    }

    public void setBalance(long balance) {
        this.balance = balance;
    }

    public StampedLock getLock() {
        return lock;
    }
}
