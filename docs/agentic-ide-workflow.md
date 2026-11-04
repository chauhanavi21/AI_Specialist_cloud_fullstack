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
     - Loyalty gift points redemption formula (10 points = $1.00 USD).
     - Tentative delivery date calculation per item and order.
     - Collaborative/content-based recommendations powered by order history.

---

## 2. Step 2 & 3: OpenAPI 3.0 Specification Generation
### Objective
Generate a complete, standard-compliant OpenAPI 3.0 YAML specification for all required endpoints.

### Agentic Action in IBM Bob
- Synthesized [`openapi.yaml`](docs/openapi.yaml:1) with:
  - 14 distinct API endpoints across 6 resource groups (Auth, User Addresses, Loyalty Points, Catalog & Books, Shopping Cart, Orders & Checkout).
  - Explicit request & response JSON schemas, path parameters, query filters, pagination, and JWT bearer security schemes.
  - Granular error models (`400 Bad Request`, `401 Unauthorized`, `404 Not Found`, `409 Conflict`).

---

## 3. Step 4: Full-Stack Code Generation Strategy
- **Backend Architecture**: Spring Boot 3.3.x (Java 21), Spring Data JPA, Spring Security with JWT, PostgreSQL/H2 hybrid capability, validation with Jakarta Validation.
- **Frontend Architecture**: React 18, Vite, Tailwind CSS, Lucide icons, responsive and accessible UI.
- **Testing Strategy**: MockMvc integration tests, JUnit 5 service unit tests with Mockito, and Postman API contract collections.
