package com.ibm.capstone.bookstore.domain.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "author_follows", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "author_id"}))
public class AuthorFollow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private Author author;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public AuthorFollow() {}

    public AuthorFollow(User user, Author author) {
        this.user = user;
        this.author = author;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Author getAuthor() { return author; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
