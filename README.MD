# TicketRush

## Ports
Frontend: http://localhost:5173
Backend: http://localhost:8080
PostgreSQL: localhost:5432
Redis: localhost:6379

## Run our group database
Open docker desktop
> docker compose up -d

## Run backend
cd ticket
> mvn spring-boot:run( cần cài maven trong máy cùng jdk21)

## Config web
resources/application.yml

## Run frontend
cd frontend
> npm install
> npm run dev

## Reset local database
> docker compose down -v
> docker compose up -d

## Database rule
- DBML( mô tả cơ sở dữ liệu) is stored in docs
- Can install database client in vs code extension to view database's structure
(Connection String => postgresql://ticketrush:ticketrush@localhost:5432/ticketrush)
- Real schema is created by Flyway migrations(file sql khởi tạo cở sở dữ liệu ở resources/db/migration)
- Nếu muốn xem các phiên bản cơ sở dữ liệu đã được flyway tích hợp
>SELECT version, description, type, script, installed_on, success
>FROM flyway_schema_history
>ORDER BY installed_rank;

- Any DB change must add a new migration

## User Cache (Redis) - Home/Event only
- Mục tiêu: cache cho luồng user/khách (`CUSTOMER` hoặc anonymous), không cache cho `ADMIN`/`PROVIDER`.
- Cache framework: Spring Cache + Redis.
- Bật cache tại backend:
  - `ticket/src/main/java/com/example/ticket/TicketApplication.java` (`@EnableCaching`)
  - `ticket/src/main/resources/application.yml` (`spring.cache.type=redis`)
  - `ticket/src/main/java/com/example/ticket/config/CacheConfig.java` (TTL + serializer)
  - `ticket/src/main/java/com/example/ticket/config/UserCachePolicy.java` (điều kiện role được cache)

### Endpoint user đang được cache
- `GET /api/home` -> `user:home` (TTL 90s)
- `GET /api/events/grouped?limitPerCategory=...` -> `user:events:grouped` (TTL 90s)
- `GET /api/events?categoryId=&q=&city=&page=&size=` -> `user:events:search` (TTL 45s)
- `GET /api/events/slug/{slug}` -> `user:events:slug` (TTL 120s)
- `GET /api/events/legacy` -> `user:events:legacy` (TTL 60s)

### Invalidation logic
- Khi `create event` hoặc `update event` (provider/admin): xóa toàn bộ cache user Home/Event.
- Khi admin `delete user` (có thể kéo theo xóa event provider): xóa toàn bộ cache user Home/Event.
- Các thay đổi này được gắn qua `@CacheEvict(allEntries = true)` trong:
  - `ticket/src/main/java/com/example/ticket/service/EventServiceImpl.java`
  - `ticket/src/main/java/com/example/ticket/service/AdminServiceImpl.java`

### Lưu ý kỹ thuật
- `@Cacheable(sync = true)` được bật cho key nóng để tránh nhiều request cùng lúc cùng đánh DB.
- Cache condition:
  - `CUSTOMER`/anonymous: dùng cache.
  - `ADMIN`/`PROVIDER`: bỏ qua cache, đọc trực tiếp DB.

## Auto run docker container and run springboot web
- Open docker desktop
- Use git bash
> ./auto.sh

## 1 tài khoản admin để test phân quyền: 
>chạy truy vấn sql: UPDATE users
SET role = 'ADMIN',
    provider_request_status = NULL,
    provider_requested_at = NULL,
    provider_reviewed_at = NULL,
    provider_reviewed_by = NULL,
    provider_rejection_reason = NULL,
    email_verified = true
WHERE email = '@example.com';
