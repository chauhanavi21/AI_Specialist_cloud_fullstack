package com.ibm.capstone.bookstore.controller;

import com.ibm.capstone.bookstore.domain.dto.AuthResponse;
import com.ibm.capstone.bookstore.domain.dto.LoginRequest;
import com.ibm.capstone.bookstore.domain.dto.RegisterRequest;
import com.ibm.capstone.bookstore.domain.dto.UserProfileResponse;
import com.ibm.capstone.bookstore.security.UserPrincipal;
import com.ibm.capstone.bookstore.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "User registration, authentication and profile retrieval")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register new customer")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Login and obtain JWT token")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Sign out: revokes the presented token so it can no longer be used")
    public ResponseEntity<java.util.Map<String, String>> logout(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        String token = header != null && header.startsWith("Bearer ") ? header.substring(7) : null;
        authService.logout(token);
        return ResponseEntity.ok(java.util.Map.of("message", "Signed out"));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated customer profile")
    public ResponseEntity<UserProfileResponse> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        UserProfileResponse response = authService.getCurrentUser(principal.getId());
        return ResponseEntity.ok(response);
    }
}
