# Cart Checkout + Mock Payment System

A single-service Spring Boot application that models a small e-commerce checkout flow with mock payment processing.

The focus of this project is not scale. The focus is correctness: order state transitions, safe payment retries, duplicate webhook handling, and avoiding double stock/payment operations.

---

## Application Features

The application supports the following flow:

1. View seeded products
2. Create a cart
3. Add, update, or remove cart items
4. Checkout the cart into an order
5. Start a payment for the order
6. Trigger a mock payment result: `CONFIRMED` or `FAILED`
7. Process the payment result through a webhook endpoint
8. Keep the order/payment state correct even if the same webhook is sent more than once

---

## Tech stack

- Java 21
- Spring Boot 4.0.6
- Maven
- Spring Web MVC
- Spring Data JPA
- Spring Validation
- H2 Database
- Lombok
- MapStruct
- Springdoc OpenAPI
- JUnit 5 / AssertJ / MockMvc

---

## Architecture

The project uses a layered architecture with domain methods protecting the important business rules.

```text
Controller layer       -> REST endpoints only
Application layer      -> transaction boundaries and orchestration
Domain layer           -> state transitions and business invariants
Repository layer       -> database access
```

I intentionally did not use Spring Statemachine. The order/payment flows are small enough to keep the state rules explicit inside the domain model, which makes the behavior easier to read, test, and explain.

---

## Main packages

```text
com.codequests.checkout
├── product       # Seeded product catalog
├── cart          # Cart and cart item management
├── order         # Checkout and order state machine
├── payment       # Payment start and webhook handling
├── mockprovider  # Mock provider endpoint that calls the webhook API
└── shared        # Common exceptions, base entity, config, constants.
```

---

## Domain model

### Product

A product has a name, description, price, and available stock quantity.

Stock is not deducted when an item is added to the cart. It is deducted during checkout only. This avoids blocking stock for abandoned carts.

### Cart

A cart starts as `OPEN`.

Items can be added, updated, or removed while the cart is open. Once checkout succeeds, the cart becomes `CHECKED_OUT` and can no longer be modified.

Adding the same product twice merges the quantity instead of creating duplicate cart rows.

In case of adding cart item with quantity 0, the item is removed from the cart. This allows clients to remove items by setting quantity to 0.

### Order

An order is created from a cart during checkout.

The order stores:

- the related cart
- order status
- total amount
- order items

`OrderItem` references the product and stores the unit price at checkout time. The price snapshot is important because payment should be based on the price at the time the order was placed.

### Payment

A payment belongs to an order.

If a payment fails, retrying creates a new payment row. The failed payment remains in the database as a previous attempt.

A payment has an amount copied from the order total when payment starts. This is intentional because payment records should show what amount was attempted.

---

## Order state machine

Allowed order states:

```text
CREATED
PENDING_PAYMENT
PAYMENT_FAILED
PAID
```

Allowed transitions:

```text
CREATED          -> PENDING_PAYMENT
PENDING_PAYMENT  -> PAID
PENDING_PAYMENT  -> PAYMENT_FAILED
PAYMENT_FAILED   -> PENDING_PAYMENT
```

Invalid transitions are rejected by domain methods. For example, an order cannot move directly from `CREATED` to `PAID`, and a `PAID` order cannot move back to `PAYMENT_FAILED`.

---

## Payment state machine

Allowed payment states:

```text
PENDING
CONFIRMED
FAILED
```

Rules:

- `PENDING` can become `CONFIRMED`
- `PENDING` can become `FAILED`
- `CONFIRMED` is final
- `FAILED` is final
- a failed payment cannot later become confirmed
- a confirmed payment cannot later become failed

This protects the case where a user receives a failed payment result but a late success event arrives for the same payment attempt. That late event is ignored.

---

## Payment and webhook safety

### Starting payment

When `POST /orders/{orderId}/payment/start` is called:

- If the order is `CREATED`, a new pending payment is created.
- If the order is `PAYMENT_FAILED`, a new pending payment is created as a retry.
- If the order is already `PENDING_PAYMENT`, the existing pending payment is returned.
- If the order is `PAID`, the request is rejected.

This prevents duplicate active payments.

### Webhook handling

The mock provider eventually sends a result to:

```http
POST /payments/webhook
```

Webhook behavior:

| Current payment status | Incoming result | Behavior |
|---|---|---|
| `PENDING` | `CONFIRMED` | Payment becomes `CONFIRMED`, order becomes `PAID` |
| `PENDING` | `FAILED` | Payment becomes `FAILED`, order becomes `PAYMENT_FAILED` |
| `CONFIRMED` | `CONFIRMED` | Duplicate webhook ignored safely |
| `FAILED` | `FAILED` | Duplicate webhook ignored safely |
| `CONFIRMED` | `FAILED` | Ignored because payment is already final |
| `FAILED` | `CONFIRMED` | Ignored because payment is already final |

For this small mock provider, idempotency is handled using the payment status itself because the webhook carries the internal `paymentId`.

---

## Important design decisions

### Database choice

H2 is used so the reviewer can run the application without Docker or external database setup. This is a deliberate assessment-friendly choice. For production, PostgreSQL with Flyway or Liquibase migrations would be preferred.

### Product catalog and pricing

A seeded product table is used instead of accepting client-provided product data. 
The client cannot be trusted to send the price—the system reads the product price from the database during checkout to prevent price manipulation.

### Stock management

Stock is validated when adding to cart, but only deducted during checkout. 
This avoids reserving stock indefinitely for abandoned carts.

### Idempotency

- **Checkout**: Checking out the same cart twice returns the existing order, preventing duplicate orders and double stock deduction.
- **Payment start**: Starting payment while one is already pending returns the existing payment, preventing duplicate active payments.

### Concurrency protection

`@Version` (optimistic locking) is used on Cart, Order, and Payment entities. This is critical for stock deduction during checkout—if two concurrent checkouts attempt to update the same product stock, one will fail and retry, preventing inconsistent stock updates.

### Mapping strategy

MapStruct is used only for entity-to-DTO mapping. Business logic, state transitions, and validations remain in domain classes and services—not in mappers.

---

## Edge cases covered

The application handles various edge cases and failure scenarios:

**Validation errors:**
- Product not found
- Insufficient stock (when adding to cart or at checkout)
- Cart item not found in the selected cart
- Cannot modify checked-out cart
- Empty cart checkout

**Idempotency scenarios:**
- Checkout same cart twice → returns existing order
- Start payment twice while pending → returns existing payment
- Duplicate webhook result → safely ignored

**Payment flow edge cases:**
- Payment failure followed by retry → creates new payment
- Opposite webhook after payment is final → ignored (e.g., FAILED event after CONFIRMED)
- State transition violations → rejected (e.g., PAID order cannot move to PAYMENT_FAILED)

---

## Known trade-offs

- JPA schema generation and `data.sql` are used for simplicity. Production should use versioned migrations (Flyway/Liquibase).
- Webhook idempotency relies on `Payment.status` since the mock provider sends the internal payment ID. A real provider should use external event IDs stored in a separate audit table.
- Product data is seeded and static. Product management APIs are intentionally out of scope.
- Authentication and authorization are not included—the task focuses on checkout/payment correctness.
- Currency is assumed to be USD and is not stored separately.

---

## AI usage disclosure

AI tools were used as an assistant during planning and implementation, but not as a replacement for the design decisions.

### How AI was used

AI was used as a **design review assistant** and **productivity tool**, not as a replacement for architectural decisions or critical thinking.

Specifically, AI helped with:

- **Architecture validation**: Discussing trade-offs between layered architecture vs. alternatives
- **Edge case identification**: Brainstorming concurrent scenarios and race conditions
- **State machine verification**: Reviewing order/payment transition rules for completeness
- **Test scenario generation**: Suggesting test cases for webhook idempotency and retry logic
- **Boilerplate acceleration**: Generating repetitive code (DTOs, mappers, documentation, postman collection)

### Example AI prompts used

I used AI mainly to review and challenge my design decisions, not to blindly generate the solution. I provided the intended architecture, domain model direction, and payment flow, then used AI to discuss alternatives, identify missing edge cases, and make sure the implementation matched the task requirements.

Examples of prompts used:

- I want to design this application using layered architecture with domain-level state transitions. Challenge the design and tell me what can go wrong.
- Review this order/payment state model and help me identify invalid transitions that should be prevented.
- Given this payment retry flow, what edge cases should I handle for duplicate webhooks, failed payments, and payment confirmation?
- Create a Postman flow that demonstrates the required task scenarios: checkout, payment failure and retry, duplicate webhook, and idempotency.
### Decisions made by me

The following decisions were made intentionally after evaluating alternatives:

- Use layered architecture with domain methods for state transitions.
- Use a manual state machine.
- Use H2 to make the project easy for reviewers to run.
- Add a Product table instead of trusting client-provided prices.
- Deduct stock only during checkout.
- Make checkout idempotent by returning the existing order for a checked-out cart.
- Make payment start idempotent by returning the existing pending payment.
- Treat failed and confirmed payments as final states.
- Use MapStruct only for DTO mapping, not business logic.
- Make the mock provider call the webhook API over HTTP to better represent a real webhook callback.

### What I would improve in a real system

For a production payment integration, I would add:

- Order Cancellation and Refunds
- provider payment IDs
- provider event IDs
- a payment event/audit table
- stronger authentication for webhooks
- a production database such as PostgreSQL
- structured logging and correlation IDs

---

## API endpoints

### Products

```http
GET /products
GET /products/{productId}
```

### Carts

```http
POST   /carts
GET    /carts/{cartId}
POST   /carts/{cartId}/items
PUT    /carts/{cartId}/items/{itemId}
DELETE /carts/{cartId}/items/{itemId}
POST   /carts/{cartId}/checkout
```

### Orders

```http
GET /orders/{orderId}
```

### Payments

```http
POST /orders/{orderId}/payment/start
GET  /payments/{paymentId}
POST /payments/webhook
```

### Mock provider

```http
POST /mock-provider/payments/{paymentId}/result
```

The mock provider endpoint calls the webhook endpoint over HTTP to simulate how a real payment provider would notify the application.

---

## Example flow

### 1. Get products

```http
GET /products
```

### 2. Create cart

```http
POST /carts
```

Example response:

```json
{
  "id": 1,
  "status": "OPEN",
  "items": [],
  "totalItems": 0
}
```

### 3. Add item

```http
POST /carts/1/items
Content-Type: application/json

{
  "productId": 1,
  "quantity": 2
}
```

### 4. Checkout

```http
POST /carts/1/checkout
```

Example response:

```json
{
  "id": 1,
  "cartId": 1,
  "status": "CREATED",
  "totalAmount": 240.00,
  "items": [
    {
      "id": 1,
      "productId": 1,
      "productName": "Mechanical Keyboard",
      "quantity": 2,
      "unitPrice": 120.00,
      "lineTotal": 240.00
    }
  ]
}
```

### 5. Start payment

```http
POST /orders/1/payment/start
```

Example response:

```json
{
  "id": 1,
  "orderId": 1,
  "status": "PENDING",
  "amount": 240.00
}
```

### 6. Trigger mock payment result

```http
POST /mock-provider/payments/1/result
Content-Type: application/json

{
  "result": "CONFIRMED"
}
```

Example response:

```json
{
  "paymentId": 1,
  "paymentStatus": "CONFIRMED",
  "orderId": 1,
  "orderStatus": "PAID",
  "message": "Payment confirmed successfully"
}
```

---

## Running the application

### Prerequisites

- Java 21
- Maven 3.9+

### Start the app

```bash
mvn spring-boot:run
```

Application URL:

```text
http://localhost:8080
```

Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```

H2 connection details:

```text
JDBC URL: jdbc:h2:file:./data/checkoutdb
Username: sa
Password: <empty>
```

---

## Initial data

The application starts with 5 seeded products:

| ID | Product | Price | Stock |
|---:|---|---:|---:|
| 1 | Mechanical Keyboard | 120.00 | 15 |
| 2 | Wireless Mouse | 45.00 | 30 |
| 3 | USB-C Hub | 65.00 | 20 |
| 4 | Laptop Stand | 35.00 | 25 |
| 5 | Noise Cancelling Headphones | 180.00 | 10 |

---

## Running tests

Run all tests:

```bash
mvn test
```

The test suite covers domain logic, state transitions, and integration scenarios including order/payment flows, cart operations, idempotency, stock validation, and webhook handling.

Integration tests call `/payments/webhook` directly. The mock provider endpoint is primarily used in the Postman collection to demonstrate HTTP-based webhook simulation.

---

## Postman collection

A Postman collection is included demonstrating key flows:

1. **Happy path**: create cart → add items → checkout → start payment → confirm payment
2. **Payment failure and retry**: fail payment → start new payment → confirm retry
3. **Duplicate webhook handling**: send same confirmed result multiple times
4. **Cart modification and validation edge cases**
5. **Direct webhook API checks**

---

## Error response format

Example validation/business error:

```json
{
  "errorCode": "INSUFFICIENT_STOCK",
  "message": "Requested quantity 20 exceeds available quantity 15 for product 1",
  "fieldErrors": []
}
```

Example field validation error:

```json
{
  "errorCode": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "fieldErrors": [
    {
      "field": "quantity",
      "message": "quantity must be at least 1"
    }
  ]
}
```

