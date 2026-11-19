package com.ibm.capstone.bookstore.repository;

import com.ibm.capstone.bookstore.domain.entities.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrderId(Long orderId);

    /**
     * Ids of books that appear in the same order as any of the given books, most frequent first.
     * Cancelled orders are ignored so that purchases that did not happen do not shape the suggestions.
     */
    @Query("SELECT other.book.id FROM OrderItem mine JOIN OrderItem other ON other.order.id = mine.order.id " +
           "WHERE mine.book.id IN :bookIds AND other.book.id NOT IN :bookIds " +
           "AND mine.order.orderStatus <> com.ibm.capstone.bookstore.domain.entities.OrderStatus.CANCELLED " +
           "GROUP BY other.book.id ORDER BY COUNT(other.id) DESC")
    List<Long> findBoughtWith(@Param("bookIds") List<Long> bookIds);
}
