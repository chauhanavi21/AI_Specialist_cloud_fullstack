package com.ibm.capstone.bookstore.controller;

import com.ibm.capstone.bookstore.domain.dto.AdminDto;
import com.ibm.capstone.bookstore.domain.dto.BookDetailResponse;
import com.ibm.capstone.bookstore.domain.dto.CategoryResponse;
import com.ibm.capstone.bookstore.domain.dto.LifecycleDto;
import com.ibm.capstone.bookstore.domain.entities.Order;
import com.ibm.capstone.bookstore.domain.entities.ShipmentStatus;
import com.ibm.capstone.bookstore.repository.BookRepository;
import com.ibm.capstone.bookstore.repository.OrderRepository;
import com.ibm.capstone.bookstore.repository.OrderReturnRepository;
import com.ibm.capstone.bookstore.repository.UserRepository;
import com.ibm.capstone.bookstore.service.CatalogAdminService;
import com.ibm.capstone.bookstore.service.ReturnService;
import com.ibm.capstone.bookstore.service.ShipmentService;
import com.ibm.capstone.bookstore.service.StoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Store administration. Every route here needs ROLE_ADMIN: the URL rule in SecurityConfig refuses everyone
 * else before the request arrives, and {@code @PreAuthorize} repeats the check so a mistake in one place
 * cannot expose the API.
 */
@RestController
@RequestMapping("/admin")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@Tag(name = "Administration", description = "Stores, store policies, catalogue, stock, shipments and returns (administrators only)")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final StoreService storeService;
    private final CatalogAdminService catalogService;
    private final ShipmentService shipmentService;
    private final ReturnService returnService;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final OrderReturnRepository returnRepository;

    public AdminController(StoreService storeService, CatalogAdminService catalogService,
                           ShipmentService shipmentService, ReturnService returnService,
                           OrderRepository orderRepository, UserRepository userRepository,
                           BookRepository bookRepository, OrderReturnRepository returnRepository) {
        this.storeService = storeService;
        this.catalogService = catalogService;
        this.shipmentService = shipmentService;
        this.returnService = returnService;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
        this.returnRepository = returnRepository;
    }

    // ---- Stores and policies --------------------------------------------------------------

    @GetMapping("/stores")
    @Operation(summary = "List every store, including inactive ones")
    public List<AdminDto.StoreResponse> stores() {
        return storeService.allStores();
    }

    @PostMapping("/stores")
    @Operation(summary = "Create a store")
    public ResponseEntity<AdminDto.StoreResponse> createStore(@Valid @RequestBody AdminDto.StoreRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(storeService.create(request));
    }

    @PutMapping("/stores/{id}")
    @Operation(summary = "Update a store's name, tagline and return window")
    public AdminDto.StoreResponse updateStore(@PathVariable Long id, @Valid @RequestBody AdminDto.StoreRequest request) {
        return storeService.update(id, request);
    }

    @PutMapping("/stores/{id}/active/{active}")
    @Operation(summary = "Open or close a store")
    public AdminDto.StoreResponse setStoreActive(@PathVariable Long id, @PathVariable boolean active) {
        return storeService.setActive(id, active);
    }

    @PostMapping("/stores/{id}/policies")
    @Operation(summary = "Add a store policy (cancellation, returns, shipping, privacy, payment)")
    public ResponseEntity<AdminDto.StoreResponse> addPolicy(@PathVariable Long id, @Valid @RequestBody AdminDto.PolicyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(storeService.addPolicy(id, request));
    }

    @PutMapping("/stores/{id}/policies/{policyId}")
    @Operation(summary = "Edit a store policy")
    public AdminDto.StoreResponse updatePolicy(@PathVariable Long id, @PathVariable Long policyId,
                                               @Valid @RequestBody AdminDto.PolicyRequest request) {
        return storeService.updatePolicy(id, policyId, request);
    }

    @DeleteMapping("/stores/{id}/policies/{policyId}")
    @Operation(summary = "Remove a store policy")
    public AdminDto.StoreResponse deletePolicy(@PathVariable Long id, @PathVariable Long policyId) {
        return storeService.deletePolicy(id, policyId);
    }

    // ---- Catalogue ----------------------------------------------------------------------------

    @PostMapping("/categories")
    @Operation(summary = "Create a catalogue category; MEMBERS visibility hides it from guests")
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody AdminDto.CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogService.createCategory(request));
    }

    @PutMapping("/categories/{id}")
    @Operation(summary = "Update a category")
    public CategoryResponse updateCategory(@PathVariable Long id, @Valid @RequestBody AdminDto.CategoryRequest request) {
        return catalogService.updateCategory(id, request);
    }

    @DeleteMapping("/categories/{id}")
    @Operation(summary = "Delete an empty category")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        catalogService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/books")
    @Operation(summary = "Add a book to the catalogue")
    public ResponseEntity<BookDetailResponse> createBook(@Valid @RequestBody AdminDto.BookRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogService.createBook(request));
    }

    @PutMapping("/books/{id}")
    @Operation(summary = "Update a book")
    public BookDetailResponse updateBook(@PathVariable Long id, @Valid @RequestBody AdminDto.BookRequest request) {
        return catalogService.updateBook(id, request);
    }

    @PutMapping("/books/{id}/stock")
    @Operation(summary = "Set the stock on hand for a book")
    public BookDetailResponse setStock(@PathVariable Long id, @Valid @RequestBody AdminDto.StockUpdate request) {
        return catalogService.setStock(id, request.stockQuantity());
    }

    @DeleteMapping("/books/{id}")
    @Operation(summary = "Delete a book that has never been ordered")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        catalogService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }

    // ---- Orders and shipments ------------------------------------------------------------------

    @GetMapping("/orders")
    @Operation(summary = "Recent orders across all customers")
    public List<AdminDto.OrderRow> orders() {
        return orderRepository.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(100)
                .map(o -> new AdminDto.OrderRow(o.getId(), o.getOrderNumber(), o.getUser().getEmail(),
                        o.getOrderStatus().name(), o.getPaymentStatus().name(), o.getTotalAmount(), o.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @PutMapping("/orders/{orderId}/shipment")
    @Operation(summary = "Mark an order's parcel as SHIPPED or DELIVERED")
    public LifecycleDto.ShipmentResponse updateShipment(@PathVariable Long orderId, @Valid @RequestBody AdminDto.ShipmentUpdate request) {
        return shipmentService.advance(orderId, ShipmentStatus.valueOf(request.status()));
    }

    // ---- Returns ------------------------------------------------------------------------------

    @GetMapping("/returns")
    @Operation(summary = "Return requests; open ones only unless all=true")
    public List<LifecycleDto.ReturnResponse> returns(@RequestParam(defaultValue = "false") boolean all) {
        return returnService.all(!all);
    }

    @PostMapping("/returns/{id}/pickup")
    @Operation(summary = "Record that the carrier has collected the returned parcel")
    public LifecycleDto.ReturnResponse pickup(@PathVariable Long id) {
        return returnService.markPickedUp(id);
    }

    @PostMapping("/returns/{id}/receive")
    @Operation(summary = "Record that the returned books arrived: restock them and pay the refund")
    public LifecycleDto.ReturnResponse receive(@PathVariable Long id, @Valid @RequestBody(required = false) LifecycleDto.AdminReturnAction action) {
        return returnService.receiveAndRefund(id, action == null ? null : action.note());
    }

    @PostMapping("/returns/{id}/reject")
    @Operation(summary = "Reject a return request")
    public LifecycleDto.ReturnResponse reject(@PathVariable Long id, @Valid @RequestBody(required = false) LifecycleDto.AdminReturnAction action) {
        return returnService.reject(id, action == null ? null : action.note());
    }

    // ---- Dashboard ----------------------------------------------------------------------------

    @GetMapping("/overview")
    @Operation(summary = "Headline numbers for the admin dashboard")
    public AdminDto.Overview overview() {
        List<Order> all = orderRepository.findAll();
        BigDecimal revenue = all.stream()
                .map(o -> o.getTotalAmount().subtract(o.getRefundedAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long openReturns = returnRepository.findByStatusInOrderByRequestedAtAsc(
                List.of(com.ibm.capstone.bookstore.domain.entities.ReturnStatus.REQUESTED,
                        com.ibm.capstone.bookstore.domain.entities.ReturnStatus.PICKED_UP)).size();
        long customers = userRepository.findAll().stream().filter(u -> "ROLE_CUSTOMER".equals(u.getRole())).count();
        return new AdminDto.Overview(all.size(), customers, bookRepository.count(), openReturns, revenue);
    }
}
