package com.ibm.capstone.bookstore.service;

import com.ibm.capstone.bookstore.domain.dto.BookSummaryResponse;
import java.util.List;

public interface RecommendationService {
    List<BookSummaryResponse> getHistoryRecommendations(Long userId, int limit);
}
