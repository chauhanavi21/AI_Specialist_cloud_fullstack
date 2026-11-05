# E-Bookstore Platform — Data Model & Domain Architecture

This document describes the schema as implemented in `backend/src/main/resources/schema.sql` (H2, used for local
development and tests) and `backend/src/main/resources/db/postgres/schema.sql` (PostgreSQL, generated from the
H2 file by a script so the two cannot drift). All money is in Indian rupees (INR).

## 1. Domain Entities & ER Schema

```mermaid
erDiagram
    USERS ||--o{ ADDRESSES : "has saved"
    USERS ||--o{ ORDERS : "places"
    USERS ||--o| CARTS : "owns"
    USERS ||--o{ GIFT_POINTS_LEDGER : "accrues/redeems"
    USERS ||--o{ WALLET_TRANSACTIONS : "tops up/pays"
    USERS ||--o{ WISHLIST_ITEMS : "saves"
    USERS ||--o{ AUTHOR_FOLLOWS : "follows"
    USERS ||--o{ REVIEWS : "writes"
    AUTHORS ||--o{ BOOKS : "writes"
    AUTHORS ||--o{ AUTHOR_FOLLOWS : "followed by"
    CATEGORIES ||--o{ BOOKS : "classifies"
    PUBLISHERS ||--o{ BOOKS : "publishes"
    BOOKS ||--o{ REVIEWS : "receives"
    BOOKS ||--o{ WISHLIST_ITEMS : "saved in"
    CARTS ||--o{ CART_ITEMS : "contains"
    BOOKS ||--o{ CART_ITEMS : "referenced by"
    ORDERS ||--o{ ORDER_ITEMS : "composed of"
    BOOKS ||--o{ ORDER_ITEMS : "referenced by"
    ADDRESSES ||--o{ ORDERS : "delivery target"
```

`coupons` is a standalone lookup table; an order stores the code and the discount it received.

---

## 2. Relational Schema Specification

### `users`
| Column | Type | Notes |
|---|---|---|
| `id` | `BIGINT` identity | Primary key |
| `email` | `VARCHAR(255)` | Unique, login credential |
| `password_hash` | `VARCHAR(255)` | BCrypt |
| `full_name`, `phone_number` | `VARCHAR` | Phone is optional |
| `gift_points_balance` | `INT` | New accounts start with 150 points |
| `wallet_balance` | `DECIMAL(10,2)` | Stored-value wallet, default 0 |
| `role`, `created_at` | | `ROLE_CUSTOMER` by default |

### `addresses`
`id`, `user_id` (FK, cascade delete), `recipient_name`, `email`, `phone_number`, `street_address`,
`address_line2` (optional), `city`, `state`, `postal_code` (PIN code), `country`, `is_default`.
These are the fields in the checkout wireframe (name, email, phone, address, address line 2, city, PIN, state, country).

### `categories`, `publishers`
- `categories`: `id`, `name` (unique), `slug` (unique), `description`, `book_count`.
- `publishers` (brands): `id`, `name` (unique), `code` (unique), `website`.

### `authors` (writers)
`id`, `name` (unique), `bio`, `photo_url` (optional; the UI draws an initials avatar when it is empty).

### `books`
| Column | Type | Notes |
|---|---|---|
| `id` | `BIGINT` identity | Primary key |
| `title`, `author` | `VARCHAR(255)` | `author` is display text and may list co-authors |
| `author_id` | FK -> `authors` | Primary writer profile |
| `isbn` | `VARCHAR(20)` | Unique |
| `category_id`, `publisher_id` | FK | Genre and brand |
| `price`, `original_price` | `DECIMAL(10,2)` | Sale price and list price, in INR |
| `stock_quantity` | `INT` | Inventory |
| `rating`, `review_count` | `DECIMAL(3,2)`, `INT` | Running average, updated when a review is posted |
| `cover_image_url` | `VARCHAR(500)` | Optional; the UI draws a typographic cover when empty |
| `description` | `TEXT` | Synopsis |
| `published_year`, `launch_date` | `INT`, `DATE` | `launch_date` drives the "New Launches" shelf |
| `page_count` | `INT` | |
| `book_language`, `book_format` | `VARCHAR` | Catalog filters (for example English, Paperback/Hardcover/eBook) |
| `copies_sold` | `INT` | Drives the "Bestsellers" shelf; incremented on purchase, decremented on cancellation |
| `tentative_delivery_days` | `INT` | Used to compute the delivery date shown on cards |
| `is_featured` | `BOOLEAN` | Default sort priority |

### `reviews`
`id`, `book_id`, `user_id`, `rating` (1 to 5), `comment_text` (max 300 stored, 100 allowed by the API),
`created_at`. Unique on (`book_id`, `user_id`): one review per customer per book.

### `wishlist_items`, `author_follows`
Join tables with `user_id` and `book_id` / `author_id`, `created_at`, and a unique constraint so that adding twice is a no-op.
They back **My Wishlist** and **My Writers**.

### `coupons`
`code` (unique), `description`, `discount_type` (`PERCENT` or `FLAT`), `discount_value`, `min_order_amount`,
`max_discount` (optional cap), `active`, `expires_at` (optional).

### `carts` & `cart_items`
- `carts`: `id`, `user_id` (unique), `created_at`, `updated_at`.
- `cart_items`: `id`, `cart_id`, `book_id`, `quantity`, `unit_price`, `created_at`.

### `orders` & `order_items`
- `orders`:
  - `id`, `order_number` (`BK-YYYYMMDD-NNNNNN`), `user_id`, `address_id`
  - `order_status` (`CONFIRMED`, `PROCESSING`, `SHIPPED`, `DELIVERED`, `CANCELLED`)
  - `payment_status` (`PAID`, `REFUNDED`, `FAILED`)
  - `subtotal`, `tax`, `shipping_fee`, `total_amount` (the amount actually paid, after coupon and points)
  - `coupon_code`, `coupon_discount`
  - `points_redeemed`, `points_discount`, `points_earned`
  - `wallet_amount_used`
  - `payment_method` (`CREDIT_CARD`, `DEBIT_CARD`, `UPI`, `WALLET`), `card_last4`, `payment_reference`
  - `estimated_delivery_date`, `created_at`, `cancelled_at`, `cancel_reason`
- `order_items`: `id`, `order_id`, `book_id`, `book_title`, `book_author`, `cover_image_url`, `quantity`, `unit_price`, `total_price`
  (title and author are copied so history stays correct if the catalog changes).

Card numbers, expiry dates and CVVs are **never stored**. Only the last four digits and a gateway reference are kept.

### `gift_points_ledger`
`id`, `user_id`, `order_id` (nullable), `points`, `type` (`EARNED`, `REDEEMED`, `REFUNDED`, `REVOKED`), `description`, `created_at`.

### `wallet_transactions`
`id`, `user_id`, `order_id` (nullable), `amount` (positive in, negative out), `type` (`TOP_UP`, `PAYMENT`, `REFUND`), `description`, `created_at`.

---

## 3. Core Business Rules

1. **Pricing** (one shared calculator is used by the basket, the quote endpoint and order placement, so the numbers cannot disagree):
   - Coupon discount comes off the subtotal first.
   - **Tax is 12%** of the discounted subtotal.
   - **Delivery is free** when the discounted subtotal is at least **₹499**; otherwise a flat **₹49**.
   - Rates are configurable under `app.pricing` in `application.yml`.

2. **48-Hour Order Cancellation Policy**:
   - A customer can cancel when `order_status` is `CONFIRMED` or `PROCESSING` and fewer than 48 hours have passed.
   - On cancellation:
     - `order_status` becomes `CANCELLED` and `payment_status` becomes `REFUNDED`.
     - Book stock is restored (and `copies_sold` reduced).
     - Redeemed gift points are returned (ledger type `REFUNDED`).
     - Points earned from the order are taken back (ledger type `REVOKED`), never below zero.
     - Any wallet money used is credited back (wallet type `REFUND`).

3. **Loyalty & Gift Points**:
   - Redeem at **1 point = ₹1** off. Redemption is capped at the amount payable.
   - Earn **1 point for every ₹100 paid** (`floor(amountPaid / 100)`).

4. **Payments** (a simulated gateway; no real money moves):
   - Cards: Luhn check, future expiry, 3 to 4 digit CVV, name required. Test card `4242 4242 4242 4242` succeeds;
     `4000 0000 0000 0002` is declined.
   - UPI: the ID must look like `name@bank`.
   - Wallet: the balance must cover the amount payable.
   - If gift points cover the whole order, no payment instrument is needed.
   - Everything is validated **before** stock, points or wallet are changed, so a failed payment leaves nothing half-done.

5. **Recommendations**:
   - **Related Reads** on a book: other books in the same category, best rated first; if there are none, the top rated books.
   - **Recommended for You**: books in categories the customer has bought from before that they have not bought yet,
     best rated first, topped up with the top rated books. (It uses categories, not authors.)

6. **Landing shelves**: Bestsellers (most `copies_sold`), New Launches (latest `launch_date`), and top rated.

7. **Buy It Again (1-click reorder)**: puts every line from a previous order back in the basket, with current stock checks.
