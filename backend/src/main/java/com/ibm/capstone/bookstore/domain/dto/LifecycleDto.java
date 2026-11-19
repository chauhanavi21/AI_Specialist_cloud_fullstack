package com.ibm.capstone.bookstore.domain.dto;

import com.ibm.capstone.bookstore.domain.entities.ReturnStatus;
import com.ibm.capstone.bookstore.domain.entities.ShipmentStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Request and response shapes for modifying orders, shipments, returns and refunds. */
public final class LifecycleDto {

    private LifecycleDto() {}

    // ---- Modify order -------------------------------------------------------------------

    /** One line to take off an order: {@code quantity} units of this order item are removed and refunded. */
    public record ItemRemoval(
            @NotNull(message = "Order item id is required") Long orderItemId,
            @NotNull(message = "Quantity is required") @Min(value = 1, message = "Quantity must be at least 1") Integer quantity) {}

    /** Either field may be omitted. Changing the address re-prices delivery; removing items refunds them. */
    public record ModifyOrderRequest(Long addressId, @Valid List<ItemRemoval> removeItems) {}

    // ---- Shipment -----------------------------------------------------------------------

    public record ShipmentResponse(Long id, Long orderId, String carrier, String trackingNumber, ShipmentStatus status,
                                   String shippingZone, BigDecimal shippingFee, LocalDate estimatedDeliveryDate,
                                   LocalDateTime shippedAt, LocalDateTime deliveredAt, List<TrackingEvent> events) {}

    public record TrackingEvent(String status, String description, LocalDateTime at) {}

    // ---- Shipping rate quote (no login needed) --------------------------------------------

    public record ShippingQuoteRequest(
            @NotBlank(message = "City is required") String city,
            @NotBlank(message = "State is required") String state,
            BigDecimal subtotal) {}

    public record ShippingQuoteResponse(String zone, BigDecimal fee, boolean freeDelivery, BigDecimal freeDeliveryThreshold,
                                        int extraTransitDays, String carrier) {}

    // ---- Returns ------------------------------------------------------------------------

    public record ReturnLine(
            @NotNull(message = "Order item id is required") Long orderItemId,
            @NotNull(message = "Quantity is required") @Min(value = 1, message = "Quantity must be at least 1") Integer quantity) {}

    public record ReturnRequest(
            @NotEmpty(message = "Choose at least one book to return") @Valid List<ReturnLine> items,
            @NotBlank(message = "Tell us why you are returning the books") @Size(max = 300, message = "Reason must be 300 characters or fewer") String reason) {}

    public record ReturnLineResponse(Long orderItemId, String bookTitle, int quantity, BigDecimal value) {}

    public record ReturnResponse(Long id, String rmaNumber, Long orderId, String orderNumber, ReturnStatus status,
                                 String reason, List<ReturnLineResponse> items,
                                 BigDecimal estimatedRefund, BigDecimal refundAmount, String refundReference,
                                 int pointsRefunded, int pointsRevoked,
                                 String returnCarrier, String returnTrackingNumber,
                                 LocalDateTime requestedAt, LocalDateTime completedAt, String adminNote,
                                 String message) {}

    /** Result of previewing a return before the customer commits. */
    public record ReturnEligibility(boolean eligible, String reason, LocalDate returnDeadline, List<ReturnableLine> lines) {}

    public record ReturnableLine(Long orderItemId, String bookTitle, int maxQuantity, BigDecimal unitPrice) {}

    public record AdminReturnAction(@Size(max = 300, message = "Note must be 300 characters or fewer") String note) {}

    // ---- Up-sell and cross-sell ---------------------------------------------------------

    public record UpsellResponse(List<BookSummaryResponse> upsell, List<BookSummaryResponse> crossSell) {}
}
