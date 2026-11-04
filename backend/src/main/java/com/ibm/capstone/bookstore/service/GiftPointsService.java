package com.ibm.capstone.bookstore.service;

import com.ibm.capstone.bookstore.domain.dto.GiftPointsSummaryResponse;
import com.ibm.capstone.bookstore.domain.entities.PointsTransactionType;
import com.ibm.capstone.bookstore.domain.entities.User;

public interface GiftPointsService {
    GiftPointsSummaryResponse getGiftPointsSummary(Long userId);
    void recordTransaction(User user, Long orderId, int points, PointsTransactionType type, String description);
}
