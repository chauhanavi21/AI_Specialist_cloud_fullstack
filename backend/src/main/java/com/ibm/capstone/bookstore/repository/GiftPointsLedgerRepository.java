package com.ibm.capstone.bookstore.repository;

import com.ibm.capstone.bookstore.domain.entities.GiftPointsLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GiftPointsLedgerRepository extends JpaRepository<GiftPointsLedger, Long> {
    List<GiftPointsLedger> findByUserIdOrderByCreatedAtDesc(Long userId);
}
