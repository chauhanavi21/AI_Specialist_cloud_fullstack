package com.ibm.capstone.bookstore.service.impl;

import com.ibm.capstone.bookstore.domain.dto.AuthResponse;
import com.ibm.capstone.bookstore.domain.dto.LoginRequest;
import com.ibm.capstone.bookstore.domain.dto.RegisterRequest;
import com.ibm.capstone.bookstore.domain.dto.UserProfileResponse;
import com.ibm.capstone.bookstore.domain.entities.PointsTransactionType;
import com.ibm.capstone.bookstore.domain.entities.User;
import com.ibm.capstone.bookstore.exception.BadRequestException;
import com.ibm.capstone.bookstore.exception.ResourceNotFoundException;
import com.ibm.capstone.bookstore.repository.UserRepository;
import com.ibm.capstone.bookstore.security.JwtTokenProvider;
import com.ibm.capstone.bookstore.security.UserPrincipal;
import com.ibm.capstone.bookstore.service.AuthService;
import com.ibm.capstone.bookstore.service.GiftPointsService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final GiftPointsService giftPointsService;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager,
                           JwtTokenProvider tokenProvider,
                           GiftPointsService giftPointsService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.giftPointsService = giftPointsService;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
            throw new BadRequestException("Email is already registered: " + request.getEmail());
        }

        User user = new User(
                request.getEmail().trim().toLowerCase(),
                passwordEncoder.encode(request.getPassword()),
                request.getFullName().trim(),
                request.getPhoneNumber() != null ? request.getPhoneNumber().trim() : null
        );
        user.setGiftPointsBalance(150); // Welcome bonus points
        User savedUser = userRepository.save(user);

        giftPointsService.recordTransaction(
                savedUser,
                null,
                150,
                PointsTransactionType.EARNED,
                "Welcome Registration Gift Points"
        );

        UserPrincipal principal = UserPrincipal.create(savedUser);
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = tokenProvider.generateToken(authentication);
        return new AuthResponse(jwt, mapToProfileResponse(savedUser));
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail().trim().toLowerCase(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + principal.getId()));

        return new AuthResponse(jwt, mapToProfileResponse(user));
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return mapToProfileResponse(user);
    }

    private UserProfileResponse mapToProfileResponse(User user) {
        UserProfileResponse profile = new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhoneNumber(),
                user.getGiftPointsBalance(),
                user.getRole(),
                user.getCreatedAt()
        );
        profile.setWalletBalance(user.getWalletBalance());
        return profile;
    }
}
