package com.ibm.capstone.bookstore.service;

import com.ibm.capstone.bookstore.domain.entities.Order;
import com.ibm.capstone.bookstore.domain.entities.OrderStatus;
import com.ibm.capstone.bookstore.domain.entities.Shipment;
import com.ibm.capstone.bookstore.domain.entities.ShipmentStatus;
import com.ibm.capstone.bookstore.domain.entities.Store;
import com.ibm.capstone.bookstore.repository.StoreRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

/** Who can return what, and until when. The window comes from the store, so an administrator can change it. */
@Component
public class ReturnPolicy {

    static final int DEFAULT_WINDOW_DAYS = 7;

    private final StoreRepository storeRepository;

    public ReturnPolicy(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    public int windowDays() {
        return storeRepository.findFirstByActiveTrueOrderByIdAsc()
                .map(Store::getReturnWindowDays)
                .orElse(DEFAULT_WINDOW_DAYS);
    }

    /** The last day a delivered order can be returned, or empty if it has not been delivered. */
    public Optional<LocalDate> deadline(Shipment shipment) {
        if (shipment == null || shipment.getStatus() != ShipmentStatus.DELIVERED || shipment.getDeliveredAt() == null) {
            return Optional.empty();
        }
        return Optional.of(shipment.getDeliveredAt().toLocalDate().plusDays(windowDays()));
    }

    /** Why an order cannot be returned right now, or empty if it can. */
    public Optional<String> blocker(Order order, Shipment shipment) {
        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            return Optional.of("This order was cancelled.");
        }
        if (order.getOrderStatus() == OrderStatus.RETURNED) {
            return Optional.of("Every book on this order has already been returned.");
        }
        if (order.getOrderStatus() != OrderStatus.DELIVERED) {
            return Optional.of("Books can be returned once the order has been delivered.");
        }
        Optional<LocalDate> deadline = deadline(shipment);
        if (deadline.isEmpty()) {
            return Optional.of("We have no delivery date for this order yet.");
        }
        if (LocalDate.now().isAfter(deadline.get())) {
            return Optional.of("The " + windowDays() + "-day return window ended on " + deadline.get() + ".");
        }
        return Optional.empty();
    }
}
