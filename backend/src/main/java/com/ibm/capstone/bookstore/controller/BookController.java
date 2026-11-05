package com.ibm.capstone.bookstore.controller;

import com.ibm.capstone.bookstore.domain.dto.BookDetailResponse;
import com.ibm.capstone.bookstore.domain.dto.BookPageResponse;
import com.ibm.capstone.bookstore.domain.dto.BookSummaryResponse;
import com.ibm.capstone.bookstore.domain.dto.CategoryResponse;
import com.ibm.capstone.bookstore.domain.dto.PublisherResponse;
import com.ibm.capstone.bookstore.domain.dto.StoreDto;
import com.ibm.capstone.bookstore.security.UserPrincipal;
import com.ibm.capstone.bookstore.service.BookService;
import com.ibm.capstone.bookstore.service.EngagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@Tag(name = "Catalog & Books", description = "Browse categories, publishers/brands, search and filter books")
public class BookController {

    private final BookService bookService;
    private final EngagementService engagementService;

    public BookController(BookService bookService, EngagementService engagementService) {
        this.bookService = bookService;
        this.engagementService = engagementService;
    }

    @GetMapping("/categories")
    @Operation(summary = "List all book categories")
    public ResponseEntity<List<CategoryResponse>> getCategories() {
        return ResponseEntity.ok(bookService.getAllCategories());
    }

    @GetMapping("/publishers")
    @Operation(summary = "List all publishers / brands")
    public ResponseEntity<List<PublisherResponse>> getPublishers() {
        return ResponseEntity.ok(bookService.getAllPublishers());
    }

    @GetMapping("/books/facets")
    @Operation(summary = "Languages and formats available for filtering")
    public ResponseEntity<StoreDto.FacetsResponse> getFacets() {
        return ResponseEntity.ok(bookService.getFacets());
    }

    @GetMapping("/books/shelves")
    @Operation(summary = "Bestsellers, new launches and top rated shelves for the landing page")
    public ResponseEntity<StoreDto.ShelvesResponse> getShelves(@RequestParam(defaultValue = "4") int limit) {
        return ResponseEntity.ok(bookService.getShelves(limit));
    }

    @GetMapping("/books")
    @Operation(summary = "Search and filter books")
    public ResponseEntity<BookPageResponse> searchBooks(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long publisherId,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String format,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "featured") String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return ResponseEntity.ok(bookService.searchBooks(query, categoryId, publisherId, language, format,
                minPrice, maxPrice, sortBy, page, size));
    }

    @GetMapping("/books/{id}")
    @Operation(summary = "Get detailed book information by ID")
    public ResponseEntity<BookDetailResponse> getBookById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(bookService.getBookById(id, principal != null ? principal.getId() : null));
    }

    @GetMapping("/books/{id}/related")
    @Operation(summary = "Get related books")
    public ResponseEntity<List<BookSummaryResponse>> getRelatedBooks(
            @PathVariable Long id,
            @RequestParam(defaultValue = "4") int limit) {
        return ResponseEntity.ok(bookService.getRelatedBooks(id, limit));
    }

    @PostMapping("/books/{id}/reviews")
    @Operation(summary = "Post a review for a book (one per customer)")
    public ResponseEntity<StoreDto.ReviewResponse> addReview(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody StoreDto.ReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(engagementService.addReview(principal.getId(), id, request));
    }
}
