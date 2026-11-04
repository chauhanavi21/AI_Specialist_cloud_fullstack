package com.ibm.capstone.bookstore.service;

import com.ibm.capstone.bookstore.domain.dto.AddToCartRequest;
import com.ibm.capstone.bookstore.domain.dto.CartResponse;
import com.ibm.capstone.bookstore.domain.dto.UpdateCartItemRequest;

public interface CartService {
    CartResponse getCartByUserId(Long userId);
    CartResponse addToCart(Long userId, AddToCartRequest request);
    CartResponse updateCartItem(Long userId, Long itemId, UpdateCartItemRequest request);
    CartResponse removeCartItem(Long userId, Long itemId);
    void clearCart(Long userId);
}
