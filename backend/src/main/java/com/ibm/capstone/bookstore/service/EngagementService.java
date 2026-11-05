package com.ibm.capstone.bookstore.service;

import com.ibm.capstone.bookstore.domain.dto.BookSummaryResponse;
import com.ibm.capstone.bookstore.domain.dto.StoreDto;
import com.ibm.capstone.bookstore.domain.entities.Author;
import com.ibm.capstone.bookstore.domain.entities.AuthorFollow;
import com.ibm.capstone.bookstore.domain.entities.Book;
import com.ibm.capstone.bookstore.domain.entities.Review;
import com.ibm.capstone.bookstore.domain.entities.User;
import com.ibm.capstone.bookstore.domain.entities.WishlistItem;
import com.ibm.capstone.bookstore.exception.BadRequestException;
import com.ibm.capstone.bookstore.exception.ResourceNotFoundException;
import com.ibm.capstone.bookstore.repository.AuthorFollowRepository;
import com.ibm.capstone.bookstore.repository.AuthorRepository;
import com.ibm.capstone.bookstore.repository.BookRepository;
import com.ibm.capstone.bookstore.repository.ReviewRepository;
import com.ibm.capstone.bookstore.repository.UserRepository;
import com.ibm.capstone.bookstore.repository.WishlistRepository;
import com.ibm.capstone.bookstore.service.impl.BookServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Customer engagement: wishlist, followed writers ("My Writers") and book reviews. */
@Service
public class EngagementService {

    private final WishlistRepository wishlistRepository;
    private final AuthorFollowRepository followRepository;
    private final AuthorRepository authorRepository;
    private final ReviewRepository reviewRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final BookServiceImpl bookService;

    public EngagementService(WishlistRepository wishlistRepository,
                             AuthorFollowRepository followRepository,
                             AuthorRepository authorRepository,
                             ReviewRepository reviewRepository,
                             BookRepository bookRepository,
                             UserRepository userRepository,
                             BookServiceImpl bookService) {
        this.wishlistRepository = wishlistRepository;
        this.followRepository = followRepository;
        this.authorRepository = authorRepository;
        this.reviewRepository = reviewRepository;
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
        this.bookService = bookService;
    }

    // ---- Wishlist ----

    @Transactional(readOnly = true)
    public List<BookSummaryResponse> getWishlist(Long userId) {
        return wishlistRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(w -> bookService.mapToBookSummary(w.getBook()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void addToWishlist(Long userId, Long bookId) {
        if (wishlistRepository.findByUserIdAndBookId(userId, bookId).isPresent()) return;
        wishlistRepository.save(new WishlistItem(loadUser(userId), loadBook(bookId)));
    }

    @Transactional
    public void removeFromWishlist(Long userId, Long bookId) {
        wishlistRepository.findByUserIdAndBookId(userId, bookId).ifPresent(wishlistRepository::delete);
    }

    // ---- Writers ----

    /** Every writer, with the caller's follow flag when signed in. */
    @Transactional(readOnly = true)
    public List<StoreDto.AuthorSummary> getWriters(Long userId, boolean followedOnly) {
        Set<Long> followed = userId == null ? Set.of() : Set.copyOf(followRepository.findAuthorIdsByUserId(userId));
        return authorRepository.findAllByOrderByNameAsc().stream()
                .filter(a -> !followedOnly || followed.contains(a.getId()))
                .map(a -> new StoreDto.AuthorSummary(a.getId(), a.getName(), a.getPhotoUrl(),
                        bookRepository.findByWriterId(a.getId()).size(), followed.contains(a.getId())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StoreDto.AuthorDetail getWriter(Long authorId, Long userId) {
        Author author = loadAuthor(authorId);
        boolean following = userId != null && followRepository.findByUserIdAndAuthorId(userId, authorId).isPresent();
        List<BookSummaryResponse> books = bookRepository.findByWriterId(authorId).stream()
                .map(bookService::mapToBookSummary)
                .collect(Collectors.toList());
        return new StoreDto.AuthorDetail(author.getId(), author.getName(), author.getBio(), author.getPhotoUrl(), following, books);
    }

    @Transactional
    public void follow(Long userId, Long authorId) {
        if (followRepository.findByUserIdAndAuthorId(userId, authorId).isPresent()) return;
        followRepository.save(new AuthorFollow(loadUser(userId), loadAuthor(authorId)));
    }

    @Transactional
    public void unfollow(Long userId, Long authorId) {
        followRepository.findByUserIdAndAuthorId(userId, authorId).ifPresent(followRepository::delete);
    }

    // ---- Reviews ----

    @Transactional
    public StoreDto.ReviewResponse addReview(Long userId, Long bookId, StoreDto.ReviewRequest request) {
        if (reviewRepository.existsByBookIdAndUserId(bookId, userId)) {
            throw new BadRequestException("You have already reviewed this book");
        }
        Book book = loadBook(bookId);
        User user = loadUser(userId);

        Review review = new Review();
        review.setBook(book);
        review.setUser(user);
        review.setRating(request.rating());
        review.setComment(request.comment().trim());
        Review saved = reviewRepository.save(review);

        // Fold the new rating into the running average shown on cards.
        int count = book.getReviewCount();
        BigDecimal total = book.getRating().multiply(BigDecimal.valueOf(count)).add(BigDecimal.valueOf(request.rating()));
        book.setReviewCount(count + 1);
        book.setRating(total.divide(BigDecimal.valueOf(count + 1L), 2, RoundingMode.HALF_UP));
        bookRepository.save(book);

        return new StoreDto.ReviewResponse(saved.getId(), user.getFullName(), saved.getRating(), saved.getComment(), saved.getCreatedAt());
    }

    private User loadUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    private Book loadBook(Long id) {
        return bookRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + id));
    }

    private Author loadAuthor(Long id) {
        return authorRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Writer not found with id: " + id));
    }
}
