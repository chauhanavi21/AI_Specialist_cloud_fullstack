package com.ibm.capstone.bookstore.controller;

import com.ibm.capstone.bookstore.domain.dto.BookDetailResponse;
import com.ibm.capstone.bookstore.domain.dto.BookPageResponse;
import com.ibm.capstone.bookstore.domain.dto.BookSummaryResponse;
import com.ibm.capstone.bookstore.domain.dto.CategoryResponse;
import com.ibm.capstone.bookstore.domain.dto.PublisherResponse;
import com.ibm.capstone.bookstore.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@Tag(name = "Catalog & Books", description = "Browse categories, publishers/brands, search and filter books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
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

    @GetMapping("/books")
    @Operation(summary = "Search and filter books")
    public ResponseEntity<BookPageResponse> searchBooks(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long publisherId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "featured") String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return ResponseEntity.ok(bookService.searchBooks(query, categoryId, publisherId, minPrice, maxPrice, sortBy, page, size));
    }

    @GetMapping("/books/{id}")
    @Operation(summary = "Get detailed book information by ID")
    public ResponseEntity<BookDetailResponse> getBookById(@PathVariable Long id) {
        return ResponseEntity.ok(bookService.getBookById(id));
    }

    @GetMapping("/books/{id}/related")
    @Operation(summary = "Get related books")
    public ResponseEntity<List<BookSummaryResponse>> getRelatedBooks(
            @PathVariable Long id,
            @RequestParam(defaultValue = "4") int limit) {
        return ResponseEntity.ok(bookService.getRelatedBooks(id, limit));
    }
}
