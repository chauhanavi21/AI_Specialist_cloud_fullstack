package com.ibm.capstone.bookstore.domain.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Request/response shapes for reviews, writers, wallet, coupons and catalog facets. */
public final class StoreDto {

    private StoreDto() {}

    public record ReviewRequest(
            @NotNull(message = "Rating is required") @Min(value = 1, message = "Rating must be 1 to 5") @Max(value = 5, message = "Rating must be 1 to 5") Integer rating,
            @NotBlank(message = "Review text is required") @Size(max = 100, message = "Review must be 100 characters or fewer") String comment) {}

    public record ReviewResponse(Long id, String userName, Integer rating, String comment, LocalDateTime createdAt) {}

    public record WriterProfile(Long id, String name, String bio, String photoUrl, boolean following) {}

    public record AuthorSummary(Long id, String name, String photoUrl, int bookCount, boolean following) {}

    public record AuthorDetail(Long id, String name, String bio, String photoUrl, boolean following,
                               List<BookSummaryResponse> books) {}

    public record WalletTransactionDto(Long id, BigDecimal amount, String type, String description, LocalDateTime createdAt) {}

    public record WalletResponse(BigDecimal balance, List<WalletTransactionDto> transactions) {}

    public record TopUpRequest(
            @NotNull(message = "Amount is required")
            @DecimalMin(value = "1", message = "Minimum top-up is 1")
            @DecimalMax(value = "50000", message = "Maximum top-up is 50000") BigDecimal amount) {}

    public record CouponValidateRequest(@NotBlank(message = "Coupon code is required") String code) {}

    public record CouponResponse(String code, String description, BigDecimal discount) {}

    public record QuoteRequest(String couponCode, Integer pointsToRedeem, String paymentMethod) {}

    /** What the customer will actually pay, priced by the same code that places the order. */
    public record QuoteResponse(BigDecimal subtotal, BigDecimal couponDiscount, String couponCode,
                                BigDecimal tax, BigDecimal shippingFee, BigDecimal pointsDiscount,
                                int pointsApplied, int maxPoints, BigDecimal payable,
                                int pointsToEarn, BigDecimal walletBalance, boolean walletCoversTotal) {}

    public record FacetsResponse(List<String> languages, List<String> formats) {}

    public record ShelvesResponse(List<BookSummaryResponse> bestsellers,
                                  List<BookSummaryResponse> newLaunches,
                                  List<BookSummaryResponse> topRated) {}
}
