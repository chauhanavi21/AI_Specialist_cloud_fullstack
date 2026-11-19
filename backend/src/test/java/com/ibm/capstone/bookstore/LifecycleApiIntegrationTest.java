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
import org.springframework.test.annotation.DirtiesContext;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The requirements from slide 4 of the capstone deck: guest and member entitlements, roles, logout, stores and
 * policies, catalogue administration, shipments and rates, order modification, returns and refunds, and
 * up-sell / cross-sell. Everything runs over HTTP against the real schema and seed data.
 *
 * Seed data this relies on: alex.dev@ibm.com (customer, 250 points, Rs 1500 wallet) has order 1 (confirmed, in the
 * cancellation window) and order 2 (delivered yesterday, return window open); admin@bookworm.example is the administrator;
 * category 6 "Members Early Access" is members-only with books 16 and 17.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext
class LifecycleApiIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    @org.junit.jupiter.api.BeforeEach
    void useClientThatSupportsPatch() {
        rest.getRestTemplate().setRequestFactory(new org.springframework.http.client.HttpComponentsClientHttpRequestFactory());
    }

    private final ObjectMapper mapper = new ObjectMapper();
    private static String customer;
    private static String admin;
    private static long placedOrderId;

    private String url(String path) {
        return "http://localhost:" + port + "/api/v1" + path;
    }

    private JsonNode call(HttpMethod method, String path, Object body, String token, HttpStatus expected) throws Exception {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) h.setBearerAuth(token);
        String json = body == null ? null : mapper.writeValueAsString(body);
        ResponseEntity<String> r = rest.exchange(url(path), method, new HttpEntity<>(json, h), String.class);
        assertEquals(expected, r.getStatusCode(), method + " " + path + " -> " + r.getBody());
        return r.getBody() == null || r.getBody().isBlank() ? null : mapper.readTree(r.getBody());
    }

    private String login(String email) throws Exception {
        return call(HttpMethod.POST, "/auth/login", Map.of("email", email, "password", "Password123!"), null, HttpStatus.OK)
                .get("token").asText();
    }

    private Map<String, Object> card(long addressId) {
        return Map.of("addressId", addressId, "paymentMethod", "CREDIT_CARD", "cardNumber", "4242 4242 4242 4242",
                "cardName", "Alex Rivera", "cardExpiry", YearMonth.now().plusYears(2).format(DateTimeFormatter.ofPattern("MM/yy")),
                "cardCvv", "123");
    }

    private double balance(String token, String field) throws Exception {
        return call(HttpMethod.GET, "/auth/me", null, token, HttpStatus.OK).get(field).asDouble();
    }

    // =====================================================================================
    // Roles, entitlements, logout
    // =====================================================================================

    @Test
    @Order(1)
    @DisplayName("Guests see the public catalogue only; members also see members-only books; direct access is a 404, not a hint")
    void guestsAndMembersSeeDifferentCatalogues() throws Exception {
        customer = login("alex.dev@ibm.com");

        JsonNode guestBooks = call(HttpMethod.GET, "/books?size=50", null, null, HttpStatus.OK);
        JsonNode memberBooks = call(HttpMethod.GET, "/books?size=50", null, customer, HttpStatus.OK);
        assertEquals(15, guestBooks.get("totalElements").asInt());
        assertEquals(17, memberBooks.get("totalElements").asInt());

        assertEquals(5, call(HttpMethod.GET, "/categories", null, null, HttpStatus.OK).size());
        assertEquals(6, call(HttpMethod.GET, "/categories", null, customer, HttpStatus.OK).size());

        // A guest cannot open, relate to or get suggestions for a members-only book by guessing its id
        call(HttpMethod.GET, "/books/16", null, null, HttpStatus.NOT_FOUND);
        call(HttpMethod.GET, "/books/16/related", null, null, HttpStatus.NOT_FOUND);
        call(HttpMethod.GET, "/books/16/suggestions", null, null, HttpStatus.NOT_FOUND);
        assertEquals("Platform Engineering Field Notes (Members Early Access)",
                call(HttpMethod.GET, "/books/16", null, customer, HttpStatus.OK).get("title").asText());

        // ... and they never leak through shelves or writer pages either
        JsonNode shelves = call(HttpMethod.GET, "/books/shelves?limit=20", null, null, HttpStatus.OK);
        for (JsonNode b : shelves.get("newLaunches")) assertNotEquals("Members Early Access", b.get("categoryName").asText());
    }

    @Test
    @Order(2)
    @DisplayName("Admin routes need the admin role: guests get 401, customers get 403, admins get in")
    void roleEntitlement() throws Exception {
        admin = login("admin@bookworm.example");

        call(HttpMethod.GET, "/admin/overview", null, null, HttpStatus.UNAUTHORIZED);
        call(HttpMethod.GET, "/admin/overview", null, customer, HttpStatus.FORBIDDEN);
        JsonNode overview = call(HttpMethod.GET, "/admin/overview", null, admin, HttpStatus.OK);
        assertTrue(overview.get("books").asInt() >= 17);

        // Customers cannot reach the actuator internals, but health stays public for the platform's probes
        call(HttpMethod.GET, "/actuator/health", null, null, HttpStatus.OK);
        call(HttpMethod.GET, "/actuator/env", null, customer, HttpStatus.FORBIDDEN);
    }

    @Test
    @Order(3)
    @DisplayName("Logout revokes the token: it works before, is refused after, and logging out twice is harmless")
    void logout() throws Exception {
        String throwaway = login("sarah.connor@example.com");
        call(HttpMethod.GET, "/cart", null, throwaway, HttpStatus.OK);

        call(HttpMethod.POST, "/auth/logout", null, throwaway, HttpStatus.OK);
        call(HttpMethod.GET, "/cart", null, throwaway, HttpStatus.UNAUTHORIZED);
        call(HttpMethod.POST, "/auth/logout", null, throwaway, HttpStatus.OK);

        // A fresh login is unaffected, and the other session of the same user keeps working
        call(HttpMethod.GET, "/cart", null, login("sarah.connor@example.com"), HttpStatus.OK);
    }

    @Test
    @Order(4)
    @DisplayName("Repeated wrong passwords pause sign-in for that email only")
    void loginRateLimit() throws Exception {
        for (int i = 0; i < 5; i++) {
            call(HttpMethod.POST, "/auth/login", Map.of("email", "locked.out@example.com", "password", "wrong"), null, HttpStatus.UNAUTHORIZED);
        }
        JsonNode blocked = call(HttpMethod.POST, "/auth/login", Map.of("email", "locked.out@example.com", "password", "wrong"), null, HttpStatus.TOO_MANY_REQUESTS);
        assertTrue(blocked.get("message").asText().contains("Try again"));
        // another account is not affected
        login("alex.dev@ibm.com");
    }

    // =====================================================================================
    // Stores and policies
    // =====================================================================================

    @Test
    @Order(5)
    @DisplayName("Anyone can read store policies; only admins can create stores and policies")
    void storesAndPolicies() throws Exception {
        JsonNode stores = call(HttpMethod.GET, "/stores", null, null, HttpStatus.OK);
        assertEquals("Book Worm", stores.get(0).get("name").asText());
        assertTrue(stores.get(0).get("policies").size() >= 4);

        Map<String, Object> newStore = Map.of("name", "Book Worm Outlet", "tagline", "Clearance titles", "returnWindowDays", 14);
        call(HttpMethod.POST, "/admin/stores", newStore, customer, HttpStatus.FORBIDDEN);
        JsonNode created = call(HttpMethod.POST, "/admin/stores", newStore, admin, HttpStatus.CREATED);
        long id = created.get("id").asLong();
        call(HttpMethod.POST, "/admin/stores", newStore, admin, HttpStatus.BAD_REQUEST);   // duplicate name

        Map<String, Object> policy = Map.of("type", "RETURN", "title", "14-day returns", "body", "Outlet books can be returned within 14 days.");
        JsonNode withPolicy = call(HttpMethod.POST, "/admin/stores/" + id + "/policies", policy, admin, HttpStatus.CREATED);
        assertEquals(1, withPolicy.get("policies").size());
        call(HttpMethod.POST, "/admin/stores/" + id + "/policies", Map.of("type", "NONSENSE", "title", "x", "body", "y"), admin, HttpStatus.BAD_REQUEST);

        // a closed store disappears from the public list
        call(HttpMethod.PUT, "/admin/stores/" + id + "/active/false", null, admin, HttpStatus.OK);
        call(HttpMethod.GET, "/stores/" + id, null, null, HttpStatus.NOT_FOUND);
    }

    // =====================================================================================
    // Catalogue administration
    // =====================================================================================

    @Test
    @Order(6)
    @DisplayName("Admins create categories and books; ISBNs and names stay unique; counts stay honest; guests see only public stock")
    void catalogueAdministration() throws Exception {
        JsonNode category = call(HttpMethod.POST, "/admin/categories",
                Map.of("name", "Data Engineering", "description", "Pipelines and lakes", "visibility", "PUBLIC"), admin, HttpStatus.CREATED);
        long categoryId = category.get("id").asLong();
        assertEquals("data-engineering", category.get("slug").asText());
        call(HttpMethod.POST, "/admin/categories", Map.of("name", "data engineering"), admin, HttpStatus.BAD_REQUEST);
        call(HttpMethod.POST, "/admin/categories", Map.of("name", "Nope"), customer, HttpStatus.FORBIDDEN);

        Map<String, Object> book = new java.util.HashMap<>(Map.ofEntries(
                Map.entry("title", "Streaming Data Pipelines"), Map.entry("author", "Ada Okafor"),
                Map.entry("isbn", "978-1-000-00001-1"), Map.entry("categoryId", categoryId), Map.entry("publisherId", 2),
                Map.entry("price", 640), Map.entry("originalPrice", 800), Map.entry("stockQuantity", 12),
                Map.entry("description", "Kafka, Flink and the boring parts that keep them running."),
                Map.entry("publishedYear", 2026), Map.entry("pageCount", 300), Map.entry("tentativeDeliveryDays", 3)));
        JsonNode created = call(HttpMethod.POST, "/admin/books", book, admin, HttpStatus.CREATED);
        long bookId = created.get("id").asLong();
        assertEquals(1, call(HttpMethod.GET, "/categories", null, null, HttpStatus.OK).findValues("bookCount").stream()
                .filter(n -> n.asInt() == 1).count(), "new category now holds exactly one book");

        call(HttpMethod.POST, "/admin/books", book, admin, HttpStatus.BAD_REQUEST);                       // duplicate ISBN
        Map<String, Object> badPrice = new java.util.HashMap<>(book);
        badPrice.put("isbn", "978-1-000-00002-8");
        badPrice.put("originalPrice", 100);                                                              // original below selling price
        call(HttpMethod.POST, "/admin/books", badPrice, admin, HttpStatus.BAD_REQUEST);
        Map<String, Object> negative = new java.util.HashMap<>(book);
        negative.put("isbn", "978-1-000-00003-5");
        negative.put("stockQuantity", -4);
        call(HttpMethod.POST, "/admin/books", negative, admin, HttpStatus.BAD_REQUEST);

        // stock edits, and a members-only category hides its books from guests but not from the admin who made them
        assertEquals(0, call(HttpMethod.PUT, "/admin/books/" + bookId + "/stock", Map.of("stockQuantity", 0), admin, HttpStatus.OK).get("stockQuantity").asInt());
        JsonNode vip = call(HttpMethod.POST, "/admin/categories", Map.of("name", "Staff Picks", "visibility", "MEMBERS"), admin, HttpStatus.CREATED);
        Map<String, Object> vipBook = new java.util.HashMap<>(book);
        vipBook.put("isbn", "978-1-000-00004-2");
        vipBook.put("categoryId", vip.get("id").asLong());
        long vipId = call(HttpMethod.POST, "/admin/books", vipBook, admin, HttpStatus.CREATED).get("id").asLong();
        call(HttpMethod.GET, "/books/" + vipId, null, null, HttpStatus.NOT_FOUND);

        // cleanup rules: an empty category can go, one with books cannot
        call(HttpMethod.DELETE, "/admin/categories/" + categoryId, null, admin, HttpStatus.BAD_REQUEST);
        call(HttpMethod.DELETE, "/admin/books/" + bookId, null, admin, HttpStatus.NO_CONTENT);
        call(HttpMethod.DELETE, "/admin/categories/" + categoryId, null, admin, HttpStatus.NO_CONTENT);
        // a book that has been ordered cannot be deleted
        call(HttpMethod.DELETE, "/admin/books/3", null, admin, HttpStatus.BAD_REQUEST);
    }

    // =====================================================================================
    // Shipping rates, shipments, up-sell
    // =====================================================================================

    @Test
    @Order(7)
    @DisplayName("Shipping rates depend on the zone, and free delivery overrides them; no sign-in needed")
    void shippingRates() throws Exception {
        JsonNode local = call(HttpMethod.POST, "/shipping/quote", Map.of("city", "Bengaluru", "state", "Karnataka"), null, HttpStatus.OK);
        JsonNode regional = call(HttpMethod.POST, "/shipping/quote", Map.of("city", "Mysuru", "state", "Karnataka"), null, HttpStatus.OK);
        JsonNode neighbour = call(HttpMethod.POST, "/shipping/quote", Map.of("city", "Chennai", "state", "Tamil Nadu"), null, HttpStatus.OK);
        JsonNode national = call(HttpMethod.POST, "/shipping/quote", Map.of("city", "Delhi", "state", "Delhi"), null, HttpStatus.OK);
        assertEquals("LOCAL", local.get("zone").asText());
        assertEquals("REGIONAL", regional.get("zone").asText());
        assertEquals("REGIONAL", neighbour.get("zone").asText());
        assertEquals("NATIONAL", national.get("zone").asText());
        assertEquals(29.0, local.get("fee").asDouble());
        assertEquals(49.0, regional.get("fee").asDouble());
        assertEquals(79.0, national.get("fee").asDouble());
        assertTrue(national.get("extraTransitDays").asInt() > local.get("extraTransitDays").asInt());

        JsonNode free = call(HttpMethod.POST, "/shipping/quote", Map.of("city", "Delhi", "state", "Delhi", "subtotal", 1200), null, HttpStatus.OK);
        assertEquals(0.0, free.get("fee").asDouble());
        assertTrue(free.get("freeDelivery").asBoolean());
        call(HttpMethod.POST, "/shipping/quote", Map.of("city", "", "state", "Delhi"), null, HttpStatus.BAD_REQUEST);
    }

    @Test
    @Order(8)
    @DisplayName("Up-sell stays in the category and is a believable step up; cross-sell comes from other categories; guests get public books only")
    void upsellAndCrossSell() throws Exception {
        JsonNode s = call(HttpMethod.GET, "/books/3/suggestions?limit=4", null, null, HttpStatus.OK);
        JsonNode book = call(HttpMethod.GET, "/books/3", null, null, HttpStatus.OK);
        for (JsonNode up : s.get("upsell")) {
            assertEquals(book.get("categoryId").asLong(), up.get("categoryId").asLong(), "up-sell is the same category");
            assertTrue(up.get("price").asDouble() <= book.get("price").asDouble() * 1.4 + 0.01, "no more than 40% dearer");
            assertNotEquals(3, up.get("id").asInt());
        }
        assertTrue(s.get("crossSell").size() > 0);
        for (JsonNode x : s.get("crossSell")) {
            assertNotEquals(book.get("categoryId").asLong(), x.get("categoryId").asLong(), "cross-sell is a different category");
            assertNotEquals("Members Early Access", x.get("categoryName").asText());
        }

        JsonNode basket = call(HttpMethod.GET, "/recommendations/basket?bookIds=1,2&limit=3", null, customer, HttpStatus.OK);
        for (JsonNode x : basket.get("crossSell")) assertFalse(List.of(1, 2).contains(x.get("id").asInt()));
    }

    // =====================================================================================
    // Order lifecycle: modify, ship, track, return, refund
    // =====================================================================================

    @Test
    @Order(9)
    @DisplayName("Placing an order records the zone, fee, carrier and tracking number; the quote with an address matches what is charged")
    void placeOrderCreatesShipment() throws Exception {
        long address = call(HttpMethod.POST, "/users/addresses", Map.ofEntries(
                Map.entry("recipientName", "Alex Rivera"), Map.entry("email", "alex.dev@ibm.com"), Map.entry("phoneNumber", "+91 98100 00144"),
                Map.entry("streetAddress", "12 Connaught Place"), Map.entry("city", "Delhi"), Map.entry("state", "Delhi"),
                Map.entry("postalCode", "110001"), Map.entry("country", "India")), customer, HttpStatus.CREATED).get("id").asLong();

        call(HttpMethod.DELETE, "/cart", null, customer, HttpStatus.NO_CONTENT);
        call(HttpMethod.POST, "/cart", Map.of("bookId", 10, "quantity", 1), customer, HttpStatus.OK);   // 699
        call(HttpMethod.POST, "/cart", Map.of("bookId", 14, "quantity", 1), customer, HttpStatus.OK);   // 699 -> 1398, free delivery

        JsonNode quote = call(HttpMethod.POST, "/orders/quote", Map.of("addressId", address), customer, HttpStatus.OK);
        assertEquals("NATIONAL", quote.get("shippingZone").asText());
        assertEquals(0.0, quote.get("shippingFee").asDouble(), "free above the threshold even in the national zone");

        JsonNode order = call(HttpMethod.POST, "/orders", withAddress(card(address)), customer, HttpStatus.CREATED);
        assertEquals(quote.get("payable").asDouble(), order.get("totalAmount").asDouble(), 0.001, "the amount quoted is the amount charged");
        placedOrderId = order.get("orderId").asLong();

        JsonNode shipment = call(HttpMethod.GET, "/orders/" + placedOrderId + "/shipment", null, customer, HttpStatus.OK);
        assertEquals("PROCESSING", shipment.get("status").asText());
        assertEquals("NATIONAL", shipment.get("shippingZone").asText());
        assertTrue(shipment.get("trackingNumber").asText().startsWith("DL-"));
        assertTrue(shipment.get("events").size() >= 2);

        // a customer cannot see someone else's shipment
        call(HttpMethod.GET, "/orders/" + placedOrderId + "/shipment", null, login("sarah.connor@example.com"), HttpStatus.NOT_FOUND);
    }

    private Map<String, Object> withAddress(Map<String, Object> m) {
        return new java.util.HashMap<>(m);
    }

    @Test
    @Order(10)
    @DisplayName("Removing a book refunds its price, tax and points share exactly; the last book cannot be removed; cash and points reconcile")
    void modifyOrderRemovesBooks() throws Exception {
        double walletBefore = balance(customer, "walletBalance");
        double pointsBefore = balance(customer, "giftPointsBalance");
        JsonNode detail = call(HttpMethod.GET, "/orders/" + placedOrderId, null, customer, HttpStatus.OK);
        assertTrue(detail.get("canModify").asBoolean());
        long firstItem = detail.get("items").get(0).get("id").asLong();
        long secondItem = detail.get("items").get(1).get("id").asLong();
        double paid = detail.get("totalAmount").asDouble();

        // two books at 699: removing one is worth 699 + 12% tax = 782.88
        JsonNode result = call(HttpMethod.PATCH, "/orders/" + placedOrderId,
                Map.of("removeItems", List.of(Map.of("orderItemId", firstItem, "quantity", 1))), customer, HttpStatus.OK);
        assertEquals("PARTIALLY_REFUNDED", result.get("paymentStatus").asText());

        JsonNode after = call(HttpMethod.GET, "/orders/" + placedOrderId, null, customer, HttpStatus.OK);
        assertEquals(782.88, after.get("refundedAmount").asDouble(), 0.011);
        assertEquals(1, after.get("itemCount").asInt());
        assertTrue(paid - after.get("refundedAmount").asDouble() > 0, "something is still owed to the store");

        // too many, unknown item, nothing to change, and removing the last book are all refused
        call(HttpMethod.PATCH, "/orders/" + placedOrderId, Map.of("removeItems", List.of(Map.of("orderItemId", firstItem, "quantity", 1))), customer, HttpStatus.BAD_REQUEST);
        call(HttpMethod.PATCH, "/orders/" + placedOrderId, Map.of("removeItems", List.of(Map.of("orderItemId", 999999, "quantity", 1))), customer, HttpStatus.NOT_FOUND);
        call(HttpMethod.PATCH, "/orders/" + placedOrderId, Map.of(), customer, HttpStatus.BAD_REQUEST);
        call(HttpMethod.PATCH, "/orders/" + placedOrderId, Map.of("removeItems", List.of(Map.of("orderItemId", secondItem, "quantity", 1))), customer, HttpStatus.BAD_REQUEST);

        // another customer cannot touch this order
        call(HttpMethod.PATCH, "/orders/" + placedOrderId, Map.of("removeItems", List.of(Map.of("orderItemId", secondItem, "quantity", 1))),
                login("sarah.connor@example.com"), HttpStatus.NOT_FOUND);
        assertEquals(walletBefore, balance(customer, "walletBalance"), 0.001, "a card payment is refunded to the card, not the wallet");
        assertTrue(balance(customer, "giftPointsBalance") <= pointsBefore, "earned points shrink, never grow, when books come off");
    }

    @Test
    @Order(11)
    @DisplayName("Cancelling after a partial refund pays back only what is left, so the total refunded equals the total paid")
    void cancelAfterModificationRefundsTheRemainder() throws Exception {
        double paid = call(HttpMethod.GET, "/orders/" + placedOrderId, null, customer, HttpStatus.OK).get("totalAmount").asDouble();
        call(HttpMethod.POST, "/orders/" + placedOrderId + "/cancel", Map.of("reason", "Changed my mind"), customer, HttpStatus.OK);

        JsonNode cancelled = call(HttpMethod.GET, "/orders/" + placedOrderId, null, customer, HttpStatus.OK);
        assertEquals("CANCELLED", cancelled.get("orderStatus").asText());
        assertEquals("REFUNDED", cancelled.get("paymentStatus").asText());
        assertEquals(paid, cancelled.get("refundedAmount").asDouble(), 0.011, "refunded exactly what was paid, no more, no less");
        assertEquals("CANCELLED", call(HttpMethod.GET, "/orders/" + placedOrderId + "/shipment", null, customer, HttpStatus.OK).get("status").asText());
        call(HttpMethod.PATCH, "/orders/" + placedOrderId, Map.of("addressId", 1), customer, HttpStatus.BAD_REQUEST);   // cancelled orders are frozen
    }

    @Test
    @Order(12)
    @DisplayName("Only the store can ship; shipping and delivery follow the right order; a shipped order can no longer be edited or cancelled")
    void shippingLifecycle() throws Exception {
        call(HttpMethod.PUT, "/admin/orders/1/shipment", Map.of("status", "SHIPPED"), customer, HttpStatus.FORBIDDEN);
        call(HttpMethod.PUT, "/admin/orders/1/shipment", Map.of("status", "DELIVERED"), admin, HttpStatus.BAD_REQUEST);   // not shipped yet
        call(HttpMethod.PUT, "/admin/orders/1/shipment", Map.of("status", "BOGUS"), admin, HttpStatus.BAD_REQUEST);

        JsonNode shipped = call(HttpMethod.PUT, "/admin/orders/1/shipment", Map.of("status", "SHIPPED"), admin, HttpStatus.OK);
        assertEquals("SHIPPED", shipped.get("status").asText());
        call(HttpMethod.PUT, "/admin/orders/1/shipment", Map.of("status", "SHIPPED"), admin, HttpStatus.BAD_REQUEST);    // twice

        call(HttpMethod.POST, "/orders/1/cancel", Map.of("reason", "too late"), customer, HttpStatus.BAD_REQUEST);
        call(HttpMethod.PATCH, "/orders/1", Map.of("addressId", 1), customer, HttpStatus.BAD_REQUEST);
        // cancelled orders cannot be shipped
        call(HttpMethod.PUT, "/admin/orders/" + placedOrderId + "/shipment", Map.of("status", "SHIPPED"), admin, HttpStatus.BAD_REQUEST);
    }

    @Test
    @Order(13)
    @DisplayName("A delivered order can be returned inside the window; the refund is only paid, and the stock only restored, when the parcel arrives")
    void returnAndRefund() throws Exception {
        // order 2 was seeded as delivered yesterday: one copy of book 5 at 749 (+12% tax = 838.88)
        JsonNode eligibility = call(HttpMethod.GET, "/orders/2/return-eligibility", null, customer, HttpStatus.OK);
        assertTrue(eligibility.get("eligible").asBoolean());
        long item = eligibility.get("lines").get(0).get("orderItemId").asLong();
        int stockBefore = call(HttpMethod.GET, "/books/5", null, null, HttpStatus.OK).get("stockQuantity").asInt();
        double pointsBefore = balance(customer, "giftPointsBalance");

        // Order 1 was shipped but not delivered, so it cannot be returned yet
        assertFalse(call(HttpMethod.GET, "/orders/1/return-eligibility", null, customer, HttpStatus.OK).get("eligible").asBoolean());
        call(HttpMethod.POST, "/orders/1/returns", Map.of("items", List.of(Map.of("orderItemId", 1, "quantity", 1)), "reason", "early"), customer, HttpStatus.BAD_REQUEST);

        // validation: reason required, quantity within what was bought
        call(HttpMethod.POST, "/orders/2/returns", Map.of("items", List.of(Map.of("orderItemId", item, "quantity", 1)), "reason", ""), customer, HttpStatus.BAD_REQUEST);
        call(HttpMethod.POST, "/orders/2/returns", Map.of("items", List.of(Map.of("orderItemId", item, "quantity", 5)), "reason", "too many"), customer, HttpStatus.BAD_REQUEST);
        call(HttpMethod.POST, "/orders/2/returns", Map.of("items", List.of(), "reason", "nothing"), customer, HttpStatus.BAD_REQUEST);
        call(HttpMethod.POST, "/orders/2/returns", Map.of("items", List.of(Map.of("orderItemId", item, "quantity", 1)), "reason", "mine?"),
                login("sarah.connor@example.com"), HttpStatus.NOT_FOUND);

        JsonNode ret = call(HttpMethod.POST, "/orders/2/returns",
                Map.of("items", List.of(Map.of("orderItemId", item, "quantity", 1)), "reason", "Wrong edition"), customer, HttpStatus.CREATED);
        long returnId = ret.get("id").asLong();
        assertEquals("REQUESTED", ret.get("status").asText());
        assertTrue(ret.get("rmaNumber").asText().startsWith("RMA-"));
        assertTrue(ret.get("returnTrackingNumber").asText().startsWith("RT-"));
        assertTrue(ret.get("refundAmount").isNull(), "nothing is refunded just because the customer asked");
        assertEquals(838.88, ret.get("estimatedRefund").asDouble(), 0.011, "the customer is told what to expect");

        // Asking is not enough: nothing has been refunded or restocked yet, and the same book cannot be returned twice
        assertEquals(stockBefore, call(HttpMethod.GET, "/books/5", null, null, HttpStatus.OK).get("stockQuantity").asInt());
        assertEquals(pointsBefore, balance(customer, "giftPointsBalance"), 0.001);
        call(HttpMethod.POST, "/orders/2/returns", Map.of("items", List.of(Map.of("orderItemId", item, "quantity", 1)), "reason", "again"), customer, HttpStatus.BAD_REQUEST);

        // Customers cannot process their own refund
        call(HttpMethod.POST, "/admin/returns/" + returnId + "/receive", Map.of("note", "ok"), customer, HttpStatus.FORBIDDEN);

        call(HttpMethod.POST, "/admin/returns/" + returnId + "/pickup", null, admin, HttpStatus.OK);
        call(HttpMethod.POST, "/admin/returns/" + returnId + "/pickup", null, admin, HttpStatus.BAD_REQUEST);       // twice
        call(HttpMethod.DELETE, "/returns/" + returnId, null, customer, HttpStatus.BAD_REQUEST);                    // too late to withdraw

        JsonNode done = call(HttpMethod.POST, "/admin/returns/" + returnId + "/receive", Map.of("note", "Parcel intact"), admin, HttpStatus.OK);
        assertEquals("REFUNDED", done.get("status").asText());
        assertEquals(838.88, done.get("refundAmount").asDouble(), 0.011);
        assertTrue(done.get("refundReference").asText().startsWith("RFD-"));
        call(HttpMethod.POST, "/admin/returns/" + returnId + "/receive", Map.of(), admin, HttpStatus.BAD_REQUEST);   // cannot refund twice

        assertEquals(stockBefore + 1, call(HttpMethod.GET, "/books/5", null, null, HttpStatus.OK).get("stockQuantity").asInt(), "stock restored on receipt");
        assertEquals("RETURNED", call(HttpMethod.GET, "/orders/2", null, customer, HttpStatus.OK).get("orderStatus").asText());
        assertEquals("REFUNDED", call(HttpMethod.GET, "/orders/2", null, customer, HttpStatus.OK).get("paymentStatus").asText());
        assertTrue(balance(customer, "giftPointsBalance") < pointsBefore, "the points earned on the returned book are taken back");
        assertEquals(1, call(HttpMethod.GET, "/returns", null, customer, HttpStatus.OK).size());
        call(HttpMethod.GET, "/orders/2/return-eligibility", null, customer, HttpStatus.OK);
    }

    @Test
    @Order(14)
    @DisplayName("A return can be withdrawn before pickup, and the admin can reject one")
    void withdrawAndReject() throws Exception {
        // Deliver order 1 so it becomes returnable
        call(HttpMethod.PUT, "/admin/orders/1/shipment", Map.of("status", "DELIVERED"), admin, HttpStatus.OK);
        JsonNode eligibility = call(HttpMethod.GET, "/orders/1/return-eligibility", null, customer, HttpStatus.OK);
        assertTrue(eligibility.get("eligible").asBoolean());
        long item = eligibility.get("lines").get(0).get("orderItemId").asLong();

        JsonNode first = call(HttpMethod.POST, "/orders/1/returns",
                Map.of("items", List.of(Map.of("orderItemId", item, "quantity", 1)), "reason", "Changed my mind"), customer, HttpStatus.CREATED);
        call(HttpMethod.DELETE, "/returns/" + first.get("id").asLong(), null, customer, HttpStatus.OK);
        assertEquals("REJECTED", call(HttpMethod.GET, "/returns/" + first.get("id").asLong(), null, customer, HttpStatus.OK).get("status").asText());

        // withdrawing frees the book to be returned again, and an admin can then turn it down
        JsonNode second = call(HttpMethod.POST, "/orders/1/returns",
                Map.of("items", List.of(Map.of("orderItemId", item, "quantity", 1)), "reason", "Pages missing"), customer, HttpStatus.CREATED);
        JsonNode rejected = call(HttpMethod.POST, "/admin/returns/" + second.get("id").asLong() + "/reject", Map.of("note", "Item shows signs of use"), admin, HttpStatus.OK);
        assertEquals("REJECTED", rejected.get("status").asText());
        assertEquals("Item shows signs of use", rejected.get("adminNote").asText());
        assertEquals("DELIVERED", call(HttpMethod.GET, "/orders/1", null, customer, HttpStatus.OK).get("orderStatus").asText(), "a rejected return leaves the order as it was");
        JsonNode allReturns = call(HttpMethod.GET, "/admin/returns?all=true", null, admin, HttpStatus.OK);
        assertEquals(3, allReturns.size(), "the refunded return plus the withdrawn and the rejected one");
        assertEquals(0, call(HttpMethod.GET, "/admin/returns", null, admin, HttpStatus.OK).size(), "no return is still open");
    }
}
