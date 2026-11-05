package com.ibm.capstone.bookstore;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Boots the whole application on in-memory H2 with the real schema.sql and data.sql and
 * exercises it over HTTP, so SQL, JPA mapping and security rules are all covered.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BookstoreApiIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    private final ObjectMapper mapper = new ObjectMapper();
    private static String token;

    private String url(String path) {
        return "http://localhost:" + port + "/api/v1" + path;
    }

    private HttpHeaders headers(boolean auth) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        if (auth) h.setBearerAuth(token);
        return h;
    }

    private JsonNode call(HttpMethod method, String path, Object body, boolean auth, HttpStatus expected) throws Exception {
        String json = body == null ? null : mapper.writeValueAsString(body);
        ResponseEntity<String> response = rest.exchange(url(path), method, new HttpEntity<>(json, headers(auth)), String.class);
        assertEquals(expected, response.getStatusCode(), "unexpected status for " + method + " " + path + ": " + response.getBody());
        return response.getBody() == null || response.getBody().isBlank() ? null : mapper.readTree(response.getBody());
    }

    @Test
    @Order(1)
    @DisplayName("Catalog is public and the language, format, price and sort filters work")
    void catalogFilters() throws Exception {
        JsonNode all = call(HttpMethod.GET, "/books?size=50", null, false, HttpStatus.OK);
        assertEquals(15, all.get("totalElements").asInt());

        assertEquals(3, call(HttpMethod.GET, "/books?format=eBook", null, false, HttpStatus.OK).get("totalElements").asInt());
        assertEquals(0, call(HttpMethod.GET, "/books?language=Hindi", null, false, HttpStatus.OK).get("totalElements").asInt());
        assertEquals(1, call(HttpMethod.GET, "/books?query=openshift", null, false, HttpStatus.OK).get("totalElements").asInt());

        JsonNode cheap = call(HttpMethod.GET, "/books?maxPrice=700&size=50", null, false, HttpStatus.OK);
        for (JsonNode b : cheap.get("content")) assertTrue(b.get("price").asDouble() <= 700);

        JsonNode asc = call(HttpMethod.GET, "/books?sortBy=price_asc&size=50", null, false, HttpStatus.OK).get("content");
        assertTrue(asc.get(0).get("price").asDouble() <= asc.get(asc.size() - 1).get("price").asDouble());

        JsonNode facets = call(HttpMethod.GET, "/books/facets", null, false, HttpStatus.OK);
        assertTrue(facets.get("formats").toString().contains("Hardcover"));
    }

    @Test
    @Order(2)
    @DisplayName("Landing shelves return bestsellers, new launches and top rated")
    void shelves() throws Exception {
        JsonNode shelves = call(HttpMethod.GET, "/books/shelves?limit=3", null, false, HttpStatus.OK);
        assertEquals(3, shelves.get("bestsellers").size());
        assertEquals(3, shelves.get("newLaunches").size());
        assertEquals(3, shelves.get("topRated").size());
        // Designing Data-Intensive Applications has the most copies sold in the seed data
        assertEquals("Designing Data-Intensive Applications", shelves.get("bestsellers").get(0).get("title").asText());
    }

    @Test
    @Order(3)
    @DisplayName("Protected routes need a token; the demo user can log in")
    void authentication() throws Exception {
        call(HttpMethod.GET, "/cart", null, false, HttpStatus.UNAUTHORIZED);
        call(HttpMethod.GET, "/wishlist", null, false, HttpStatus.UNAUTHORIZED);

        JsonNode login = call(HttpMethod.POST, "/auth/login",
                Map.of("email", "alex.dev@ibm.com", "password", "Password123!"), false, HttpStatus.OK);
        token = login.get("token").asText();
        assertEquals(1500.0, login.get("user").get("walletBalance").asDouble());
    }

    @Test
    @Order(4)
    @DisplayName("Book detail includes writer profile, reviews and the caller's wishlist flag")
    void bookDetail() throws Exception {
        JsonNode book = call(HttpMethod.GET, "/books/5", null, true, HttpStatus.OK);
        assertEquals("Martin Kleppmann", book.get("writer").get("name").asText());
        assertTrue(book.get("writer").get("following").asBoolean());
        assertTrue(book.get("inWishlist").asBoolean());
        assertEquals("English", book.get("language").asText());
        assertTrue(book.get("reviews").size() >= 1);

        JsonNode anon = call(HttpMethod.GET, "/books/5", null, false, HttpStatus.OK);
        assertFalse(anon.get("inWishlist").asBoolean());
    }

    @Test
    @Order(5)
    @DisplayName("Wishlist and followed writers can be added and removed")
    void wishlistAndWriters() throws Exception {
        assertEquals(2, call(HttpMethod.GET, "/wishlist", null, true, HttpStatus.OK).size());
        call(HttpMethod.PUT, "/wishlist/9", null, true, HttpStatus.NO_CONTENT);
        call(HttpMethod.PUT, "/wishlist/9", null, true, HttpStatus.NO_CONTENT); // idempotent
        assertEquals(3, call(HttpMethod.GET, "/wishlist", null, true, HttpStatus.OK).size());
        call(HttpMethod.DELETE, "/wishlist/9", null, true, HttpStatus.NO_CONTENT);
        assertEquals(2, call(HttpMethod.GET, "/wishlist", null, true, HttpStatus.OK).size());

        assertEquals(15, call(HttpMethod.GET, "/writers", null, false, HttpStatus.OK).size());
        assertEquals(2, call(HttpMethod.GET, "/writers?followed=true", null, true, HttpStatus.OK).size());
        call(HttpMethod.GET, "/writers?followed=true", null, false, HttpStatus.UNAUTHORIZED);
        call(HttpMethod.PUT, "/writers/9/follow", null, true, HttpStatus.NO_CONTENT);
        assertEquals(3, call(HttpMethod.GET, "/writers?followed=true", null, true, HttpStatus.OK).size());
        call(HttpMethod.DELETE, "/writers/9/follow", null, true, HttpStatus.NO_CONTENT);
        JsonNode writer = call(HttpMethod.GET, "/writers/3", null, true, HttpStatus.OK);
        assertEquals(1, writer.get("books").size());
    }

    @Test
    @Order(6)
    @DisplayName("A review updates the book's rating once; a second review is refused")
    void reviews() throws Exception {
        Map<String, Object> review = Map.of("rating", 4, "comment", "Solid and practical.");
        JsonNode created = call(HttpMethod.POST, "/books/8/reviews", review, true, HttpStatus.CREATED);
        assertEquals(4, created.get("rating").asInt());
        call(HttpMethod.POST, "/books/8/reviews", review, true, HttpStatus.BAD_REQUEST);
        call(HttpMethod.POST, "/books/8/reviews", Map.of("rating", 9, "comment", "x"), true, HttpStatus.BAD_REQUEST);
        JsonNode book = call(HttpMethod.GET, "/books/8", null, true, HttpStatus.OK);
        assertEquals(311, book.get("reviewCount").asInt());
    }

    @Test
    @Order(7)
    @DisplayName("Cart totals use INR rules: 12% tax and free delivery from Rs 499")
    void cartTotals() throws Exception {
        JsonNode cart = call(HttpMethod.GET, "/cart", null, true, HttpStatus.OK);
        // seeded basket: 899 + 999 = 1898; tax 12% = 227.76; free delivery
        assertEquals(1898.0, cart.get("subtotal").asDouble());
        assertEquals(227.76, cart.get("estimatedTax").asDouble());
        assertEquals(0.0, cart.get("shippingFee").asDouble());
        assertEquals(2125.76, cart.get("totalAmount").asDouble());
        assertEquals(499.0, cart.get("freeShippingThreshold").asDouble());
    }

    @Test
    @Order(8)
    @DisplayName("Coupons validate against the basket")
    void coupons() throws Exception {
        JsonNode ok = call(HttpMethod.POST, "/coupons/validate", Map.of("code", "welcome10"), true, HttpStatus.OK);
        assertEquals(189.8, ok.get("discount").asDouble()); // 10% of 1898
        call(HttpMethod.POST, "/coupons/validate", Map.of("code", "NOPE"), true, HttpStatus.BAD_REQUEST);
    }

    @Test
    @Order(8)
    @DisplayName("The quote matches what checkout later charges")
    void quote() throws Exception {
        Map<String, Object> request = new java.util.HashMap<>();
        request.put("couponCode", "WELCOME10");
        request.put("pointsToRedeem", 100);
        JsonNode q = call(HttpMethod.POST, "/orders/quote", request, true, HttpStatus.OK);
        assertEquals(1898.0, q.get("subtotal").asDouble());
        assertEquals(189.8, q.get("couponDiscount").asDouble());
        assertEquals(204.98, q.get("tax").asDouble());
        assertEquals(100, q.get("pointsApplied").asInt());
        assertEquals(1813.18, q.get("payable").asDouble());
        assertEquals(18, q.get("pointsToEarn").asInt());

        // asking for far more points than allowed is capped, not rejected
        request.put("pointsToRedeem", 999999);
        assertEquals(250, call(HttpMethod.POST, "/orders/quote", request, true, HttpStatus.OK).get("pointsApplied").asInt());
    }

    @Test
    @Order(9)
    @DisplayName("Addresses require the full set of wireframe fields")
    void addresses() throws Exception {
        call(HttpMethod.POST, "/users/addresses", Map.of("recipientName", "A", "streetAddress", "x"), true, HttpStatus.BAD_REQUEST);
        Map<String, Object> address = Map.of(
                "recipientName", "Alex Rivera", "email", "alex.dev@ibm.com", "phoneNumber", "+91 98100 00144",
                "streetAddress", "12 MG Road", "addressLine2", "Floor 3", "city", "Pune",
                "state", "Maharashtra", "postalCode", "411001", "country", "India");
        JsonNode created = call(HttpMethod.POST, "/users/addresses", address, true, HttpStatus.CREATED);
        assertEquals("Floor 3", created.get("addressLine2").asText());
        assertEquals("alex.dev@ibm.com", created.get("email").asText());
    }

    @Test
    @Order(10)
    @DisplayName("A declined card fails the order and leaves the basket alone")
    void declinedCard() throws Exception {
        Map<String, Object> order = cardOrder("4000 0000 0000 0002");
        JsonNode error = call(HttpMethod.POST, "/orders", order, true, HttpStatus.BAD_REQUEST);
        assertTrue(error.get("message").asText().toLowerCase().contains("declined"));
        assertEquals(2, call(HttpMethod.GET, "/cart", null, true, HttpStatus.OK).get("items").size());
    }

    @Test
    @Order(11)
    @DisplayName("Checkout with coupon, points and a card succeeds; then the 48 hour cancel restores everything")
    void checkoutAndCancel() throws Exception {
        Map<String, Object> order = new java.util.HashMap<>(cardOrder("4242 4242 4242 4242"));
        order.put("couponCode", "WELCOME10");
        order.put("pointsToRedeem", 100);

        JsonNode confirmation = call(HttpMethod.POST, "/orders", order, true, HttpStatus.CREATED);
        // 1898 - 189.80 coupon = 1708.20; tax 12% = 204.98; total 1913.18; minus 100 points = 1813.18
        assertEquals(1813.18, confirmation.get("totalAmount").asDouble());
        assertEquals(189.8, confirmation.get("couponDiscount").asDouble());
        assertEquals(100, confirmation.get("pointsRedeemed").asInt());
        assertEquals(18, confirmation.get("pointsEarned").asInt());
        assertTrue(confirmation.get("paymentReference").asText().startsWith("CRD-"));
        assertEquals(2, confirmation.get("items").size());
        // card details are never echoed back
        assertFalse(confirmation.toString().contains("4242 4242"));

        assertEquals(0, call(HttpMethod.GET, "/cart", null, true, HttpStatus.OK).get("items").size());
        long orderId = confirmation.get("orderId").asLong();
        JsonNode detail = call(HttpMethod.GET, "/orders/" + orderId, null, true, HttpStatus.OK);
        assertEquals("WELCOME10", detail.get("couponCode").asText());
        assertEquals("4242", detail.get("cardLast4").asText());
        assertTrue(detail.get("canCancel").asBoolean());

        JsonNode me = call(HttpMethod.GET, "/auth/me", null, true, HttpStatus.OK);
        assertEquals(250 - 100 + 18, me.get("giftPointsBalance").asInt());

        JsonNode cancelled = call(HttpMethod.POST, "/orders/" + orderId + "/cancel", Map.of("reason", "Changed my mind"), true, HttpStatus.OK);
        assertEquals("CANCELLED", cancelled.get("orderStatus").asText());
        JsonNode after = call(HttpMethod.GET, "/auth/me", null, true, HttpStatus.OK);
        assertEquals(250, after.get("giftPointsBalance").asInt());
        call(HttpMethod.POST, "/orders/" + orderId + "/cancel", Map.of("reason", "again"), true, HttpStatus.BAD_REQUEST);

        // Buy it again puts the items back in the basket
        assertEquals(2, call(HttpMethod.POST, "/orders/" + orderId + "/reorder", null, true, HttpStatus.OK).get("items").size());
    }

    @Test
    @Order(12)
    @DisplayName("Wallet: top up, pay from the wallet, and get the money back on cancel")
    void wallet() throws Exception {
        call(HttpMethod.POST, "/wallet/top-up", Map.of("amount", 0), true, HttpStatus.BAD_REQUEST);
        JsonNode topped = call(HttpMethod.POST, "/wallet/top-up", Map.of("amount", 500), true, HttpStatus.OK);
        assertEquals(2000.0, topped.get("balance").asDouble());

        // basket holds 899 + 999 again from the reorder above: 2125.76 > 2000, so the wallet is short
        Map<String, Object> order = new java.util.HashMap<>();
        order.put("addressId", 1);
        order.put("paymentMethod", "WALLET");
        JsonNode error = call(HttpMethod.POST, "/orders", order, true, HttpStatus.BAD_REQUEST);
        assertTrue(error.get("message").asText().toLowerCase().contains("wallet"));

        call(HttpMethod.POST, "/wallet/top-up", Map.of("amount", 500), true, HttpStatus.OK);
        JsonNode confirmation = call(HttpMethod.POST, "/orders", order, true, HttpStatus.CREATED);
        assertEquals(2125.76, confirmation.get("walletAmountUsed").asDouble());
        assertEquals(374.24, confirmation.get("newWalletBalance").asDouble());

        call(HttpMethod.POST, "/orders/" + confirmation.get("orderId").asLong() + "/cancel", Map.of("reason", "test"), true, HttpStatus.OK);
        assertEquals(2500.0, call(HttpMethod.GET, "/wallet", null, true, HttpStatus.OK).get("balance").asDouble());
    }

    private Map<String, Object> cardOrder(String number) {
        String expiry = YearMonth.now().plusYears(2).format(DateTimeFormatter.ofPattern("MM/yy"));
        Map<String, Object> order = new java.util.HashMap<>();
        order.put("addressId", 1);
        order.put("paymentMethod", "CREDIT_CARD");
        order.put("cardNumber", number);
        order.put("cardName", "Alex Rivera");
        order.put("cardExpiry", expiry);
        order.put("cardCvv", "123");
        return order;
    }
}
