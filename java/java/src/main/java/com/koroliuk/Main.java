package com.koroliuk;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

import static com.koroliuk.ExchangeHelper.createRandomOperation;

public class Main {

    private static final int NUM_ACCOUNTS = 1_000;
    private static final int NUM_OPERATIONS = 100_000;
    private static final int INIT_BALANCE = 1_000;

    public static void main(String[] args) {
        final ExchangeHelper exchangeHelper = new ExchangeHelper(NUM_ACCOUNTS, INIT_BALANCE);
        final ThreadLocalRandom random = ThreadLocalRandom.current();

        ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor();
        CompletableFuture<?>[] operationFutures = new CompletableFuture[NUM_OPERATIONS];

        for (int i = 0; i < NUM_OPERATIONS; i++) {
            CompletableFuture<?> operationFuture = CompletableFuture.runAsync(() -> createRandomOperation(NUM_ACCOUNTS, random, exchangeHelper), pool);
            operationFutures[i] = operationFuture;
        }
        CompletableFuture.allOf(operationFutures).join();
        pool.shutdown();

        System.out.println("Σ = " + exchangeHelper.getTotalBalance());
    }
}
