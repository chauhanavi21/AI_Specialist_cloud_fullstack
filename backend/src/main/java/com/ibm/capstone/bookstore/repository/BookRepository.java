package com.ibm.capstone.bookstore.repository;

import com.ibm.capstone.bookstore.domain.entities.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    @Query("SELECT b FROM Book b WHERE " +
           "(:query IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "  OR LOWER(b.author) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "  OR LOWER(b.description) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND (:categoryId IS NULL OR b.category.id = :categoryId) " +
           "AND (:publisherId IS NULL OR b.publisher.id = :publisherId) " +
           "AND (:minPrice IS NULL OR b.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR b.price <= :maxPrice)")
    Page<Book> searchBooks(
            @Param("query") String query,
            @Param("categoryId") Long categoryId,
            @Param("publisherId") Long publisherId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable
    );

    List<Book> findByCategoryIdAndIdNot(Long categoryId, Long id, Pageable pageable);

    List<Book> findByIsFeaturedTrue();

    @Query("SELECT b FROM Book b WHERE b.category.id IN :categoryIds AND b.id NOT IN :excludeIds ORDER BY b.rating DESC")
    List<Book> findRecommendationsByCategories(
            @Param("categoryIds") List<Long> categoryIds,
            @Param("excludeIds") List<Long> excludeIds,
            Pageable pageable
    );

    @Query("SELECT b FROM Book b ORDER BY b.rating DESC, b.reviewCount DESC")
    List<Book> findTopRatedBooks(Pageable pageable);
}
