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
- Synthesized [`openapi.yaml`](openapi.yaml) with:
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

## 5a. Second review: the requirements on slide 4

The first reviewer asked for evidence that the app had run on a live cluster and for more on security. Rereading every slide, including the
diagram on slide 4, showed that its notes list requirements the first build never covered. The diagram is a picture whose text is dark on a
transparent background, so it was nearly invisible when the deck was opened on a dark theme; it was read by putting the image on white.

| Slide 4 note | Gap | Resolution |
|---|---|---|
| Guest / registered users, login and logout | No guest concept in the UI; logout only forgot the token in the browser | Entitlements by caller; `POST /auth/logout` revokes the token (`revoked_tokens`) |
| Role & entitlement | A `role` column that nothing checked | `ROLE_ADMIN` for `/admin`; members-only categories hidden from guests in every query path |
| Create stores, catalogs, store policies | No way to create anything | Stores, policies, categories, books and stock through an admin API and an Admin screen |
| Modify order | Only cancel | `PATCH /orders/{id}` with an exact proportional refund |
| Return order, return shipment, refund processing | Only 48-hour cancel | Returns with an RMA, a collection tracking number and a refund paid on receipt |
| Shipping rate calculation, delivery time, shipment | One flat fee, no shipment | Delivery zones with their own charge and transit time; one shipment per order with tracking |
| Up-sell and cross-sell | Only "related reads" | Two further lists, built from category, rating and what customers bought together |

### Which tool did this round (please read)
This round of work, covering the slide 4 features, the tests, the browser walk-through and these documents, was carried out with **Claude Code**
(Anthropic's coding agent), working from the capstone deck and the existing repository. **It was not done in IBM Bob**, and this journal does not claim
otherwise. The original build in sections 1 to 5 is described as done with Bob, as the author recorded it.

Prompts used on this round, in order:
1. Compare the capstone deck slide by slide with what the repository does, including requirements that appear only in diagrams, and mark each as done, partial or missing.
2. Build the missing slide 4 requirements, then test each one over real HTTP against the real schema and seed data.
3. Make any sequence of partial refunds add up to exactly what was paid, and prove it with a test.
4. Drive the real screens in a browser and report what is broken.
5. Write the security section from checks that were actually run, and state the limits.

> **For the author:** if you used IBM Bob for any part of this work, add those prompts and outputs here. Slide 2 and slide 11 ask for the agentic IDE to be
> demonstrated, and the video should say plainly which parts were done in Bob and which were not.

### What testing found (and what was not just trusted)
- Guests and customers on admin routes both got **401**; a signed-in customer should get **403**. The container's error forward was being treated as an unauthenticated request. Fixed with an explicit handler.
- The refund test was written first for the invariant (total refunded equals total paid), then the maths was built to satisfy it.
- Several tests passed when they should have failed. A security test passed with the signature check removed (the signature is checked in two places), a path test passed with its fix reverted (it sent no token), and a guard test compared against a mistyped constant. Each was found by **deliberately breaking the code and confirming the test turned red**, then strengthened. The mutation checks are recorded in the report's security section.
- Driving the real screens in a browser found two bugs the API tests could not: signing in did not refresh the catalogue, so a member kept seeing the guest list; and the admin forms overflowed a 375px phone.
- An unbounded in-memory map in the sign-in limiter, and a signing secret published in the repository, were found while writing the security section and fixed (see the report).
- Running the app on a **real PostgreSQL 16** found four things H2 never showed: a fresh database failed to start because the SQL scripts ran after Hibernate's validation; an order from before delivery zones existed crashed when modified (only an upgraded database has one); a signing secret made by the recommended `openssl` command was rejected on Windows because of a CRLF line ending; and the verification script itself passed against a leftover process and never really restarted the app. Each has a regression test or a check that fails without the fix.
- Reviewing the OpenShift manifest for what a schema cannot see found a Secret with placeholder values in the apply path, no container security contexts, no network isolation and `:latest` image tags. A network-policy probe was first written so that any failure printed "blocked, as intended"; it now distinguishes blocked, reachable and could-not-test.
- A stale server on port 8080 from an earlier session returned 401 for routes that did not exist in its old code. Lesson: confirm which process answers before believing a failing request.

## 5. Revision work: what changed

- **Backend**: authors, reviews, wishlist, follows, coupons and wallet tables; `PricingCalculator` as the single source of tax, delivery and points maths; `PaymentGateway` (simulated); `POST /orders/quote`; catalog filters and shelves.
- **Frontend**: dark theme, accessible `Modal`, toast notifications instead of `alert()`, generated typographic covers, wishlist and writers pages, and a checkout that mirrors slides 8 to 10.
- **Tests**: from 7 unit tests to 40 tests, including an integration test that boots the whole application on the real schema and seed data, and a browser walk-through of the full customer journey. Slide 4 work and the follow-up hardening brought this to 79 (see section 5a).
- **Docs**: OpenAPI, the Postman collection and the data model were regenerated from the code, and the spec was checked so that every served route is documented and every `$ref` resolves.
