package com.ibm.capstone.bookstore.service;

import com.ibm.capstone.bookstore.config.PricingProperties;
import com.ibm.capstone.bookstore.domain.entities.Address;
import com.ibm.capstone.bookstore.domain.entities.ShippingZone;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;

/**
 * Shipping rate and delivery-time calculation. The zone comes from the delivery address relative to the
 * dispatch warehouse, and drives both the delivery charge and the extra transit days.
 *
 * Free delivery above the threshold is decided in {@link PricingCalculator}, so the order total, the basket
 * and the quote all use the same rule.
 */
@Component
public class ShippingCalculator {

    private static final Set<String> NEIGHBOURING_STATES = Set.of(
            "tamil nadu", "kerala", "andhra pradesh", "telangana", "goa", "maharashtra");

    private final PricingProperties props;

    public ShippingCalculator(PricingProperties props) {
        this.props = props;
    }

    /** Local = same city as the warehouse, regional = same or neighbouring state, national = anywhere else in India. */
    public ShippingZone zoneFor(Address address) {
        if (address == null) {
            return ShippingZone.REGIONAL;
        }
        String city = normalise(address.getCity());
        String state = normalise(address.getState());
        if (city.equals(normalise(props.getWarehouseCity()))) {
            return ShippingZone.LOCAL;
        }
        if (state.equals(normalise(props.getWarehouseState())) || NEIGHBOURING_STATES.contains(state)) {
            return ShippingZone.REGIONAL;
        }
        return ShippingZone.NATIONAL;
    }

    /** Flat delivery charge for the zone, before any free-delivery rule. */
    public BigDecimal feeFor(ShippingZone zone) {
        return switch (zone) {
            case LOCAL -> props.getLocalShippingFee();
            case REGIONAL -> props.getShippingFee();
            case NATIONAL -> props.getNationalShippingFee();
        };
    }

    /** Days added on top of the book's own dispatch time. */
    public int extraTransitDays(ShippingZone zone) {
        return switch (zone) {
            case LOCAL -> 0;
            case REGIONAL -> 1;
            case NATIONAL -> 3;
        };
    }

    /** Tentative delivery date for the slowest book on the order, shipped to the given zone. */
    public LocalDate deliveryDate(int slowestBookDays, ShippingZone zone) {
        return LocalDate.now().plusDays((long) slowestBookDays + extraTransitDays(zone));
    }

    /** Carrier handling the zone. */
    public String carrierFor(ShippingZone zone) {
        return switch (zone) {
            case LOCAL -> "Dunzo Local Express";
            case REGIONAL -> "BlueDart Express";
            case NATIONAL -> "Delhivery Surface";
        };
    }

    private static String normalise(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
