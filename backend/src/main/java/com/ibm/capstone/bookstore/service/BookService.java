package com.ibm.capstone.bookstore.service;

import com.ibm.capstone.bookstore.domain.dto.BookDetailResponse;
import com.ibm.capstone.bookstore.domain.dto.BookPageResponse;
import com.ibm.capstone.bookstore.domain.dto.BookSummaryResponse;
import com.ibm.capstone.bookstore.domain.dto.CategoryResponse;
import com.ibm.capstone.bookstore.domain.dto.PublisherResponse;

import java.math.BigDecimal;
import java.util.List;

public interface BookService {
    List<CategoryResponse> getAllCategories();
    List<PublisherResponse> getAllPublishers();
    BookPageResponse searchBooks(String query, Long categoryId, Long publisherId, BigDecimal minPrice, BigDecimal maxPrice, String sortBy, int page, int size);
    BookDetailResponse getBookById(Long id);
    List<BookSummaryResponse> getRelatedBooks(Long bookId, int limit);
}
