package com.ibm.capstone.bookstore.repository;

import com.ibm.capstone.bookstore.domain.entities.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByCartIdAndBookId(Long cartId, Long bookId);
    Optional<CartItem> findByIdAndCartUserId(Long id, Long userId);
}
