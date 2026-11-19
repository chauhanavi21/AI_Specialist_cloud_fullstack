package com.ibm.capstone.bookstore.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Request and response shapes for the administration API: stores, policies, catalogues and books. */
public final class AdminDto {

    private AdminDto() {}

    // ---- Stores and policies --------------------------------------------------------------

    public record StoreRequest(
            @NotBlank(message = "Store name is required") @Size(max = 150, message = "Name must be 150 characters or fewer") String name,
            @Size(max = 255, message = "Tagline must be 255 characters or fewer") String tagline,
            @Min(value = 0, message = "Return window cannot be negative") @Max(value = 90, message = "Return window is at most 90 days") Integer returnWindowDays) {}

    public record PolicyRequest(
            @NotBlank(message = "Policy type is required")
            @Pattern(regexp = "CANCELLATION|RETURN|SHIPPING|PRIVACY|PAYMENT|OTHER", message = "Policy type must be CANCELLATION, RETURN, SHIPPING, PRIVACY, PAYMENT or OTHER") String type,
            @NotBlank(message = "Title is required") @Size(max = 150, message = "Title must be 150 characters or fewer") String title,
            @NotBlank(message = "Policy text is required") @Size(max = 4000, message = "Policy text must be 4000 characters or fewer") String body) {}

    public record PolicyResponse(Long id, String type, String title, String body) {}

    public record StoreResponse(Long id, String name, String tagline, String currency, int returnWindowDays,
                                boolean active, List<PolicyResponse> policies) {}

    // ---- Catalogue (categories) -----------------------------------------------------------

    public record CategoryRequest(
            @NotBlank(message = "Category name is required") @Size(max = 100, message = "Name must be 100 characters or fewer") String name,
            @Size(max = 1000, message = "Description must be 1000 characters or fewer") String description,
            @Pattern(regexp = "PUBLIC|MEMBERS", message = "Visibility must be PUBLIC or MEMBERS") String visibility) {}

    // ---- Books ------------------------------------------------------------------------------

    public record BookRequest(
            @NotBlank(message = "Title is required") @Size(max = 255, message = "Title must be 255 characters or fewer") String title,
            @NotBlank(message = "Author is required") @Size(max = 255, message = "Author must be 255 characters or fewer") String author,
            @NotBlank(message = "ISBN is required") @Size(max = 20, message = "ISBN must be 20 characters or fewer") String isbn,
            @NotNull(message = "Category is required") Long categoryId,
            @NotNull(message = "Publisher is required") Long publisherId,
            Long authorId,
            @NotNull(message = "Price is required") @DecimalMin(value = "0.01", message = "Price must be positive") BigDecimal price,
            @DecimalMin(value = "0.01", message = "Original price must be positive") BigDecimal originalPrice,
            @NotNull(message = "Stock is required") @Min(value = 0, message = "Stock cannot be negative") Integer stockQuantity,
            @NotBlank(message = "Description is required") @Size(max = 4000, message = "Description must be 4000 characters or fewer") String description,
            @NotNull(message = "Published year is required") @Min(value = 1900, message = "Published year is not valid") @Max(value = 2100, message = "Published year is not valid") Integer publishedYear,
            @NotNull(message = "Page count is required") @Min(value = 1, message = "Page count must be positive") Integer pageCount,
            @Size(max = 50, message = "Language must be 50 characters or fewer") String language,
            @Size(max = 30, message = "Format must be 30 characters or fewer") String format,
            @Min(value = 1, message = "Delivery days must be at least 1") @Max(value = 30, message = "Delivery days must be at most 30") Integer tentativeDeliveryDays,
            Boolean featured) {}

    public record StockUpdate(@NotNull(message = "Stock is required") @Min(value = 0, message = "Stock cannot be negative") Integer stockQuantity) {}

    // ---- Dashboard ------------------------------------------------------------------------

    public record OrderRow(Long id, String orderNumber, String customerEmail, String status, String paymentStatus,
                           BigDecimal totalAmount, LocalDateTime createdAt) {}

    public record ShipmentUpdate(@NotNull(message = "Status is required")
                                 @Pattern(regexp = "SHIPPED|DELIVERED", message = "Status must be SHIPPED or DELIVERED") String status) {}

    public record Overview(long orders, long customers, long books, long openReturns, BigDecimal revenue) {}
}
