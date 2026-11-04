package com.ibm.capstone.bookstore.repository;

import com.ibm.capstone.bookstore.domain.entities.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByUserIdOrderByIdAsc(Long userId);
    Optional<Address> findByIdAndUserId(Long id, Long userId);
}
