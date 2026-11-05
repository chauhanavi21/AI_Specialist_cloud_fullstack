# Agentic IDE Workflow & Prompting Journal (IBM Bob)

## Overview
This document logs the step-by-step application of **IBM Bob** as the Agentic IDE to design, specify, and build the E-Commerce Bookstore Platform for the Cloud FullStack AI Specialist Capstone.

---

## 1. Step 1: Wireframe & Journey Analysis (Slide 3–11)
### Objective
Extract core requirements, journey nodes, entities, and business constraints directly from the capstone specification presentation.

### Agentic Prompt & Synthesis
- **Prompt**:
  > *"Analyze the 12-step customer journey from the capstone presentation (E-Store Home, Catalog, Cart, Loyalty Points, Checkout, Payment Confirmation, Order History, Buy-in-Again, 48-Hour Order Cancellation). Derive the relational domain schema and business constraints."*
- **Outcome Generated**:
  1. Identified 8 core entities: `User`, `Address`, `Category`, `Publisher`, `Book`, `Cart`/`CartItem`, `Order`/`OrderItem`, `GiftPointsLedger`.
  2. Identified specific business logic:
     - 48-hour order cancellation window with automatic stock replenishment and points refund.
     - Loyalty gift points redemption formula (revised to 1 point = ₹1 when prices moved to rupees).
     - Tentative delivery date calculation per item and order.
     - Collaborative/content-based recommendations powered by order history.

---

## 2. Step 2 & 3: OpenAPI 3.0 Specification Generation
### Objective
Generate a complete, standard-compliant OpenAPI 3.0 YAML specification for all required endpoints.

### Agentic Action in IBM Bob
- Synthesized [`openapi.yaml`](docs/openapi.yaml:1) with:
  - The first version had 22 operations (17 paths) in 7 groups. The spec now has **37 operations on 30 paths** in 9 groups, adding Wishlist & Writers and Wallet & Coupons (see section 5).
  - Explicit request & response JSON schemas, path parameters, query filters, pagination, and JWT bearer security schemes.
  - Granular error models (`400 Bad Request`, `401 Unauthorized`, `404 Not Found`, `409 Conflict`).

---

## 3. Step 4: Full-Stack Code Generation Strategy
- **Backend Architecture**: Spring Boot 3.3.x (Java 21), Spring Data JPA, Spring Security with JWT, PostgreSQL/H2 hybrid capability, validation with Jakarta Validation.
- **Frontend Architecture**: React 18, Vite, Tailwind CSS, Lucide icons, responsive and accessible UI.
- **Testing Strategy**: JUnit 5 service unit tests with Mockito, and a Postman API collection.

---

## 4. Review against the capstone slides and gaps found

After the first build, the application was compared screen by screen with the wireframes on slides 5 to 10 and with the
journey on slide 3. The review found these gaps, which section 5 closes:

| Gap found | Wireframe | Resolution |
|---|---|---|
| Light blue theme, top category pills | Dark theme, left genre rail | Dark IBM-blue theme and a genre sidebar |
| Only sort, category and publisher filters | Search, language, format, price range, sort | Five-control filter bar, served by new `language`, `format` and price parameters |
| One recommendations shelf | Recommended, Bestsellers, New Launches | Three shelves from `/books/shelves` and `/recommendations/history` |
| No My Wishlist or My Writers | Both in the top navigation | New wishlist and followed-writers features |
| No writer profile or review form | "About the writer" and "Reviews" on the product page | Writer profile with follow, and a validated review form |
| Prices in dollars, delivery in days | ₹ prices and "Delivery by Mon, 21 Jul" | Everything in INR with real delivery dates |
| Short address form, no coupon, no tax line | Full address form, coupon box, tax and delivery lines | Full form, coupons, and a server-priced Grand Total |
| Fake disabled card field, "Net banking" | Credit, Debit, UPI and Wallet with real fields | Validated card and UPI fields, and a stored-value wallet |

A second pass looked for correctness and security problems in the code itself:

- The catalog search used `:param IS NULL` JPQL, which fails on PostgreSQL; it now builds predicates only for the filters supplied.
- The PostgreSQL profile reused the H2 scripts, which drop tables on every start and use H2-only syntax; it now has its own idempotent scripts.
- Checkout trusted a hard-coded test card; payments are now validated by a gateway class that checks the Luhn digit, expiry and CVV, and card data is never persisted.
- Stock and points were changed before payment was checked; all validation now happens first.
- Cancelling an order revoked earned points without a ledger entry; it now records a `REVOKED` entry.
- The Kubernetes manifest had no database, ran nginx as root on port 80 (which OpenShift forbids) and committed passwords; all three are fixed.

## 5. Revision work: what changed

- **Backend**: authors, reviews, wishlist, follows, coupons and wallet tables; `PricingCalculator` as the single source of tax, delivery and points maths; `PaymentGateway` (simulated); `POST /orders/quote`; catalog filters and shelves.
- **Frontend**: dark theme, accessible `Modal`, toast notifications instead of `alert()`, generated typographic covers, wishlist and writers pages, and a checkout that mirrors slides 8 to 10.
- **Tests**: from 7 unit tests to 40 tests, including an integration test that boots the whole application on the real schema and seed data, and a browser walk-through of the full customer journey.
- **Docs**: OpenAPI, the Postman collection and the data model were regenerated from the code, and the spec was checked so that every served route is documented and every `$ref` resolves.
