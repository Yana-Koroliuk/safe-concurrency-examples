package com.koroliuk;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class ExchangeHelper {

    private static final int MAX_ATTEMPTS = 3;
    private static final int BASE_AMOUNT = 1;
    private static final int BOUND = 50;

    private final List<Account> accounts;

    public ExchangeHelper(int accountNumber, long initBalance) {
        accounts = new ArrayList<>();
        for (int i = 0; i < accountNumber; i++) {
            accounts.add(new Account(initBalance));
        }
    }

    public long getTotalBalance() {
        long sum = 0;
        for (Account account : accounts) {
            long stamp = account.getLock().readLock();
            try {
                sum += account.getBalance();
            } finally {
                account.getLock().unlockRead(stamp);
            }
        }
        return sum;
    }

    public void createRandomOperation(ThreadLocalRandom random) {
        int numAccounts = accounts.size();
        int srcAccountIdx, destAccountIdx;
        do {
            srcAccountIdx = random.nextInt(numAccounts);
            destAccountIdx = random.nextInt(numAccounts);
        } while (srcAccountIdx == destAccountIdx);

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            long amount = BASE_AMOUNT + random.nextLong(BOUND);
            if (tryTransfer(srcAccountIdx, destAccountIdx, amount))
                break;
        }
    }

    private boolean tryTransfer(int srcAccountIdx, int destAccountIdx, long amount) {
        if (srcAccountIdx == destAccountIdx)
            return false;

        Account srcAccount = accounts.get(srcAccountIdx);
        Account destAccount = accounts.get(destAccountIdx);
        long writeLock1 = srcAccount.getLock().writeLock();
        long writeLock2 = destAccount.getLock().writeLock();
        try {
            if (srcAccount.getBalance() < amount)
                return false;

            long currSrcBalance = srcAccount.getBalance();
            long currDestBalance = destAccount.getBalance();
            srcAccount.setBalance(currSrcBalance - amount);
            destAccount.setBalance(currDestBalance + amount);
            return true;
        } finally {
            srcAccount.getLock().unlockWrite(writeLock1);
            destAccount.getLock().unlockWrite(writeLock2);
        }
    }
}
