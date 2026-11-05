package com.ibm.capstone.bookstore.service.impl;

import com.ibm.capstone.bookstore.domain.dto.StoreDto;
import com.ibm.capstone.bookstore.domain.entities.User;
import com.ibm.capstone.bookstore.domain.entities.WalletTransaction;
import com.ibm.capstone.bookstore.exception.BadRequestException;
import com.ibm.capstone.bookstore.exception.ResourceNotFoundException;
import com.ibm.capstone.bookstore.repository.UserRepository;
import com.ibm.capstone.bookstore.repository.WalletTransactionRepository;
import com.ibm.capstone.bookstore.service.WalletService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Stored-value wallet: top-ups, order payments and refunds, each recorded in a ledger. */
@Service
public class WalletServiceImpl implements WalletService {

    private final UserRepository userRepository;
    private final WalletTransactionRepository transactionRepository;

    public WalletServiceImpl(UserRepository userRepository, WalletTransactionRepository transactionRepository) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public StoreDto.WalletResponse getWallet(Long userId) {
        User user = loadUser(userId);
        List<StoreDto.WalletTransactionDto> txns = transactionRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(t -> new StoreDto.WalletTransactionDto(t.getId(), t.getAmount(), t.getType().name(), t.getDescription(), t.getCreatedAt()))
                .toList();
        return new StoreDto.WalletResponse(user.getWalletBalance(), txns);
    }

    @Override
    @Transactional
    public StoreDto.WalletResponse topUp(Long userId, BigDecimal amount) {
        User user = loadUser(userId);
        BigDecimal value = amount.setScale(2, RoundingMode.HALF_UP);
        user.setWalletBalance(user.getWalletBalance().add(value));
        userRepository.save(user);
        transactionRepository.save(new WalletTransaction(user, null, value, WalletTransaction.Type.TOP_UP, "Wallet top-up"));
        return getWallet(userId);
    }

    /** Debits the wallet for an order; the caller has already checked the balance is sufficient. */
    @Override
    @Transactional
    public void debit(User user, BigDecimal amount, Long orderId, String description) {
        if (user.getWalletBalance().compareTo(amount) < 0) {
            throw new BadRequestException("Insufficient wallet balance. Available: " + user.getWalletBalance());
        }
        user.setWalletBalance(user.getWalletBalance().subtract(amount));
        userRepository.save(user);
        transactionRepository.save(new WalletTransaction(user, orderId, amount.negate(), WalletTransaction.Type.PAYMENT, description));
    }

    @Override
    @Transactional
    public void credit(User user, BigDecimal amount, Long orderId, String description) {
        user.setWalletBalance(user.getWalletBalance().add(amount));
        userRepository.save(user);
        transactionRepository.save(new WalletTransaction(user, orderId, amount, WalletTransaction.Type.REFUND, description));
    }

    private User loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }
}
