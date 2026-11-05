package com.ibm.capstone.bookstore;

import com.ibm.capstone.bookstore.domain.dto.BookDetailResponse;
import com.ibm.capstone.bookstore.domain.dto.BookPageResponse;
import com.ibm.capstone.bookstore.domain.entities.Book;
import com.ibm.capstone.bookstore.domain.entities.Category;
import com.ibm.capstone.bookstore.domain.entities.Publisher;
import com.ibm.capstone.bookstore.exception.ResourceNotFoundException;
import com.ibm.capstone.bookstore.repository.BookRepository;
import com.ibm.capstone.bookstore.repository.CategoryRepository;
import com.ibm.capstone.bookstore.repository.PublisherRepository;
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

import java.math.BigDecimal;
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

    @InjectMocks
    private BookServiceImpl bookService;

    private Book sampleBook;
    private Category sampleCategory;
    private Publisher samplePublisher;

    @BeforeEach
    void setUp() {
        sampleCategory = new Category();
        sampleCategory.setId(1L);
        sampleCategory.setName("Cloud Architecture");
        sampleCategory.setSlug("cloud-architecture");

        samplePublisher = new Publisher();
        samplePublisher.setId(1L);
        samplePublisher.setName("IBM Press");
        samplePublisher.setCode("IBM_PRESS");

        sampleBook = new Book();
        sampleBook.setId(1L);
        sampleBook.setTitle("Enterprise Cloud Native Architecture");
        sampleBook.setAuthor("Dr. Roland Huemer");
        sampleBook.setIsbn("978-0134853444");
        sampleBook.setCategory(sampleCategory);
        sampleBook.setPublisher(samplePublisher);
        sampleBook.setPrice(BigDecimal.valueOf(54.99));
        sampleBook.setOriginalPrice(BigDecimal.valueOf(64.99));
        sampleBook.setStockQuantity(40);
        sampleBook.setRating(BigDecimal.valueOf(4.9));
        sampleBook.setReviewCount(120);
        sampleBook.setCoverImageUrl("https://example.com/cover.jpg");
        sampleBook.setDescription("Enterprise guide to building resilient cloud systems.");
        sampleBook.setPublishedYear(2024);
        sampleBook.setPageCount(480);
        sampleBook.setTentativeDeliveryDays(2);
        sampleBook.setIsFeatured(true);
    }

    @Test
    @DisplayName("Should retrieve book by ID with calculated tentative delivery date")
    void testGetBookById_Success() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(sampleBook));

        BookDetailResponse result = bookService.getBookById(1L);

        assertNotNull(result);
        assertEquals("Enterprise Cloud Native Architecture", result.getTitle());
        assertEquals("Cloud Architecture", result.getCategoryName());
        assertEquals("IBM Press", result.getPublisherName());
        assertNotNull(result.getTentativeDeliveryDate());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when book does not exist")
    void testGetBookById_NotFound() {
        when(bookRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> bookService.getBookById(999L));
    }

    @Test
    @DisplayName("Should search books with query and pagination")
    void testSearchBooks_Success() {
        when(bookRepository.searchBooks(eq("Cloud"), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleBook)));

        BookPageResponse pageResponse = bookService.searchBooks("Cloud", null, null, null, null, "featured", 0, 10);

        assertNotNull(pageResponse);
        assertEquals(1, pageResponse.getTotalElements());
        assertEquals(1, pageResponse.getContent().size());
        assertEquals("Enterprise Cloud Native Architecture", pageResponse.getContent().get(0).getTitle());
    }

    @Test
    @DisplayName("Verify BCrypt Password Encoding")
    void testBCryptEncoder() {
        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        String hash = encoder.encode("Password123!");
        System.out.println("BCRYPT_HASH_OUTPUT=" + hash);
        assertTrue(encoder.matches("Password123!", hash));
    }
}
