# TicketRush Web

Nền tảng đặt vé sự kiện với kiến trúc tách lớp rõ ràng:
- Frontend: Vue 3 + Vite
- Backend: Spring Boot 3
- Data: PostgreSQL + Redis

---

## Authors

- Nguyen Thanh Phuoc
- Nguyen Huu Luu
- Tran Chien Thang

---

## Project Structure

- `ticketFrontend/`: Vue frontend (UI + client API)
- `ticket/`: Spring Boot backend (REST API + business logic)
- `docs/`: tài liệu kỹ thuật, DBML, ghi chú
- `docker-compose.yml`: PostgreSQL, Redis, Mailpit cho local/dev

---

## Core Features

- User authentication (JWT, OAuth2 Google)
- Event discovery and detail pages
- Seat booking flow (internal seat map + seats.io integration)
- Checkout and payment flow (payOS)
- Virtual queue and lock timeout handling
- Conflict handling when multiple users try to lock the same seats
- Public API caching with Redis

---

## Tech Stack

## Frontend

- Vue `3.5.x`
- Vue Router
- Pinia
- Axios
- Tailwind CSS
- Vite

## Backend

- Java 21
- Spring Boot `3.5.x`
- Spring Web, Validation, Security
- Spring Data JPA + JdbcTemplate
- Flyway migrations
- Redis (Spring Cache + queue/token utilities)
- payOS Java SDK

## Infrastructure (local)

- PostgreSQL 16
- Redis 7
- Mailpit (mail testing)

---

## Important Technical Highlights

## 1) Seat Lock + Conflict Control

- Checkout lock uses DB transactional validation and row-level locking (`FOR UPDATE`) to avoid double booking.
- Seat states: `AVAILABLE`, `LOCKED`, `SOLD`.
- Confirm step requires seats to be locked by current user.

## 2) Timeout + Queue Penalty

- Seat lock has TTL (`app.booking.lock-minutes`).
- Scheduler runs periodically to:
  - expire pending orders,
  - release expired seat locks,
  - register strike penalty in virtual queue.

## 3) Redis Cache (Public/User-facing data)

- Enabled by `@EnableCaching`.
- Redis-backed Spring Cache with typed serialization and per-cache TTL.
- Event/home caches are evicted when event data changes.

---

## Local Ports

- Frontend: `http://localhost:5173`
- Backend: `http://localhost:8080`
- PostgreSQL: `localhost:5432`
- Redis: `localhost:6379`
- Mailpit UI: `http://localhost:8025`

---

## Prerequisites

- Docker Desktop
- JDK 21
- Maven 3.9+ (or use `./mvnw`)
- Node.js 20+

---

## Environment Setup

File mẫu:
- `.env.example`

Bạn có thể copy sang `.env` và chỉnh theo môi trường thật.

Redis config backend hiện hỗ trợ env:
- `REDIS_HOST` (default: `localhost`)
- `REDIS_PORT` (default: `6379`)

---

## Run Local (Recommended)

## 1) Start infrastructure

```bash
docker compose up -d
```

Services sẽ gồm:
- `ticketrush-postgres`
- `ticketrush-redis`
- `ticketrush-mailpit`

## 2) Run backend

```bash
cd ticket
./mvnw spring-boot:run
```

Hoặc:

```bash
mvn spring-boot:run
```

## 3) Run frontend

```bash
cd ticketFrontend
npm install
npm run dev
```

---

## Reset Local Database

```bash
docker compose down -v
docker compose up -d
```

---

## Database Rules

- Schema source of truth nằm trong Flyway migrations:
  - `ticket/src/main/resources/db/migration`
- Mọi thay đổi DB phải tạo migration mới, không sửa dữ liệu production thủ công.
- DBML tham khảo tại `docs/`.

---

## Cache Notes

- Bật cache:
  - `ticket/src/main/java/com/example/ticket/TicketApplication.java`
  - `ticket/src/main/resources/application.yml` (`spring.cache.type=redis`)
- Cấu hình TTL/prefix:
  - `ticket/src/main/java/com/example/ticket/config/CacheConfig.java`
- Cache key prefix:
  - `ticketrush:v3:*`

Ví dụ kiểm tra key:

```bash
redis-cli -h 127.0.0.1 -p 6379 KEYS "ticketrush:v3:*"
```

Lưu ý: key có thể rỗng nếu chưa có request tạo cache hoặc đã hết TTL.

---

## Payment + Webhook Notes

- Checkout APIs: `/api/me/checkout/*`
- payOS webhook: `/api/v1/payments/webhook`
- Return/Cancel URL cấu hình tại `app.payos.*` trong `application.yml`.

---

## Quick Troubleshooting

## Backend không kết nối Redis

- Kiểm tra Redis container:
```bash
docker ps
```
- Kiểm tra cổng:
```bash
redis-cli -h 127.0.0.1 -p 6379 ping
```

## Không thấy cache key

- Hãy gọi các API có `@Cacheable` (events/slug/search/home tùy flow code hiện tại).
- Chờ request thành công rồi kiểm tra lại `KEYS`.

## Không kết nối DB

- Kiểm tra PostgreSQL container và credentials trong `application.yml`.

---

## Production Notes (Short)

- Luôn dùng Redis riêng cho production (container/service riêng), không dùng local Redis.
- Set env rõ ràng:
  - `REDIS_HOST`
  - `REDIS_PORT`
  - DB credentials
  - JWT/OAuth/payOS secrets
- Không expose Redis public internet nếu không có network policy + auth chặt.

