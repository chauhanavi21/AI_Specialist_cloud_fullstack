package com.ibm.capstone.bookstore.repository;

import com.ibm.capstone.bookstore.domain.entities.Author;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuthorRepository extends JpaRepository<Author, Long> {
    List<Author> findAllByOrderByNameAsc();
}
