package com.ibm.capstone.bookstore;

import com.ibm.capstone.bookstore.domain.dto.BookDetailResponse;
import com.ibm.capstone.bookstore.domain.dto.BookPageResponse;
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
import com.ibm.capstone.bookstore.service.impl.BookServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BookServiceTest {

    @Mock
    private BookRepository bookRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private PublisherRepository publisherRepository;
    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private WishlistRepository wishlistRepository;
    @Mock
    private AuthorFollowRepository authorFollowRepository;

    @InjectMocks
    private BookServiceImpl bookService;

    private Book sampleBook;

    @BeforeEach
    void setUp() {
        Category category = new Category();
        category.setId(1L);
        category.setName("Cloud Architecture");
        category.setSlug("cloud-architecture");

        Publisher publisher = new Publisher();
        publisher.setId(1L);
        publisher.setName("IBM Press");
        publisher.setCode("IBM_PRESS");

        Author writer = new Author();
        writer.setId(7L);
        writer.setName("Roland Huemer");
        writer.setBio("Cloud architect.");

        sampleBook = new Book();
        sampleBook.setId(1L);
        sampleBook.setTitle("Enterprise Cloud Native Architecture");
        sampleBook.setAuthor("Roland Huemer");
        sampleBook.setWriter(writer);
        sampleBook.setIsbn("978-0134853444");
        sampleBook.setCategory(category);
        sampleBook.setPublisher(publisher);
        sampleBook.setPrice(BigDecimal.valueOf(899));
        sampleBook.setOriginalPrice(BigDecimal.valueOf(1099));
        sampleBook.setStockQuantity(40);
        sampleBook.setRating(BigDecimal.valueOf(4.9));
        sampleBook.setReviewCount(120);
        sampleBook.setDescription("Enterprise guide to building resilient cloud systems.");
        sampleBook.setPublishedYear(2024);
        sampleBook.setLaunchDate(LocalDate.now().minusDays(30));
        sampleBook.setPageCount(480);
        sampleBook.setLanguage("English");
        sampleBook.setFormat("Paperback");
        sampleBook.setTentativeDeliveryDays(2);
        sampleBook.setIsFeatured(true);
    }

    @Test
    @DisplayName("Should retrieve book by ID with delivery date, writer and wishlist state")
    void testGetBookById_Success() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(sampleBook));
        when(reviewRepository.findByBookIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        when(authorFollowRepository.findByUserIdAndAuthorId(5L, 7L)).thenReturn(Optional.empty());
        when(wishlistRepository.findByUserIdAndBookId(5L, 1L)).thenReturn(Optional.empty());

        BookDetailResponse result = bookService.getBookById(1L, 5L);

        assertEquals("Enterprise Cloud Native Architecture", result.getTitle());
        assertEquals("Cloud Architecture", result.getCategoryName());
        assertEquals("IBM Press", result.getPublisherName());
        assertEquals(LocalDate.now().plusDays(2), result.getTentativeDeliveryDate());
        assertEquals("Roland Huemer", result.getWriter().name());
        assertFalse(result.getWriter().following());
        assertFalse(result.isInWishlist());
    }

    @Test
    @DisplayName("Anonymous visitors can read a book without wishlist or follow lookups")
    void testGetBookById_Anonymous() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(sampleBook));
        when(reviewRepository.findByBookIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        BookDetailResponse result = bookService.getBookById(1L, null);

        assertFalse(result.isInWishlist());
        verifyNoInteractions(wishlistRepository, authorFollowRepository);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when book does not exist")
    void testGetBookById_NotFound() {
        when(bookRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> bookService.getBookById(999L, null));
    }

    @Test
    @DisplayName("Should search books with filters and pagination")
    @SuppressWarnings("unchecked")
    void testSearchBooks_Success() {
        when(bookRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleBook)));

        BookPageResponse page = bookService.searchBooks("Cloud", null, null, "English", "Paperback",
                null, null, "featured", 0, 10);

        assertEquals(1, page.getTotalElements());
        assertEquals("Enterprise Cloud Native Architecture", page.getContent().get(0).getTitle());
        assertEquals("English", page.getContent().get(0).getLanguage());
    }
}
