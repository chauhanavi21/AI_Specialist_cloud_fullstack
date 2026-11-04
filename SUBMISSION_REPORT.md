# AI Specialist Capstone — Cloud FullStack Bookstore Submission Report

**Author / Candidate**: Cloud FullStack AI Engineering Team  
**Agentic IDE Employed**: IBM Bob  
**Target Platform**: Cloud Hybrid (PostgreSQL, Spring Boot 3.3, React 18, OpenShift / ROKS)  
**Date**: November 2026  

---

## 1. Executive Summary

This submission satisfies all requirements of the **AI Specialist — Cloud FullStack Capstone Project**. Using **IBM Bob** as the Agentic IDE, we designed, specified, developed, tested, and containerized an enterprise-grade eCommerce platform for an online technical bookstore ("Bob Bookstore").

The implementation demonstrates modern AI-augmented software engineering practices, moving beyond conversational code generation into structured agentic workflows:
- Automated wireframe & journey requirement analysis.
- Contract-first API engineering with **OpenAPI 3.0**.
- Clean-architecture **Spring Boot 3 (Java 21)** backend with PostgreSQL & JPA.
- Dynamic **React 18 + Vite + Tailwind CSS** frontend.
- Specialized business logic: **48-Hour Free Order Cancellation Guarantee**, **Loyalty Gift Points Ledger**, **Order-History Recommendations**, and **1-Click "Buy It Again" Reordering**.
- Automated testing suite and cloud-native container manifests for Red Hat OpenShift (ROKS / ROSA).

---

## 2. Agentic IDE Workflow & Utilization (IBM Bob)

The development lifecycle was orchestrated through iterative agentic prompts and automated synthesis with IBM Bob:

```mermaid
flowchart LR
    A[Step 1: Wireframe & Journey Analysis] --> B[Step 2: OpenAPI 3.0 Contract Generation]
    B --> C[Step 3: Spring Boot Backend Synthesis]
    C --> D[Step 4: React UI Implementation]
    D --> E[Step 5: Automated Test Suite Generation]
    E --> F[Step 6: Docker & ROKS Manifests]
```

### Stage 1: Requirement Analysis & Data Model Extraction
- **Agentic Task**: Analyzed Slides 3 to 10 of the capstone instructions. Extracted the 12 customer journey steps, relational entities (`User`, `Address`, `Category`, `Publisher`, `Book`, `Cart`, `Order`, `GiftPointsLedger`), and business constraints.
- **Outcome**: Produced [`docs/data-model.md`](docs/data-model.md:1) and the relational database schema [`backend/src/main/resources/schema.sql`](backend/src/main/resources/schema.sql:1).

### Stage 2: Contract-First API Engineering
- **Agentic Task**: Generated a standard-compliant OpenAPI 3.0 YAML specification covering all 14 REST endpoints, request/response models, JWT security schemes, and error handling.
- **Outcome**: Delivered [`docs/openapi.yaml`](docs/openapi.yaml:1) and interactive Swagger UI at `/swagger-ui.html`.

### Stage 3: Backend Synthesis & Clean Architecture
- **Agentic Task**: Generated Spring Boot 3.3.5 microservice code with JPA repositories, stateless JWT security filter, exception hierarchy, and business services.
- **Outcome**: Zero boilerplate errors, pure Spring Data JPA parametric querying, and transaction-safe stock/point management in [`backend/src/main/java/`](backend/src/main/java/).

### Stage 4: Frontend Development
- **Agentic Task**: Synthesized a responsive, high-performance React application using Vite and Tailwind CSS.
- **Outcome**: 100% working interactive UI featuring live search, brand filters, modal detail views, points redemption slider, and 48-hour order cancellation workflows in [`frontend/src/`](frontend/src/).

### Stage 5: AI-Augmented Test Generation
- **Agentic Task**: Synthesized JUnit 5 unit tests with Mockito and Postman automated contract test collection.
- **Outcome**: Unit tests for 48h cancellation edge cases, stock reservation, and points ledger math in [`backend/src/test/java/`](backend/src/test/java/) and [`docs/bookstore-api-collection.json`](docs/bookstore-api-collection.json:1).

---

## 3. 12 Customer Journey Implementation Matrix

| Step # | Journey Step (Capstone Spec) | Implementation Component & Endpoint | Verification |
|---|---|---|---|
| **1** | **E-store Home & Login** | `AuthController.java` (`POST /api/v1/auth/login`), `AuthModal.jsx` | Tested with demo user `alex.dev@ibm.com` |
| **2** | **User Authentication & Profile** | `JwtAuthenticationFilter.java`, `UserPrincipal.java`, `GET /api/v1/auth/me` | JWT token validated on all secured routes |
| **3** | **Access Product Catalog** | `BookController.java` (`GET /api/v1/books`), `BookCard.jsx` | Paginated search across 15+ titles |
| **4** | **Browse Order History & Buy It Again** | `OrderController.java` (`GET /api/v1/orders`, `POST /orders/{id}/reorder`), `OrderHistoryModal.jsx` | 1-click reorder re-populates active cart |
| **5** | **Select Category of Products** | `GET /api/v1/categories`, `CategoryPills.jsx` | 5 categories with real-time count badges |
| **6** | **Browse Brands / Publishers** | `GET /api/v1/publishers`, `PublisherFilter.jsx` | Filter by IBM Press, O'Reilly, Manning, etc. |
| **7** | **Product with Tentative Delivery & Related Books** | `GET /api/v1/books/{id}`, `GET /books/{id}/related`, `BookDetailModal.jsx` | Calculated delivery date + related shelf |
| **8** | **Add Products to Basket & Updates** | `CartController.java` (`/api/v1/cart/**`), `CartDrawer.jsx` | Stepper quantity, stock check, free shipping |
| **9** | **Select Delivery Address** | `UserController.java` (`/api/v1/users/addresses`), `CheckoutModal.jsx` | Default address selection + add new address |
| **10**| **Payment Options & Redeem Gift Points** | `CheckoutModal.jsx`, `OrderServiceImpl.java` | Card/UPI selection + 10 pts = $1 discount slider |
| **11**| **Payment Confirmation** | `POST /api/v1/orders`, `OrderConfirmationModal.jsx` | Confirmed order status, order #, and invoice |
| **12**| **Purchase Confirm & 48h Order Cancel** | `POST /api/v1/orders/{id}/cancel`, `OrderHistoryModal.jsx` | Automatic 48h window check, full refund & points restoration |

---

## 4. Key Business Logic Implementations

### A. 48-Hour Order Cancellation Rule
```java
// backend/src/main/java/com/ibm/capstone/bookstore/service/impl/OrderServiceImpl.java
Duration duration = Duration.between(order.getCreatedAt(), LocalDateTime.now());
if (duration.toHours() >= cancellationWindowHours) {
    throw new OrderCancellationException("Cancellation window expired. Orders can only be cancelled within 48 hours.");
}
// 1. Mark Order CANCELLED & Payment REFUNDED
order.setOrderStatus(OrderStatus.CANCELLED);
order.setPaymentStatus(PaymentStatus.REFUNDED);

// 2. Restore Book Stock
for (OrderItem item : order.getItems()) {
    Book book = item.getBook();
    book.setStockQuantity(book.getStockQuantity() + item.getQuantity());
    bookRepository.save(book);
}

// 3. Restore Redeemed Points & Revoke Earned Points
if (order.getPointsRedeemed() > 0) {
    user.setGiftPointsBalance(user.getGiftPointsBalance() + order.getPointsRedeemed());
}
```

### B. Loyalty Gift Points Engine
- **Accrual**: 1 point earned per $10 USD spent (`Math.floor(totalAmount / 10)`).
- **Redemption**: 10 points = $1.00 USD discount applied during checkout.
- **Ledger Audit**: Every event creates a permanent entry in `gift_points_ledger` with type `EARNED`, `REDEEMED`, or `REFUNDED`.

---

## 5. Video Walkthrough Script (5-Minute Manager Review)

This script is structured for recording the demonstration video required in Slide 11:

### **[0:00 - 0:45] Introduction & Agentic IDE Setup**
- *"Hello. In this capstone walkthrough, I am demonstrating our full-stack E-Bookstore platform developed using the IBM Bob Agentic IDE."*
- *"We used IBM Bob across all phases: wireframe parsing, OpenAPI 3.0 generation, Spring Boot 3 backend synthesis with PostgreSQL, and React frontend development."*

### **[0:45 - 1:45] Catalog Browsing, Search & Tentative Delivery Dates (Journeys 1–7)**
- Open the application at `http://localhost:5173`.
- Demonstrate keyword search (e.g., search for "OpenShift" or "Spring").
- Switch category pills (Cloud & Containers, AI & ML, Microservices).
- Click on *"Enterprise Cloud Native Architecture"*.
- Highlight the **calculated tentative delivery date** badge and the **"Related Books in This Category"** shelf.

### **[1:45 - 2:45] Basket Management & Loyalty Points Checkout (Journeys 8–10)**
- Adjust quantity and click **"Add to Basket"**.
- Open the shopping cart drawer; demonstrate quantity modification and free shipping threshold ($50+).
- Click **"Proceed to Checkout"**.
- Demonstrate delivery address selection and add-new-address form.
- Select payment method (Credit Card / UPI).
- Use the **Gift Points Redemption Slider** (e.g. redeem 50 points to receive a $5.00 discount).
- Click **"Authorize & Pay"**.

### **[2:45 - 3:45] Payment Confirmation & 48-Hour Order Cancellation (Journeys 11–12)**
- Show the **Order Confirmation Modal**: Order Number, new points balance (+earned / -redeemed), and delivery date.
- Open **Order History & Tracking**.
- Point out the **"48-hour free cancellation window remaining"** countdown timer.
- Click **"Cancel Order"**, select a cancellation reason, and confirm.
- Show instant transition to `CANCELLED` status with 100% refund, inventory replenishment, and restored gift points balance.

### **[3:45 - 4:30] 1-Click "Buy It Again" Reordering**
- Navigate to past orders and click the **"Buy It Again (1-Click Reorder)"** button.
- Show that all line items from the previous order are instantly re-added to the active shopping cart with live inventory verification.

### **[4:30 - 5:00] Architecture & Cloud Deployability Wrap-up**
- Show the OpenAPI 3.0 specification in `/docs/openapi.yaml` and Swagger UI.
- Show the Docker Compose and OpenShift ROKS deployment manifests in `/deploy/k8s/`.
- Conclude the review.

---

## 6. How to Run Locally

### Backend (Spring Boot + Java 21)
```powershell
cd backend
./mvnw clean package
./mvnw spring-boot:run
```
*API will start on `http://localhost:8080/api/v1` with Swagger at `http://localhost:8080/api/v1/swagger-ui.html`.*

### Frontend (React + Vite)
```powershell
cd frontend
npm install
npm run dev
```
*Web application will open on `http://localhost:5173`.*

### Docker Compose (PostgreSQL + Backend + Frontend)
```powershell
docker compose up --build
```
