package com.ibm.capstone.bookstore.repository;

import com.ibm.capstone.bookstore.domain.entities.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {
    List<Store> findByActiveTrueOrderByIdAsc();

    Optional<Store> findFirstByActiveTrueOrderByIdAsc();

    boolean existsByNameIgnoreCase(String name);
}
