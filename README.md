# DreamShops

[![CI](https://github.com/rajatjc/DreamShops/actions/workflows/ci.yml/badge.svg)](https://github.com/rajatjc/DreamShops/actions/workflows/ci.yml)

A shopping-cart REST backend built with Spring Boot 4 and Java 21. It covers a product catalog with
categories and images, customer accounts with JWT login, per-user carts, and checkout that keeps stock
consistent. Data lives in PostgreSQL.

## Run it

With Docker:

```bash
docker compose up --build
```

The API is on http://localhost:9193 and Swagger UI is at http://localhost:9193/swagger-ui.html. An admin
account `admin@dreamshops.local` / `admin-password` is created on first start; change it with
`ADMIN_EMAIL` / `ADMIN_PASSWORD`.

Without Docker for the app, start only the database and run the app from the wrapper:

```bash
docker compose up -d db
JWT_SECRET=$(openssl rand -hex 32) ADMIN_EMAIL=me@example.com ADMIN_PASSWORD=change-me ./mvnw spring-boot:run
```

### A quick tour

```bash
API=http://localhost:9193/api/v1
ADMIN=$(curl -s $API/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"admin@dreamshops.local","password":"admin-password"}' | jq -r .data.token)

# Admin adds a product (the category is created if it doesn't exist)
curl -s $API/products -H "Authorization: Bearer $ADMIN" -H 'Content-Type: application/json' \
  -d '{"name":"Espresso Machine","brand":"Breville","price":699.95,"inventory":3,"category":"Kitchen"}'

# A customer signs up, logs in, fills a cart and checks out
curl -s $API/auth/register -H 'Content-Type: application/json' \
  -d '{"firstName":"Pat","lastName":"Doe","email":"pat@example.com","password":"password-123"}'
PAT=$(curl -s $API/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"pat@example.com","password":"password-123"}' | jq -r .data.token)
curl -s $API/cart/items -H "Authorization: Bearer $PAT" -H 'Content-Type: application/json' -d '{"productId":1,"quantity":2}'
curl -s -X POST $API/orders -H "Authorization: Bearer $PAT"
```

## API

All paths start with `/api/v1`. Every response is `{"message": ..., "data": ...}`. Errors use the same
shape with status 400 (validation, with one message per field), 401, 403, 404 or 409.

| | Endpoint | Who |
|---|---|---|
| **Auth** | `POST /auth/register`, `POST /auth/login` (returns a 1-hour JWT), `GET /users/me` | anyone / anyone / logged in |
| **Products** | `GET /products?category=&brand=&name=&page=&size=`, `GET /products/{id}` | anyone |
| | `POST /products`, `PUT /products/{id}`, `DELETE /products/{id}` | admin |
| **Categories** | `GET /categories`, `GET /categories/{id}` | anyone |
| | `POST /categories`, `PUT /categories/{id}`, `DELETE /categories/{id}` | admin |
| **Images** | `GET /images/{id}` (the image itself) | anyone |
| | `POST /products/{id}/images` (multipart `files`), `PUT /images/{id}`, `DELETE /images/{id}` | admin |
| **Cart** | `GET /cart`, `POST /cart/items`, `PUT /cart/items/{productId}`, `DELETE /cart/items/{productId}`, `DELETE /cart` | logged in, own cart |
| **Orders** | `POST /orders` (check out), `GET /orders`, `GET /orders/{id}`, `POST /orders/{id}/cancel` | logged in, own orders |

Swagger UI lists every request and response body.

## Design notes

- **Security.** The API is stateless: Spring Security's OAuth2 resource server checks HS256 tokens signed
  with `JWT_SECRET`, and the token's `roles` claim becomes `ROLE_USER` / `ROLE_ADMIN`. Passwords are
  BCrypt hashes. Login gives the same answer, after the same amount of work, whether the email or the
  password was wrong.
- **Checkout is all or nothing.** It checks every cart line against stock before taking any, and `Product`
  has an `@Version` column, so two customers checking out the last unit at the same moment can't both
  succeed. The loser gets a 409, and a test races two real transactions to prove it.
- **Orders keep history.** Order lines copy the product's name and price when placed. A later price change
  or deleting the product doesn't change past orders, and cancelling puts the stock back.
- **No entity leaks.** Controllers return DTO records. Listing products fetches image metadata with one
  projection query per page and never loads image bytes.
- **Categories** are matched ignoring case and created on the fly when a product names a new one. A
  category that still has products can't be deleted.

## Configuration

| Variable | Default | |
|---|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | `jdbc:postgresql://localhost:5432/dreamshops`, `dreamshops`, `dreamshops` | PostgreSQL connection |
| `JWT_SECRET` | random per start | At least 32 bytes. Without it, tokens stop working when the app restarts. |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | unset | Creates this admin at startup if the account doesn't exist |
| `SHOW_SQL` | `false` | Log Hibernate's SQL |

## Tests

```bash
./mvnw verify
```

35 tests. MockMvc tests cover each API on in-memory H2, including access rules with real tokens.
`ConcurrentCheckoutTest` races two checkouts for the last unit. `PostgresIntegrationTest` runs the
whole flow against PostgreSQL in a Testcontainers container; it is skipped when Docker isn't available.
GitHub Actions runs all of them, then starts the Compose stack and logs in.

## Layout

```
src/main/java/com/dailycodework/dreamshops/
  controller/   REST endpoints
  service/      business rules (product, category, image, cart, order, user)
  model/        JPA entities
  repository/   Spring Data repositories
  dto/, request/, response/   what goes over the wire
  security/     JWT config, token issuing, first-admin setup
  exceptions/   error types and the handler that maps them to status codes
```
