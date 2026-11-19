package com.ibm.capstone.bookstore.service;

import com.ibm.capstone.bookstore.domain.dto.BookSummaryResponse;
import com.ibm.capstone.bookstore.domain.dto.LifecycleDto;
import com.ibm.capstone.bookstore.domain.entities.Book;
import com.ibm.capstone.bookstore.exception.ResourceNotFoundException;
import com.ibm.capstone.bookstore.repository.BookRepository;
import com.ibm.capstone.bookstore.repository.OrderItemRepository;
import com.ibm.capstone.bookstore.security.Entitlements;
import com.ibm.capstone.bookstore.service.impl.BookServiceImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Up-sell and cross-sell suggestions.
 *
 * Up-sell: a better alternative to the book being looked at. Same category, higher rated, and not more than
 * {@value #MAX_PRICE_JUMP_PERCENT}% dearer, so it is a believable step up rather than a different price tier.
 *
 * Cross-sell: books that go with it. Titles that other customers bought in the same order come first;
 * if there is too little purchase history, top-rated books from other categories fill the gap.
 */
@Service
public class UpsellService {

    static final int MAX_PRICE_JUMP_PERCENT = 40;

    private final BookRepository bookRepository;
    private final OrderItemRepository orderItemRepository;
    private final BookServiceImpl bookService;

    public UpsellService(BookRepository bookRepository, OrderItemRepository orderItemRepository, BookServiceImpl bookService) {
        this.bookRepository = bookRepository;
        this.orderItemRepository = orderItemRepository;
        this.bookService = bookService;
    }

    @Transactional(readOnly = true)
    public LifecycleDto.UpsellResponse forBook(Long bookId, int limit) {
        List<String> visible = Entitlements.visibleCategories();
        Book book = bookRepository.findById(bookId)
                .filter(b -> visible.contains(b.getCategory().getVisibility()))
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + bookId));
        int size = Math.max(1, Math.min(limit, 12));
        return new LifecycleDto.UpsellResponse(upsell(book, visible, size), crossSell(List.of(book), visible, size));
    }

    /** For the basket: up-sell on the most expensive book in it, cross-sell across everything in it. */
    @Transactional(readOnly = true)
    public LifecycleDto.UpsellResponse forBasket(List<Long> bookIds, int limit) {
        List<String> visible = Entitlements.visibleCategories();
        List<Book> books = bookRepository.findAllById(bookIds).stream()
                .filter(b -> visible.contains(b.getCategory().getVisibility()))
                .collect(Collectors.toList());
        int size = Math.max(1, Math.min(limit, 12));
        if (books.isEmpty()) {
            return new LifecycleDto.UpsellResponse(List.of(), List.of());
        }
        Book anchor = books.stream().max(Comparator.comparing(Book::getPrice)).orElse(books.get(0));
        List<BookSummaryResponse> up = upsell(anchor, visible, size).stream()
                .filter(b -> !bookIds.contains(b.getId())).collect(Collectors.toList());
        return new LifecycleDto.UpsellResponse(up, crossSell(books, visible, size));
    }

    private List<BookSummaryResponse> upsell(Book book, List<String> visible, int size) {
        BigDecimal ceiling = book.getPrice().multiply(BigDecimal.valueOf(100 + MAX_PRICE_JUMP_PERCENT))
                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        return bookRepository.findRelated(book.getCategory().getId(), book.getId(), visible, PageRequest.of(0, 20)).stream()
                .filter(b -> b.getRating().compareTo(book.getRating()) > 0 || b.getPrice().compareTo(book.getPrice()) > 0)
                .filter(b -> b.getPrice().compareTo(ceiling) <= 0)
                .filter(b -> b.getStockQuantity() > 0)
                .sorted(Comparator.comparing(Book::getRating).reversed().thenComparing(Book::getPrice))
                .limit(size)
                .map(bookService::mapToBookSummary)
                .collect(Collectors.toList());
    }

    private List<BookSummaryResponse> crossSell(List<Book> books, List<String> visible, int size) {
        Set<Long> ownIds = books.stream().map(Book::getId).collect(Collectors.toSet());
        Set<Long> ownCategories = books.stream().map(b -> b.getCategory().getId()).collect(Collectors.toSet());

        // 1. Bought together: books that share an order with any of these, most often first.
        List<Long> boughtTogether = orderItemRepository.findBoughtWith(new ArrayList<>(ownIds));
        LinkedHashSet<Long> pickedIds = new LinkedHashSet<>();
        for (Long id : boughtTogether) {
            if (!ownIds.contains(id)) pickedIds.add(id);
        }
        List<Book> picked = new ArrayList<>();
        for (Book candidate : bookRepository.findAllById(pickedIds)) {
            if (visible.contains(candidate.getCategory().getVisibility()) && candidate.getStockQuantity() > 0) {
                picked.add(candidate);
            }
        }
        // keep the "most often together" order that the query returned
        List<Long> order = new ArrayList<>(pickedIds);
        picked.sort(Comparator.comparingInt(b -> order.indexOf(b.getId())));

        List<Book> result = new ArrayList<>(picked);

        // 2. Top up with top-rated books from other categories.
        if (result.size() < size) {
            for (Book b : bookRepository.findTopRatedBooks(visible, PageRequest.of(0, 30))) {
                if (result.size() >= size) break;
                if (ownIds.contains(b.getId()) || ownCategories.contains(b.getCategory().getId())) continue;
                if (b.getStockQuantity() <= 0 || result.contains(b)) continue;
                result.add(b);
            }
        }
        return result.stream().limit(size).map(bookService::mapToBookSummary).collect(Collectors.toList());
    }
}
