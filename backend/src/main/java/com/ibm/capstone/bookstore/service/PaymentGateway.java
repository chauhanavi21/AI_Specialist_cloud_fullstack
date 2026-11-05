package com.ibm.capstone.bookstore.service;

import com.ibm.capstone.bookstore.domain.dto.PlaceOrderRequest;
import com.ibm.capstone.bookstore.domain.entities.PaymentMethod;
import com.ibm.capstone.bookstore.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Simulated payment gateway. It validates the payment instrument the way a real
 * gateway front-end would and returns a payment reference, but it never contacts a
 * real processor and never keeps the card number, expiry or CVV.
 *
 * Test card 4000 0000 0000 0002 is always declined, so the failure path can be demoed.
 */
@Component
public class PaymentGateway {

    private static final String DECLINED_TEST_CARD = "4000000000000002";
    private static final Pattern UPI_ID = Pattern.compile("^[a-zA-Z0-9.\\-_]{2,64}@[a-zA-Z]{2,32}$");

    public record PaymentResult(String reference, String cardLast4) {}

    public PaymentResult charge(PaymentMethod method, PlaceOrderRequest request) {
        return switch (method) {
            case CREDIT_CARD, DEBIT_CARD -> chargeCard(request);
            case UPI -> chargeUpi(request);
            case WALLET -> new PaymentResult(newReference("WLT"), null);
        };
    }

    private PaymentResult chargeCard(PlaceOrderRequest request) {
        String number = digitsOnly(request.getCardNumber());
        if (number.length() < 13 || number.length() > 19 || !passesLuhn(number)) {
            throw new BadRequestException("Enter a valid card number");
        }
        if (request.getCardName() == null || request.getCardName().isBlank()) {
            throw new BadRequestException("Enter the name on the card");
        }
        if (!isFutureExpiry(request.getCardExpiry())) {
            throw new BadRequestException("Card expiry date is invalid or in the past (use MM/YY)");
        }
        String cvv = digitsOnly(request.getCardCvv());
        if (cvv.length() < 3 || cvv.length() > 4) {
            throw new BadRequestException("Enter a valid CVV");
        }
        if (DECLINED_TEST_CARD.equals(number)) {
            throw new BadRequestException("Payment declined by the issuing bank. Try another card or payment method.");
        }
        return new PaymentResult(newReference("CRD"), number.substring(number.length() - 4));
    }

    private PaymentResult chargeUpi(PlaceOrderRequest request) {
        String upiId = request.getUpiId() == null ? "" : request.getUpiId().trim();
        if (!UPI_ID.matcher(upiId).matches()) {
            throw new BadRequestException("Enter a valid UPI ID, for example name@bank");
        }
        return new PaymentResult(newReference("UPI"), null);
    }

    static boolean isFutureExpiry(String expiry) {
        if (expiry == null) return false;
        String[] parts = expiry.trim().split("/");
        if (parts.length != 2) return false;
        try {
            int month = Integer.parseInt(parts[0].trim());
            int year = Integer.parseInt(parts[1].trim());
            if (year < 100) year += 2000;
            if (month < 1 || month > 12) return false;
            return !YearMonth.of(year, month).isBefore(YearMonth.now());
        } catch (NumberFormatException e) {
            return false;
        }
    }

    static boolean passesLuhn(String digits) {
        int sum = 0;
        boolean doubleIt = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int d = digits.charAt(i) - '0';
            if (doubleIt) {
                d *= 2;
                if (d > 9) d -= 9;
            }
            sum += d;
            doubleIt = !doubleIt;
        }
        return sum % 10 == 0;
    }

    private static String digitsOnly(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }

    private static String newReference(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}
