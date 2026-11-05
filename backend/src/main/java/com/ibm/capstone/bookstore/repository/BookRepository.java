package com.ibm.capstone.bookstore.repository;

import com.ibm.capstone.bookstore.domain.entities.Book;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Catalog search is done with {@link JpaSpecificationExecutor} so that only the filters
 * actually supplied become SQL predicates. That avoids the {@code :param IS NULL} pattern,
 * which fails on PostgreSQL ("could not determine data type of parameter").
 */
@Repository
public interface BookRepository extends JpaRepository<Book, Long>, JpaSpecificationExecutor<Book> {

    List<Book> findByCategoryIdAndIdNot(Long categoryId, Long id, Pageable pageable);

    List<Book> findByWriterId(Long authorId);

    List<Book> findByIsFeaturedTrue();

    @Query("SELECT b FROM Book b WHERE b.category.id IN :categoryIds AND b.id NOT IN :excludeIds ORDER BY b.rating DESC")
    List<Book> findRecommendationsByCategories(
            @Param("categoryIds") List<Long> categoryIds,
            @Param("excludeIds") List<Long> excludeIds,
            Pageable pageable
    );

    @Query("SELECT b FROM Book b ORDER BY b.rating DESC, b.reviewCount DESC")
    List<Book> findTopRatedBooks(Pageable pageable);

    @Query("SELECT DISTINCT b.language FROM Book b ORDER BY b.language")
    List<String> findDistinctLanguages();

    @Query("SELECT DISTINCT b.format FROM Book b ORDER BY b.format")
    List<String> findDistinctFormats();
}
