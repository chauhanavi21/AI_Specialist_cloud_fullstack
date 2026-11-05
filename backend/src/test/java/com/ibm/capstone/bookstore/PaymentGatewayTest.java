package com.ibm.capstone.bookstore;

import com.ibm.capstone.bookstore.domain.dto.PlaceOrderRequest;
import com.ibm.capstone.bookstore.domain.entities.PaymentMethod;
import com.ibm.capstone.bookstore.exception.BadRequestException;
import com.ibm.capstone.bookstore.service.PaymentGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

class PaymentGatewayTest {

    private final PaymentGateway gateway = new PaymentGateway();

    private PlaceOrderRequest card(String number, String expiry, String cvv) {
        PlaceOrderRequest r = new PlaceOrderRequest();
        r.setCardNumber(number);
        r.setCardName("Alex Rivera");
        r.setCardExpiry(expiry);
        r.setCardCvv(cvv);
        return r;
    }

    private String nextYearExpiry() {
        return YearMonth.now().plusYears(1).format(DateTimeFormatter.ofPattern("MM/yy"));
    }

    @Test
    @DisplayName("Accepts a Luhn-valid card and keeps only the last four digits")
    void validCard() {
        PaymentGateway.PaymentResult result = gateway.charge(PaymentMethod.CREDIT_CARD,
                card("4242 4242 4242 4242", nextYearExpiry(), "123"));
        assertEquals("4242", result.cardLast4());
        assertTrue(result.reference().startsWith("CRD-"));
    }

    @Test
    @DisplayName("Rejects a card number that fails the Luhn check")
    void badLuhn() {
        assertThrows(BadRequestException.class, () -> gateway.charge(PaymentMethod.DEBIT_CARD,
                card("4242 4242 4242 4241", nextYearExpiry(), "123")));
    }

    @Test
    @DisplayName("Rejects an expired card")
    void expiredCard() {
        assertThrows(BadRequestException.class, () -> gateway.charge(PaymentMethod.CREDIT_CARD,
                card("4242424242424242", "01/20", "123")));
    }

    @Test
    @DisplayName("Rejects a missing or short CVV")
    void badCvv() {
        assertThrows(BadRequestException.class, () -> gateway.charge(PaymentMethod.CREDIT_CARD,
                card("4242424242424242", nextYearExpiry(), "12")));
    }

    @Test
    @DisplayName("The documented decline test card is declined")
    void declinedCard() {
        BadRequestException ex = assertThrows(BadRequestException.class, () -> gateway.charge(PaymentMethod.CREDIT_CARD,
                card("4000 0000 0000 0002", nextYearExpiry(), "123")));
        assertTrue(ex.getMessage().toLowerCase().contains("declined"));
    }

    @Test
    @DisplayName("UPI accepts name@bank and rejects malformed IDs")
    void upi() {
        PlaceOrderRequest good = new PlaceOrderRequest();
        good.setUpiId("avi.chauhan@okbank");
        assertTrue(gateway.charge(PaymentMethod.UPI, good).reference().startsWith("UPI-"));

        PlaceOrderRequest bad = new PlaceOrderRequest();
        bad.setUpiId("not-a-upi-id");
        assertThrows(BadRequestException.class, () -> gateway.charge(PaymentMethod.UPI, bad));
    }

    @Test
    @DisplayName("Card details never appear in the request's toString")
    void requestToStringHidesCard() {
        String text = card("4242424242424242", "12/30", "123").toString();
        assertFalse(text.contains("4242424242424242"));
        assertFalse(text.contains("123"));
    }
}
