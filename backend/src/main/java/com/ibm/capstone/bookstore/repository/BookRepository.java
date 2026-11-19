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

    @Query("SELECT b FROM Book b WHERE b.category.id = :categoryId AND b.id <> :id AND b.category.visibility IN :visibility ORDER BY b.rating DESC")
    List<Book> findRelated(@Param("categoryId") Long categoryId, @Param("id") Long id,
                           @Param("visibility") List<String> visibility, Pageable pageable);

    List<Book> findByWriterId(Long authorId);

    @Query("SELECT b FROM Book b WHERE b.writer.id = :authorId AND b.category.visibility IN :visibility")
    List<Book> findByWriterIdVisible(@Param("authorId") Long authorId, @Param("visibility") List<String> visibility);

    List<Book> findByIsFeaturedTrue();

    @Query("SELECT b FROM Book b WHERE b.category.id IN :categoryIds AND b.id NOT IN :excludeIds AND b.category.visibility IN :visibility ORDER BY b.rating DESC")
    List<Book> findRecommendationsByCategories(
            @Param("categoryIds") List<Long> categoryIds,
            @Param("excludeIds") List<Long> excludeIds,
            @Param("visibility") List<String> visibility,
            Pageable pageable
    );

    @Query("SELECT b FROM Book b WHERE b.category.visibility IN :visibility ORDER BY b.rating DESC, b.reviewCount DESC")
    List<Book> findTopRatedBooks(@Param("visibility") List<String> visibility, Pageable pageable);

    @Query("SELECT DISTINCT b.language FROM Book b WHERE b.category.visibility IN :visibility ORDER BY b.language")
    List<String> findDistinctLanguages(@Param("visibility") List<String> visibility);

    @Query("SELECT DISTINCT b.format FROM Book b WHERE b.category.visibility IN :visibility ORDER BY b.format")
    List<String> findDistinctFormats(@Param("visibility") List<String> visibility);
}
