package com.ibm.capstone.bookstore.repository;

import com.ibm.capstone.bookstore.domain.entities.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Order> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT DISTINCT oi.book.category.id FROM Order o JOIN o.items oi WHERE o.user.id = :userId")
    List<Long> findPurchasedCategoryIdsByUserId(@Param("userId") Long userId);

    @Query("SELECT DISTINCT oi.book.id FROM Order o JOIN o.items oi WHERE o.user.id = :userId")
    List<Long> findPurchasedBookIdsByUserId(@Param("userId") Long userId);
}
