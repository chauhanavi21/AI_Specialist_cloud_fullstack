package com.ibm.capstone.bookstore.repository;

import com.ibm.capstone.bookstore.domain.entities.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByBookIdOrderByCreatedAtDesc(Long bookId);

    boolean existsByBookIdAndUserId(Long bookId, Long userId);
}
