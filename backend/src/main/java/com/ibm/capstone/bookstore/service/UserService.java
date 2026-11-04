package com.ibm.capstone.bookstore.service;

import com.ibm.capstone.bookstore.domain.dto.AddressRequest;
import com.ibm.capstone.bookstore.domain.dto.AddressResponse;

import java.util.List;

public interface UserService {
    List<AddressResponse> getUserAddresses(Long userId);
    AddressResponse addAddress(Long userId, AddressRequest request);
}
