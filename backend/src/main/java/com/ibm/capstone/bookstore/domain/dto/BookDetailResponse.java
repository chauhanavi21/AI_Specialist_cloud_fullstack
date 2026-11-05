package com.ibm.capstone.bookstore.domain.dto;

import java.util.ArrayList;
import java.util.List;

public class BookDetailResponse extends BookSummaryResponse {
    private String description;
    private Integer publishedYear;
    private Integer pageCount;
    private CategoryResponse category;
    private PublisherResponse publisher;
    private StoreDto.WriterProfile writer;
    private List<StoreDto.ReviewResponse> reviews = new ArrayList<>();
    private boolean inWishlist;

    public BookDetailResponse() {}

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getPublishedYear() { return publishedYear; }
    public void setPublishedYear(Integer publishedYear) { this.publishedYear = publishedYear; }

    public Integer getPageCount() { return pageCount; }
    public void setPageCount(Integer pageCount) { this.pageCount = pageCount; }

    public CategoryResponse getCategory() { return category; }
    public void setCategory(CategoryResponse category) { this.category = category; }

    public PublisherResponse getPublisher() { return publisher; }
    public void setPublisher(PublisherResponse publisher) { this.publisher = publisher; }

    public StoreDto.WriterProfile getWriter() { return writer; }
    public void setWriter(StoreDto.WriterProfile writer) { this.writer = writer; }

    public List<StoreDto.ReviewResponse> getReviews() { return reviews; }
    public void setReviews(List<StoreDto.ReviewResponse> reviews) { this.reviews = reviews; }

    public boolean isInWishlist() { return inWishlist; }
    public void setInWishlist(boolean inWishlist) { this.inWishlist = inWishlist; }
}
