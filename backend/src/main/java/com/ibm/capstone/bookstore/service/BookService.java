package com.ibm.capstone.bookstore.service;

import com.ibm.capstone.bookstore.domain.dto.BookDetailResponse;
import com.ibm.capstone.bookstore.domain.dto.BookPageResponse;
import com.ibm.capstone.bookstore.domain.dto.BookSummaryResponse;
import com.ibm.capstone.bookstore.domain.dto.CategoryResponse;
import com.ibm.capstone.bookstore.domain.dto.PublisherResponse;
import com.ibm.capstone.bookstore.domain.dto.StoreDto;

import java.math.BigDecimal;
import java.util.List;

public interface BookService {
    List<CategoryResponse> getAllCategories();
    List<PublisherResponse> getAllPublishers();
    StoreDto.FacetsResponse getFacets();
    BookPageResponse searchBooks(String query, Long categoryId, Long publisherId, String language, String format,
                                 BigDecimal minPrice, BigDecimal maxPrice, String sortBy, int page, int size);
    StoreDto.ShelvesResponse getShelves(int limit);
    /** {@code userId} may be null for anonymous visitors; it only drives the wishlist and follow flags. */
    BookDetailResponse getBookById(Long id, Long userId);
    List<BookSummaryResponse> getRelatedBooks(Long bookId, int limit);
}
