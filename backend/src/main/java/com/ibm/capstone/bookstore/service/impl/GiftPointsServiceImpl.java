package com.ibm.capstone.bookstore.service.impl;

import com.ibm.capstone.bookstore.domain.dto.GiftPointsSummaryResponse;
import com.ibm.capstone.bookstore.domain.dto.GiftPointsTransactionDto;
import com.ibm.capstone.bookstore.domain.entities.GiftPointsLedger;
import com.ibm.capstone.bookstore.domain.entities.PointsTransactionType;
import com.ibm.capstone.bookstore.domain.entities.User;
import com.ibm.capstone.bookstore.exception.ResourceNotFoundException;
import com.ibm.capstone.bookstore.repository.GiftPointsLedgerRepository;
import com.ibm.capstone.bookstore.repository.UserRepository;
import com.ibm.capstone.bookstore.service.GiftPointsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class GiftPointsServiceImpl implements GiftPointsService {

    private final GiftPointsLedgerRepository ledgerRepository;
    private final UserRepository userRepository;

    public GiftPointsServiceImpl(GiftPointsLedgerRepository ledgerRepository, UserRepository userRepository) {
        this.ledgerRepository = ledgerRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public GiftPointsSummaryResponse getGiftPointsSummary(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        List<GiftPointsTransactionDto> transactions = ledgerRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(t -> new GiftPointsTransactionDto(
                        t.getId(),
                        t.getPoints(),
                        t.getType(),
                        t.getDescription(),
                        t.getCreatedAt()
                ))
                .collect(Collectors.toList());

        return new GiftPointsSummaryResponse(user.getGiftPointsBalance(), transactions);
    }

    @Override
    @Transactional
    public void recordTransaction(User user, Long orderId, int points, PointsTransactionType type, String description) {
        GiftPointsLedger ledger = new GiftPointsLedger(user, orderId, points, type, description);
        ledgerRepository.save(ledger);
    }
}
