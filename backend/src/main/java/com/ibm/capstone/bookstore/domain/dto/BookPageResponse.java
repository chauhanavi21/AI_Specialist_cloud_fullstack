package com.ibm.capstone.bookstore.domain.dto;

import java.util.List;

public class BookPageResponse {
    private List<BookSummaryResponse> content;
    private long totalElements;
    private int totalPages;
    private int page;
    private int size;

    public BookPageResponse() {}

    public BookPageResponse(List<BookSummaryResponse> content, long totalElements, int totalPages, int page, int size) {
        this.content = content;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.page = page;
        this.size = size;
    }

    public List<BookSummaryResponse> getContent() { return content; }
    public void setContent(List<BookSummaryResponse> content) { this.content = content; }

    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
}
