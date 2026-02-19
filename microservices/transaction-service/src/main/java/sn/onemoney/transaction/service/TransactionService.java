package sn.onemoney.transaction.service;

import sn.onemoney.transaction.model.Transaction;
import java.util.List;

public interface TransactionService {
    Transaction processPayment(String debitorPhone, String creditorId, long amount, String currency, String description);
    long getBalance(String phoneNumber);
    List<Transaction> streamTransactions(String phoneNumber, int limit);
}
