package com.ibm.capstone.bookstore.service.impl;

import com.ibm.capstone.bookstore.domain.dto.*;
import com.ibm.capstone.bookstore.domain.entities.Book;
import com.ibm.capstone.bookstore.domain.entities.Category;
import com.ibm.capstone.bookstore.domain.entities.Publisher;
import com.ibm.capstone.bookstore.exception.ResourceNotFoundException;
import com.ibm.capstone.bookstore.repository.BookRepository;
import com.ibm.capstone.bookstore.repository.CategoryRepository;
import com.ibm.capstone.bookstore.repository.PublisherRepository;
import com.ibm.capstone.bookstore.service.BookService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final PublisherRepository publisherRepository;

    public BookServiceImpl(BookRepository bookRepository,
                           CategoryRepository categoryRepository,
                           PublisherRepository publisherRepository) {
        this.bookRepository = bookRepository;
        this.categoryRepository = categoryRepository;
        this.publisherRepository = publisherRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::mapToCategoryResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublisherResponse> getAllPublishers() {
        return publisherRepository.findAll().stream()
                .map(this::mapToPublisherResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public BookPageResponse searchBooks(String query, Long categoryId, Long publisherId,
                                        BigDecimal minPrice, BigDecimal maxPrice,
                                        String sortBy, int page, int size) {
        Sort sort = switch (sortBy != null ? sortBy : "featured") {
            case "price_asc" -> Sort.by("price").ascending();
            case "price_desc" -> Sort.by("price").descending();
            case "rating_desc" -> Sort.by("rating").descending().and(Sort.by("reviewCount").descending());
            case "title_asc" -> Sort.by("title").ascending();
            case "newest" -> Sort.by("publishedYear").descending();
            default -> Sort.by("isFeatured").descending().and(Sort.by("rating").descending());
        };

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), sort);
        Page<Book> bookPage = bookRepository.searchBooks(
                (query != null && !query.trim().isEmpty()) ? query.trim() : null,
                categoryId,
                publisherId,
                minPrice,
                maxPrice,
                pageable
        );

        List<BookSummaryResponse> content = bookPage.getContent().stream()
                .map(this::mapToBookSummary)
                .collect(Collectors.toList());

        return new BookPageResponse(
                content,
                bookPage.getTotalElements(),
                bookPage.getTotalPages(),
                bookPage.getNumber(),
                bookPage.getSize()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public BookDetailResponse getBookById(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + id));
        return mapToBookDetail(book);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookSummaryResponse> getRelatedBooks(Long bookId, int limit) {
        Book current = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + bookId));

        Pageable pageable = PageRequest.of(0, Math.max(1, limit), Sort.by("rating").descending());
        List<Book> related = bookRepository.findByCategoryIdAndIdNot(current.getCategory().getId(), bookId, pageable);

        if (related.isEmpty()) {
            related = bookRepository.findTopRatedBooks(pageable);
        }

        return related.stream()
                .map(this::mapToBookSummary)
                .collect(Collectors.toList());
    }

    public BookSummaryResponse mapToBookSummary(Book book) {
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

    private BookDetailResponse mapToBookDetail(Book book) {
        BookDetailResponse dto = new BookDetailResponse();
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

        dto.setDescription(book.getDescription());
        dto.setPublishedYear(book.getPublishedYear());
        dto.setPageCount(book.getPageCount());
        dto.setTentativeDeliveryDate(LocalDate.now().plusDays(book.getTentativeDeliveryDays()));
        dto.setCategory(mapToCategoryResponse(book.getCategory()));
        dto.setPublisher(mapToPublisherResponse(book.getPublisher()));
        return dto;
    }

    private CategoryResponse mapToCategoryResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.getBookCount()
        );
    }

    private PublisherResponse mapToPublisherResponse(Publisher publisher) {
        return new PublisherResponse(
                publisher.getId(),
                publisher.getName(),
                publisher.getCode(),
                publisher.getWebsite()
        );
    }
}
