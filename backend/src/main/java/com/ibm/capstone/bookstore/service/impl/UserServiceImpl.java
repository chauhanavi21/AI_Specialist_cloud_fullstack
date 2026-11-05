package com.ibm.capstone.bookstore.service.impl;

import com.ibm.capstone.bookstore.domain.dto.AddressRequest;
import com.ibm.capstone.bookstore.domain.dto.AddressResponse;
import com.ibm.capstone.bookstore.domain.entities.Address;
import com.ibm.capstone.bookstore.domain.entities.User;
import com.ibm.capstone.bookstore.exception.ResourceNotFoundException;
import com.ibm.capstone.bookstore.repository.AddressRepository;
import com.ibm.capstone.bookstore.repository.UserRepository;
import com.ibm.capstone.bookstore.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    public UserServiceImpl(AddressRepository addressRepository, UserRepository userRepository) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getUserAddresses(Long userId) {
        return addressRepository.findByUserIdOrderByIdAsc(userId).stream()
                .map(this::mapToAddressResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AddressResponse addAddress(Long userId, AddressRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        List<Address> existing = addressRepository.findByUserIdOrderByIdAsc(userId);

        Address address = new Address();
        address.setUser(user);
        address.setRecipientName(request.getRecipientName().trim());
        address.setEmail(request.getEmail().trim().toLowerCase());
        address.setPhoneNumber(request.getPhoneNumber().trim());
        address.setStreetAddress(request.getStreetAddress().trim());
        address.setAddressLine2(request.getAddressLine2() != null && !request.getAddressLine2().isBlank()
                ? request.getAddressLine2().trim() : null);
        address.setCity(request.getCity().trim());
        address.setState(request.getState().trim());
        address.setPostalCode(request.getPostalCode().trim());
        address.setCountry(request.getCountry().trim());

        if (existing.isEmpty() || Boolean.TRUE.equals(request.getIsDefault())) {
            address.setIsDefault(true);
            if (Boolean.TRUE.equals(request.getIsDefault())) {
                existing.forEach(a -> a.setIsDefault(false));
                addressRepository.saveAll(existing);
            }
        } else {
            address.setIsDefault(false);
        }

        Address saved = addressRepository.save(address);
        return mapToAddressResponse(saved);
    }

    private AddressResponse mapToAddressResponse(Address address) {
        return AddressResponse.from(address);
    }
}
