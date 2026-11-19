package com.ibm.capstone.bookstore.repository;

import com.ibm.capstone.bookstore.domain.entities.OrderReturn;
import com.ibm.capstone.bookstore.domain.entities.ReturnStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderReturnRepository extends JpaRepository<OrderReturn, Long> {

    List<OrderReturn> findByOrderUserIdOrderByRequestedAtDesc(Long userId);

    Optional<OrderReturn> findByIdAndOrderUserId(Long id, Long userId);

    List<OrderReturn> findByOrderId(Long orderId);

    List<OrderReturn> findByStatusInOrderByRequestedAtAsc(Collection<ReturnStatus> statuses);

    List<OrderReturn> findAllByOrderByRequestedAtDesc();
}
