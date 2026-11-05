package com.ibm.capstone.bookstore.domain.dto;

import com.ibm.capstone.bookstore.domain.entities.Address;

public class AddressResponse {
    private Long id;
    private String recipientName;
    private String email;
    private String phoneNumber;
    private String streetAddress;
    private String addressLine2;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private Boolean isDefault;

    public AddressResponse() {}

    public static AddressResponse from(Address a) {
        AddressResponse r = new AddressResponse();
        r.id = a.getId();
        r.recipientName = a.getRecipientName();
        r.email = a.getEmail();
        r.phoneNumber = a.getPhoneNumber();
        r.streetAddress = a.getStreetAddress();
        r.addressLine2 = a.getAddressLine2();
        r.city = a.getCity();
        r.state = a.getState();
        r.postalCode = a.getPostalCode();
        r.country = a.getCountry();
        r.isDefault = a.getIsDefault();
        return r;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getStreetAddress() { return streetAddress; }
    public void setStreetAddress(String streetAddress) { this.streetAddress = streetAddress; }

    public String getAddressLine2() { return addressLine2; }
    public void setAddressLine2(String addressLine2) { this.addressLine2 = addressLine2; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public Boolean getIsDefault() { return isDefault; }
    public void setIsDefault(Boolean isDefault) { this.isDefault = isDefault; }
}
