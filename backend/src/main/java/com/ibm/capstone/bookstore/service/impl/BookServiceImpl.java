package com.ibm.capstone.bookstore.service.impl;

import com.ibm.capstone.bookstore.domain.dto.*;
import com.ibm.capstone.bookstore.domain.entities.Author;
import com.ibm.capstone.bookstore.domain.entities.Book;
import com.ibm.capstone.bookstore.domain.entities.Category;
import com.ibm.capstone.bookstore.domain.entities.Publisher;
import com.ibm.capstone.bookstore.exception.ResourceNotFoundException;
import com.ibm.capstone.bookstore.repository.AuthorFollowRepository;
import com.ibm.capstone.bookstore.repository.BookRepository;
import com.ibm.capstone.bookstore.repository.CategoryRepository;
import com.ibm.capstone.bookstore.repository.PublisherRepository;
import com.ibm.capstone.bookstore.repository.ReviewRepository;
import com.ibm.capstone.bookstore.repository.WishlistRepository;
import com.ibm.capstone.bookstore.security.Entitlements;
import com.ibm.capstone.bookstore.service.BookService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final PublisherRepository publisherRepository;
    private final ReviewRepository reviewRepository;
    private final WishlistRepository wishlistRepository;
    private final AuthorFollowRepository authorFollowRepository;

    public BookServiceImpl(BookRepository bookRepository,
                           CategoryRepository categoryRepository,
                           PublisherRepository publisherRepository,
                           ReviewRepository reviewRepository,
                           WishlistRepository wishlistRepository,
                           AuthorFollowRepository authorFollowRepository) {
        this.bookRepository = bookRepository;
        this.categoryRepository = categoryRepository;
        this.publisherRepository = publisherRepository;
        this.reviewRepository = reviewRepository;
        this.wishlistRepository = wishlistRepository;
        this.authorFollowRepository = authorFollowRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        List<String> visible = Entitlements.visibleCategories();
        return categoryRepository.findAll().stream()
                .filter(c -> visible.contains(c.getVisibility()))
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
    public StoreDto.FacetsResponse getFacets() {
        List<String> visible = Entitlements.visibleCategories();
        return new StoreDto.FacetsResponse(bookRepository.findDistinctLanguages(visible), bookRepository.findDistinctFormats(visible));
    }

    @Override
    @Transactional(readOnly = true)
    public BookPageResponse searchBooks(String query, Long categoryId, Long publisherId,
                                        String language, String format,
                                        BigDecimal minPrice, BigDecimal maxPrice,
                                        String sortBy, int page, int size) {
        Sort sort = switch (sortBy != null ? sortBy : "featured") {
            case "price_asc" -> Sort.by("price").ascending();
            case "price_desc" -> Sort.by("price").descending();
            case "rating_desc" -> Sort.by("rating").descending().and(Sort.by("reviewCount").descending());
            case "title_asc" -> Sort.by("title").ascending();
            case "newest" -> Sort.by("launchDate").descending();
            case "bestselling" -> Sort.by("copiesSold").descending();
            default -> Sort.by("isFeatured").descending().and(Sort.by("rating").descending());
        };

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), sort);
        Page<Book> bookPage = bookRepository.findAll(
                filters(query, categoryId, publisherId, language, format, minPrice, maxPrice), pageable);

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

    /** Builds a predicate only for the filters that were actually supplied. */
    private Specification<Book> filters(String query, Long categoryId, Long publisherId,
                                        String language, String format,
                                        BigDecimal minPrice, BigDecimal maxPrice) {
        List<String> visible = Entitlements.visibleCategories();
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            // Entitlement: guests never see books in members-only categories
            predicates.add(root.get("category").get("visibility").in(visible));
            if (query != null && !query.isBlank()) {
                String like = "%" + query.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(root.get("author")), like),
                        cb.like(cb.lower(root.get("description")), like)));
            }
            if (categoryId != null) predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            if (publisherId != null) predicates.add(cb.equal(root.get("publisher").get("id"), publisherId));
            if (language != null && !language.isBlank()) predicates.add(cb.equal(root.get("language"), language));
            if (format != null && !format.isBlank()) predicates.add(cb.equal(root.get("format"), format));
            if (minPrice != null) predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            if (maxPrice != null) predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Override
    @Transactional(readOnly = true)
    public StoreDto.ShelvesResponse getShelves(int limit) {
        int size = Math.max(1, limit);
        return new StoreDto.ShelvesResponse(
                shelf(Sort.by("copiesSold").descending(), size),
                shelf(Sort.by("launchDate").descending(), size),
                shelf(Sort.by("rating").descending().and(Sort.by("reviewCount").descending()), size));
    }

    private List<BookSummaryResponse> shelf(Sort sort, int size) {
        List<String> visible = Entitlements.visibleCategories();
        Specification<Book> spec = (root, cq, cb) -> root.get("category").get("visibility").in(visible);
        return bookRepository.findAll(spec, PageRequest.of(0, size, sort)).getContent().stream()
                .map(this::mapToBookSummary)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public BookDetailResponse getBookById(Long id, Long userId) {
        return getBookById(id, userId, false);
    }

    /**
     * @param includeHidden true only for administrators, who may open books in members-only categories
     *                      regardless of who is asking (for example to see the book they have just created).
     */
    @Transactional(readOnly = true)
    public BookDetailResponse getBookById(Long id, Long userId, boolean includeHidden) {
        Book book = bookRepository.findById(id)
                .filter(b -> includeHidden || Entitlements.visibleCategories().contains(b.getCategory().getVisibility()))
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + id));
        return mapToBookDetail(book, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookSummaryResponse> getRelatedBooks(Long bookId, int limit) {
        List<String> visible = Entitlements.visibleCategories();
        Book current = bookRepository.findById(bookId)
                .filter(b -> visible.contains(b.getCategory().getVisibility()))
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + bookId));

        Pageable pageable = PageRequest.of(0, Math.max(1, limit));
        List<Book> related = bookRepository.findRelated(current.getCategory().getId(), bookId, visible, pageable);

        if (related.isEmpty()) {
            related = bookRepository.findTopRatedBooks(visible, pageable).stream()
                    .filter(b -> !b.getId().equals(bookId))
                    .collect(Collectors.toList());
        }

        return related.stream()
                .map(this::mapToBookSummary)
                .collect(Collectors.toList());
    }

    public BookSummaryResponse mapToBookSummary(Book book) {
        BookSummaryResponse dto = new BookSummaryResponse();
        fillSummary(dto, book);
        return dto;
    }

    private void fillSummary(BookSummaryResponse dto, Book book) {
        dto.setId(book.getId());
        dto.setTitle(book.getTitle());
        dto.setAuthor(book.getAuthor());
        dto.setAuthorId(book.getWriter() != null ? book.getWriter().getId() : null);
        dto.setIsbn(book.getIsbn());
        dto.setBlurb(blurb(book.getDescription()));
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
        dto.setTentativeDeliveryDate(LocalDate.now().plusDays(book.getTentativeDeliveryDays()));
        dto.setIsFeatured(book.getIsFeatured());
        dto.setLanguage(book.getLanguage());
        dto.setFormat(book.getFormat());
        dto.setCopiesSold(book.getCopiesSold());
        dto.setLaunchDate(book.getLaunchDate());
    }

    private static String blurb(String description) {
        if (description == null) return "";
        return description.length() <= 120 ? description : description.substring(0, 117).trim() + "...";
    }

    private BookDetailResponse mapToBookDetail(Book book, Long userId) {
        BookDetailResponse dto = new BookDetailResponse();
        fillSummary(dto, book);

        dto.setDescription(book.getDescription());
        dto.setPublishedYear(book.getPublishedYear());
        dto.setPageCount(book.getPageCount());
        dto.setCategory(mapToCategoryResponse(book.getCategory()));
        dto.setPublisher(mapToPublisherResponse(book.getPublisher()));

        Author writer = book.getWriter();
        boolean following = writer != null && userId != null
                && authorFollowRepository.findByUserIdAndAuthorId(userId, writer.getId()).isPresent();
        dto.setWriter(writer != null
                ? new StoreDto.WriterProfile(writer.getId(), writer.getName(), writer.getBio(), writer.getPhotoUrl(), following)
                : new StoreDto.WriterProfile(null, book.getAuthor(), "", null, false));

        dto.setReviews(reviewRepository.findByBookIdOrderByCreatedAtDesc(book.getId()).stream()
                .map(r -> new StoreDto.ReviewResponse(r.getId(), r.getUser().getFullName(), r.getRating(), r.getComment(), r.getCreatedAt()))
                .collect(Collectors.toList()));

        dto.setInWishlist(userId != null && wishlistRepository.findByUserIdAndBookId(userId, book.getId()).isPresent());
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
