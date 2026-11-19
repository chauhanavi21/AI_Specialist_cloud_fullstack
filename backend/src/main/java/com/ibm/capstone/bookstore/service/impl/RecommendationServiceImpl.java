package com.ibm.capstone.bookstore.service.impl;

import com.ibm.capstone.bookstore.domain.dto.BookSummaryResponse;
import com.ibm.capstone.bookstore.domain.entities.Book;
import com.ibm.capstone.bookstore.repository.BookRepository;
import com.ibm.capstone.bookstore.repository.OrderRepository;
import com.ibm.capstone.bookstore.security.Entitlements;
import com.ibm.capstone.bookstore.service.RecommendationService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RecommendationServiceImpl implements RecommendationService {

    private final OrderRepository orderRepository;
    private final BookRepository bookRepository;
    private final BookServiceImpl bookService;

    public RecommendationServiceImpl(OrderRepository orderRepository,
                                     BookRepository bookRepository,
                                     BookServiceImpl bookService) {
        this.orderRepository = orderRepository;
        this.bookRepository = bookRepository;
        this.bookService = bookService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookSummaryResponse> getHistoryRecommendations(Long userId, int limit) {
        List<Long> purchasedCategories = orderRepository.findPurchasedCategoryIdsByUserId(userId);
        List<Long> purchasedBookIds = orderRepository.findPurchasedBookIdsByUserId(userId);
        if (purchasedBookIds.isEmpty()) {
            purchasedBookIds = List.of(-1L);
        }

        Pageable pageable = PageRequest.of(0, Math.max(1, limit));
        List<Book> recommendedBooks = new ArrayList<>();

        if (!purchasedCategories.isEmpty()) {
            recommendedBooks = new ArrayList<>(
                    bookRepository.findRecommendationsByCategories(purchasedCategories, purchasedBookIds,
                            Entitlements.visibleCategories(), pageable));
        }

        if (recommendedBooks.size() < limit) {
            for (Book b : bookRepository.findTopRatedBooks(Entitlements.visibleCategories(), pageable)) {
                if (recommendedBooks.size() >= limit) break;
                if (!recommendedBooks.contains(b)) {
                    recommendedBooks.add(b);
                }
            }
        }

        return recommendedBooks.stream()
                .map(bookService::mapToBookSummary)
                .collect(Collectors.toList());
    }
}
