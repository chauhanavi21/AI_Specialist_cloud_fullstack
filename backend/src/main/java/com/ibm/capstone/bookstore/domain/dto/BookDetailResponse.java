package com.ibm.capstone.bookstore.domain.dto;

import java.time.LocalDate;

public class BookDetailResponse extends BookSummaryResponse {
    private String description;
    private Integer publishedYear;
    private Integer pageCount;
    private LocalDate tentativeDeliveryDate;
    private CategoryResponse category;
    private PublisherResponse publisher;

    public BookDetailResponse() {}

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getPublishedYear() { return publishedYear; }
    public void setPublishedYear(Integer publishedYear) { this.publishedYear = publishedYear; }

    public Integer getPageCount() { return pageCount; }
    public void setPageCount(Integer pageCount) { this.pageCount = pageCount; }

    public LocalDate getTentativeDeliveryDate() { return tentativeDeliveryDate; }
    public void setTentativeDeliveryDate(LocalDate tentativeDeliveryDate) { this.tentativeDeliveryDate = tentativeDeliveryDate; }

    public CategoryResponse getCategory() { return category; }
    public void setCategory(CategoryResponse category) { this.category = category; }

    public PublisherResponse getPublisher() { return publisher; }
    public void setPublisher(PublisherResponse publisher) { this.publisher = publisher; }
}
