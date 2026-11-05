package com.ibm.capstone.bookstore.service;

import com.ibm.capstone.bookstore.domain.dto.StoreDto;
import com.ibm.capstone.bookstore.domain.entities.User;

import java.math.BigDecimal;

/** Stored-value wallet: top-ups, order payments and refunds, each recorded in a ledger. */
public interface WalletService {

    StoreDto.WalletResponse getWallet(Long userId);

    StoreDto.WalletResponse topUp(Long userId, BigDecimal amount);

    /** Debits the wallet for an order; fails if the balance is insufficient. */
    void debit(User user, BigDecimal amount, Long orderId, String description);

    /** Returns money to the wallet, for example when an order is cancelled. */
    void credit(User user, BigDecimal amount, Long orderId, String description);
}
