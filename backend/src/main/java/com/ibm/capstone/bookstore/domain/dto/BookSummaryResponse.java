package com.ibm.capstone.bookstore.domain.dto;

import java.math.BigDecimal;

public class BookSummaryResponse {
    private Long id;
    private String title;
    private String author;
    private String isbn;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private BigDecimal rating;
    private Integer reviewCount;
    private String coverImageUrl;
    private Long categoryId;
    private String categoryName;
    private Long publisherId;
    private String publisherName;
    private Boolean inStock;
    private Integer stockQuantity;
    private Integer tentativeDeliveryDays;
    private Boolean isFeatured;
    private Long authorId;
    private String language;
    private String format;
    private Integer copiesSold;
    private java.time.LocalDate launchDate;
    private java.time.LocalDate tentativeDeliveryDate;

    public BookSummaryResponse() {}

    public Long getAuthorId() { return authorId; }
    public void setAuthorId(Long authorId) { this.authorId = authorId; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    public Integer getCopiesSold() { return copiesSold; }
    public void setCopiesSold(Integer copiesSold) { this.copiesSold = copiesSold; }

    public java.time.LocalDate getLaunchDate() { return launchDate; }
    public void setLaunchDate(java.time.LocalDate launchDate) { this.launchDate = launchDate; }

    public java.time.LocalDate getTentativeDeliveryDate() { return tentativeDeliveryDate; }
    public void setTentativeDeliveryDate(java.time.LocalDate tentativeDeliveryDate) { this.tentativeDeliveryDate = tentativeDeliveryDate; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(BigDecimal originalPrice) { this.originalPrice = originalPrice; }

    public BigDecimal getRating() { return rating; }
    public void setRating(BigDecimal rating) { this.rating = rating; }

    public Integer getReviewCount() { return reviewCount; }
    public void setReviewCount(Integer reviewCount) { this.reviewCount = reviewCount; }

    public String getCoverImageUrl() { return coverImageUrl; }
    public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public Long getPublisherId() { return publisherId; }
    public void setPublisherId(Long publisherId) { this.publisherId = publisherId; }

    public String getPublisherName() { return publisherName; }
    public void setPublisherName(String publisherName) { this.publisherName = publisherName; }

    public Boolean getInStock() { return inStock; }
    public void setInStock(Boolean inStock) { this.inStock = inStock; }

    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }

    public Integer getTentativeDeliveryDays() { return tentativeDeliveryDays; }
    public void setTentativeDeliveryDays(Integer tentativeDeliveryDays) { this.tentativeDeliveryDays = tentativeDeliveryDays; }

    public Boolean getIsFeatured() { return isFeatured; }
    public void setIsFeatured(Boolean isFeatured) { this.isFeatured = isFeatured; }
}
