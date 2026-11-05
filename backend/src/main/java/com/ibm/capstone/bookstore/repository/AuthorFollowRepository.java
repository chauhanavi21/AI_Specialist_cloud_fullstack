package com.ibm.capstone.bookstore.repository;

import com.ibm.capstone.bookstore.domain.entities.AuthorFollow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuthorFollowRepository extends JpaRepository<AuthorFollow, Long> {
    List<AuthorFollow> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<AuthorFollow> findByUserIdAndAuthorId(Long userId, Long authorId);

    @Query("SELECT f.author.id FROM AuthorFollow f WHERE f.user.id = :userId")
    List<Long> findAuthorIdsByUserId(@Param("userId") Long userId);
}
