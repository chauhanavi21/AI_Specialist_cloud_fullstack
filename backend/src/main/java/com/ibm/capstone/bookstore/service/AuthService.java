package com.ibm.capstone.bookstore.service;

import com.ibm.capstone.bookstore.domain.dto.AuthResponse;
import com.ibm.capstone.bookstore.domain.dto.LoginRequest;
import com.ibm.capstone.bookstore.domain.dto.RegisterRequest;
import com.ibm.capstone.bookstore.domain.dto.UserProfileResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    UserProfileResponse getCurrentUser(Long userId);
}
