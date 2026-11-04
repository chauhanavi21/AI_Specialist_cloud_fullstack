package com.ibm.capstone.bookstore.domain.dto;

public class PublisherResponse {
    private Long id;
    private String name;
    private String code;
    private String website;

    public PublisherResponse() {}

    public PublisherResponse(Long id, String name, String code, String website) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.website = website;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
}
