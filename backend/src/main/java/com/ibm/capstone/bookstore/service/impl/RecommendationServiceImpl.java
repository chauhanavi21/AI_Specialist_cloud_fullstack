package com.ibm.capstone.bookstore.service.impl;

import com.ibm.capstone.bookstore.domain.dto.BookSummaryResponse;
import com.ibm.capstone.bookstore.domain.entities.Book;
import com.ibm.capstone.bookstore.repository.BookRepository;
import com.ibm.capstone.bookstore.repository.OrderRepository;
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

    public RecommendationServiceImpl(OrderRepository orderRepository,
                                     BookRepository bookRepository) {
        this.orderRepository = orderRepository;
        this.bookRepository = bookRepository;
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
            recommendedBooks = bookRepository.findRecommendationsByCategories(purchasedCategories, purchasedBookIds, pageable);
        }

        if (recommendedBooks.size() < limit) {
            List<Book> topRated = bookRepository.findTopRatedBooks(pageable);
            for (Book b : topRated) {
                if (recommendedBooks.size() >= limit) break;
                if (!recommendedBooks.contains(b)) {
                    recommendedBooks.add(b);
                }
            }
        }

        return recommendedBooks.stream()
                .map(this::mapToBookSummary)
                .collect(Collectors.toList());
    }

    private BookSummaryResponse mapToBookSummary(Book book) {
        BookSummaryResponse dto = new BookSummaryResponse();
        dto.setId(book.getId());
        dto.setTitle(book.getTitle());
        dto.setAuthor(book.getAuthor());
        dto.setIsbn(book.getIsbn());
        dto.setPrice(book.getPrice());
        dto.setOriginalPrice(book.getOriginalPrice());
        dto.setRating(book.getRating());
        dto.setReviewCount(book.getReviewCount());
        dto.setCoverImageUrl(book.getCoverImageUrl());
        dto.setCategoryId(book.getCategory().getId());
        dto.setCategoryName(book.getCategory().getName());
        dto.setPublisherId(book.getPublisher().getId());
        dto.setPublisherName(book.getPublisher().getName());
        dto.setStockQuantity(book.getStockQuantity());
        dto.setInStock(book.getStockQuantity() > 0);
        dto.setTentativeDeliveryDays(book.getTentativeDeliveryDays());
        dto.setIsFeatured(book.getIsFeatured());
        return dto;
    }
}
