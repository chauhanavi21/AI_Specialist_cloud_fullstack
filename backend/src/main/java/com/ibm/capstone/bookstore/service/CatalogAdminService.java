package com.ibm.capstone.bookstore.service;

import com.ibm.capstone.bookstore.domain.dto.AdminDto;
import com.ibm.capstone.bookstore.domain.dto.BookDetailResponse;
import com.ibm.capstone.bookstore.domain.dto.CategoryResponse;
import com.ibm.capstone.bookstore.domain.entities.Author;
import com.ibm.capstone.bookstore.domain.entities.Book;
import com.ibm.capstone.bookstore.domain.entities.Category;
import com.ibm.capstone.bookstore.domain.entities.Publisher;
import com.ibm.capstone.bookstore.exception.BadRequestException;
import com.ibm.capstone.bookstore.exception.ResourceNotFoundException;
import com.ibm.capstone.bookstore.repository.AuthorRepository;
import com.ibm.capstone.bookstore.repository.BookRepository;
import com.ibm.capstone.bookstore.repository.CartItemRepository;
import com.ibm.capstone.bookstore.repository.CategoryRepository;
import com.ibm.capstone.bookstore.repository.OrderItemRepository;
import com.ibm.capstone.bookstore.repository.PublisherRepository;
import com.ibm.capstone.bookstore.service.impl.BookServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.Locale;

/**
 * Creating and maintaining the catalogue: categories, books and stock. Administrator-only; the controller
 * enforces the role, this class enforces the data rules (unique ISBNs and slugs, honest category counts).
 */
@Service
public class CatalogAdminService {

    private final CategoryRepository categoryRepository;
    private final PublisherRepository publisherRepository;
    private final AuthorRepository authorRepository;
    private final BookRepository bookRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;
    private final BookServiceImpl bookService;

    public CatalogAdminService(CategoryRepository categoryRepository, PublisherRepository publisherRepository,
                               AuthorRepository authorRepository, BookRepository bookRepository,
                               OrderItemRepository orderItemRepository, CartItemRepository cartItemRepository,
                               BookServiceImpl bookService) {
        this.categoryRepository = categoryRepository;
        this.publisherRepository = publisherRepository;
        this.authorRepository = authorRepository;
        this.bookRepository = bookRepository;
        this.orderItemRepository = orderItemRepository;
        this.cartItemRepository = cartItemRepository;
        this.bookService = bookService;
    }

    // ---- Categories --------------------------------------------------------------------

    @Transactional
    public CategoryResponse createCategory(AdminDto.CategoryRequest request) {
        String name = request.name().trim();
        if (categoryRepository.findAll().stream().anyMatch(c -> c.getName().equalsIgnoreCase(name))) {
            throw new BadRequestException("A category named '" + name + "' already exists");
        }
        String slug = slugify(name);
        if (slug.isEmpty() || categoryRepository.findBySlug(slug).isPresent()) {
            throw new BadRequestException("Choose a different category name: its web address '" + slug + "' is taken or empty");
        }
        Category category = new Category();
        category.setName(name);
        category.setSlug(slug);
        category.setDescription(request.description() == null ? null : request.description().trim());
        category.setVisibility(request.visibility() == null ? "PUBLIC" : request.visibility());
        category.setBookCount(0);
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, AdminDto.CategoryRequest request) {
        Category category = loadCategory(id);
        String name = request.name().trim();
        if (categoryRepository.findAll().stream().anyMatch(c -> !c.getId().equals(id) && c.getName().equalsIgnoreCase(name))) {
            throw new BadRequestException("A category named '" + name + "' already exists");
        }
        category.setName(name);
        category.setDescription(request.description() == null ? null : request.description().trim());
        if (request.visibility() != null) {
            category.setVisibility(request.visibility());
        }
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = loadCategory(id);
        if (!bookRepository.findAll().stream().noneMatch(b -> b.getCategory().getId().equals(id))) {
            throw new BadRequestException("This category still has books. Move or remove them first.");
        }
        categoryRepository.delete(category);
    }

    // ---- Books -------------------------------------------------------------------------

    @Transactional
    public BookDetailResponse createBook(AdminDto.BookRequest request) {
        String isbn = request.isbn().trim();
        if (bookRepository.findAll().stream().anyMatch(b -> b.getIsbn().equalsIgnoreCase(isbn))) {
            throw new BadRequestException("A book with ISBN " + isbn + " already exists");
        }
        Book book = new Book();
        applyBook(book, request);
        book.setLaunchDate(LocalDate.now());
        book.setRating(new BigDecimal("4.50"));
        book.setReviewCount(0);
        book.setCopiesSold(0);
        Book saved = bookRepository.save(book);
        recount(saved.getCategory());
        return bookService.getBookById(saved.getId(), null, true);
    }

    @Transactional
    public BookDetailResponse updateBook(Long id, AdminDto.BookRequest request) {
        Book book = loadBook(id);
        String isbn = request.isbn().trim();
        if (bookRepository.findAll().stream().anyMatch(b -> !b.getId().equals(id) && b.getIsbn().equalsIgnoreCase(isbn))) {
            throw new BadRequestException("Another book already uses ISBN " + isbn);
        }
        Category before = book.getCategory();
        applyBook(book, request);
        Book saved = bookRepository.save(book);
        recount(saved.getCategory());
        if (!before.getId().equals(saved.getCategory().getId())) {
            recount(before);
        }
        return bookService.getBookById(saved.getId(), null, true);
    }

    @Transactional
    public BookDetailResponse setStock(Long id, int stock) {
        Book book = loadBook(id);
        book.setStockQuantity(stock);
        return bookService.getBookById(bookRepository.save(book).getId(), null, true);
    }

    /** A book that was ever bought stays in the database so past orders keep working; it is simply taken off sale. */
    @Transactional
    public void deleteBook(Long id) {
        Book book = loadBook(id);
        if (orderItemRepository.findAll().stream().anyMatch(i -> i.getBook() != null && i.getBook().getId().equals(id))) {
            throw new BadRequestException("This book appears on past orders, so it cannot be deleted. Set its stock to 0 to take it off sale.");
        }
        cartItemRepository.findAll().stream()
                .filter(i -> i.getBook().getId().equals(id))
                .forEach(cartItemRepository::delete);
        Category category = book.getCategory();
        bookRepository.delete(book);
        bookRepository.flush();
        recount(category);
    }

    // ---- Helpers -----------------------------------------------------------------------

    private void applyBook(Book book, AdminDto.BookRequest r) {
        Category category = loadCategory(r.categoryId());
        Publisher publisher = publisherRepository.findById(r.publisherId())
                .orElseThrow(() -> new ResourceNotFoundException("Publisher not found: " + r.publisherId()));
        Author writer = r.authorId() == null ? null : authorRepository.findById(r.authorId())
                .orElseThrow(() -> new ResourceNotFoundException("Writer not found: " + r.authorId()));

        BigDecimal original = r.originalPrice() == null ? r.price() : r.originalPrice();
        if (original.compareTo(r.price()) < 0) {
            throw new BadRequestException("The original price cannot be lower than the selling price");
        }
        book.setTitle(r.title().trim());
        book.setAuthor(r.author().trim());
        book.setWriter(writer);
        book.setIsbn(r.isbn().trim());
        book.setCategory(category);
        book.setPublisher(publisher);
        book.setPrice(r.price());
        book.setOriginalPrice(original);
        book.setStockQuantity(r.stockQuantity());
        book.setDescription(r.description().trim());
        book.setPublishedYear(r.publishedYear());
        book.setPageCount(r.pageCount());
        book.setLanguage(r.language() == null || r.language().isBlank() ? "English" : r.language().trim());
        book.setFormat(r.format() == null || r.format().isBlank() ? "Paperback" : r.format().trim());
        book.setTentativeDeliveryDays(r.tentativeDeliveryDays() == null ? 2 : r.tentativeDeliveryDays());
        book.setIsFeatured(Boolean.TRUE.equals(r.featured()));
    }

    /** Keeps the stored count honest by counting the books rather than adding and subtracting. */
    private void recount(Category category) {
        long count = bookRepository.findAll().stream().filter(b -> b.getCategory().getId().equals(category.getId())).count();
        category.setBookCount((int) count);
        categoryRepository.save(category);
    }

    private Category loadCategory(Long id) {
        return categoryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
    }

    private Book loadBook(Long id) {
        return bookRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + id));
    }

    private CategoryResponse toResponse(Category c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getSlug(), c.getDescription(), c.getBookCount());
    }

    static String slugify(String name) {
        String ascii = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return ascii.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
    }
}
