package com.ibm.capstone.bookstore.service;

import com.ibm.capstone.bookstore.domain.dto.AuthResponse;
import com.ibm.capstone.bookstore.domain.dto.LoginRequest;
import com.ibm.capstone.bookstore.domain.dto.RegisterRequest;
import com.ibm.capstone.bookstore.domain.dto.UserProfileResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    UserProfileResponse getCurrentUser(Long userId);

    /** Signs the caller out: the presented token is revoked until it would have expired. */
    void logout(String token);
}
