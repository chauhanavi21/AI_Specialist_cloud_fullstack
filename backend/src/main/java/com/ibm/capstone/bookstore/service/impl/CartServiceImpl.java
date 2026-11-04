package com.ibm.capstone.bookstore.service.impl;

import com.ibm.capstone.bookstore.domain.dto.AddToCartRequest;
import com.ibm.capstone.bookstore.domain.dto.CartItemResponse;
import com.ibm.capstone.bookstore.domain.dto.CartResponse;
import com.ibm.capstone.bookstore.domain.dto.UpdateCartItemRequest;
import com.ibm.capstone.bookstore.domain.entities.Book;
import com.ibm.capstone.bookstore.domain.entities.Cart;
import com.ibm.capstone.bookstore.domain.entities.CartItem;
import com.ibm.capstone.bookstore.domain.entities.User;
import com.ibm.capstone.bookstore.exception.BadRequestException;
import com.ibm.capstone.bookstore.exception.ResourceNotFoundException;
import com.ibm.capstone.bookstore.repository.BookRepository;
import com.ibm.capstone.bookstore.repository.CartItemRepository;
import com.ibm.capstone.bookstore.repository.CartRepository;
import com.ibm.capstone.bookstore.repository.UserRepository;
import com.ibm.capstone.bookstore.service.CartService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final BookServiceImpl bookService;

    public CartServiceImpl(CartRepository cartRepository,
                           CartItemRepository cartItemRepository,
                           BookRepository bookRepository,
                           UserRepository userRepository,
                           BookServiceImpl bookService) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
        this.bookService = bookService;
    }

    @Override
    @Transactional
    public CartResponse getCartByUserId(Long userId) {
        Cart cart = getOrCreateCart(userId);
        return mapToCartResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse addToCart(Long userId, AddToCartRequest request) {
        Cart cart = getOrCreateCart(userId);
        Book book = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + request.getBookId()));

        if (book.getStockQuantity() < request.getQuantity()) {
            throw new BadRequestException("Requested quantity (" + request.getQuantity() + ") exceeds available stock (" + book.getStockQuantity() + ")");
        }

        Optional<CartItem> existingItem = cartItemRepository.findByCartIdAndBookId(cart.getId(), book.getId());

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            int newQuantity = item.getQuantity() + request.getQuantity();
            if (book.getStockQuantity() < newQuantity) {
                throw new BadRequestException("Total quantity in cart (" + newQuantity + ") exceeds available stock (" + book.getStockQuantity() + ")");
            }
            item.setQuantity(newQuantity);
            item.setUnitPrice(book.getPrice());
            cartItemRepository.save(item);
        } else {
            CartItem newItem = new CartItem(cart, book, request.getQuantity(), book.getPrice());
            cart.addItem(newItem);
            cartItemRepository.save(newItem);
        }

        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);

        return mapToCartResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse updateCartItem(Long userId, Long itemId, UpdateCartItemRequest request) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = cartItemRepository.findByIdAndCartUserId(itemId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found: " + itemId));

        if (item.getBook().getStockQuantity() < request.getQuantity()) {
            throw new BadRequestException("Requested quantity exceeds available stock (" + item.getBook().getStockQuantity() + ")");
        }

        item.setQuantity(request.getQuantity());
        item.setUnitPrice(item.getBook().getPrice());
        cartItemRepository.save(item);

        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);

        return mapToCartResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse removeCartItem(Long userId, Long itemId) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = cartItemRepository.findByIdAndCartUserId(itemId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found: " + itemId));

        cart.removeItem(item);
        cartItemRepository.delete(item);

        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);

        return mapToCartResponse(cart);
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        cart.getItems().clear();
        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);
    }

    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
            Cart newCart = new Cart(user);
            return cartRepository.save(newCart);
        });
    }

    public CartResponse mapToCartResponse(Cart cart) {
        CartResponse dto = new CartResponse();
        dto.setId(cart.getId());

        var items = cart.getItems().stream().map(item -> new CartItemResponse(
                item.getId(),
                bookService.mapToBookSummary(item.getBook()),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getSubtotal()
        )).collect(Collectors.toList());

        dto.setItems(items);
        dto.setItemCount(items.stream().mapToInt(CartItemResponse::getQuantity).sum());

        BigDecimal subtotal = items.stream()
                .map(CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dto.setSubtotal(subtotal);

        BigDecimal estimatedTax = subtotal.multiply(BigDecimal.valueOf(0.08)).setScale(2, RoundingMode.HALF_UP);
        dto.setEstimatedTax(estimatedTax);

        // Free shipping if subtotal > 50.00
        BigDecimal shippingFee = (subtotal.compareTo(BigDecimal.valueOf(50.00)) >= 0 || subtotal.compareTo(BigDecimal.ZERO) == 0)
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(5.99);
        dto.setShippingFee(shippingFee);

        dto.setTotalAmount(subtotal.add(estimatedTax).add(shippingFee));
        return dto;
    }
}
