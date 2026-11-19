package com.ibm.capstone.bookstore.repository;

import com.ibm.capstone.bookstore.domain.entities.StorePolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StorePolicyRepository extends JpaRepository<StorePolicy, Long> {
    Optional<StorePolicy> findByIdAndStoreId(Long id, Long storeId);
}
