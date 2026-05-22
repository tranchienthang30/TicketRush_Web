# TicketRush Study Guide

Tài liệu này tổng hợp chi tiết 5 luồng bạn yêu cầu:

1. Booking
2. Payment
3. Xử lý conflict khi 2 người đặt cùng lúc
4. Timeout + anti-spam (hết thời gian thì bắt chờ)
5. Cache cho API public

Mục tiêu: bạn có thể lần theo code theo đúng chuỗi `route -> controller -> service -> truy vấn -> phản hồi UI`, và hiểu vì sao hệ thống làm vậy.

---

## 0) Bản đồ tổng quan nhanh

### Frontend (Vue)

- Route booking: `ticketFrontend/src/router/index.js` -> `/booking`
- Route checkout: `ticketFrontend/src/router/index.js` -> `/checkout`
- Route callback payOS:
  - `/checkout/success` (`CheckoutSuccessView.vue`)
  - `/checkout/cancel` (`CheckoutCancelView.vue`)
- API wrapper gọi backend: `ticketFrontend/src/api/ticketRushApi.js`

### Backend (Spring Boot)

- Booking event API: `EventController#getBookingEvent` (`/api/events/{eventId}/booking`)
- Checkout APIs: `CheckoutController` (`/api/me/checkout/*`)
- Virtual queue APIs: `VirtualQueueController` (`/api/me/virtual-queue/*`)
- payOS webhook API: `PaymentWebhookController` (`/api/v1/payments/webhook`)

### Dữ liệu/infra chính

- Bảng lõi booking: `event_seats` (status, locked_by, lock_expires_at)
- Bảng đơn hàng: `orders`, `order_items`
- Redis:
  - Caching API public
  - Virtual queue + penalty cooldown

---

## 1) Booking Flow (chi tiết end-to-end)

## 1.1 Frontend route đi vào booking

- Từ card sự kiện:
  - `ticketFrontend/src/components/EventCard.vue` -> `:to="\`/booking?eventId=${event.id}\`"`
- Route guard:
  - `ticketFrontend/src/router/index.js`
  - `/booking` có `meta.requiresAuth: true`, `meta.requiresCustomer: true`
  - Nghĩa là phải đăng nhập và role phải là `CUSTOMER`.

Tác dụng:

- Chặn provider/admin đi vào luồng mua vé khách hàng.
- Giảm case bất thường ở backend.

## 1.2 BookView khởi tạo dữ liệu

File: `ticketFrontend/src/views/BookView.vue`

Hàm chính:

- `onMounted` -> gọi `startTimer()` + `loadBookingData()`
- `loadBookingData()` thực hiện:
  1. Reset state UI (error, selected seats, queue status...)
  2. Lấy `eventId` từ query
  3. Gọi `ensureQueueAccess(eventId)` (join virtual queue)
  4. Nếu queue `READY` thì gọi `refreshSeatMap()`
  5. Gọi thêm `getEventBySlug` + `getCategories` để hiển thị thông tin event
  6. Bật polling trạng thái ghế (`startSeatPolling`) cho seat map nội bộ

API frontend gọi:

- `joinVirtualQueue(eventId)`
- `getVirtualQueueStatus(eventId)`
- `getBookingEvent(eventId)`
- `lockBookingSeats(...)`
- `releaseBookingSeats(...)`

Tất cả nằm trong `ticketFrontend/src/api/ticketRushApi.js`.

## 1.3 Backend gate: bắt buộc qua virtual queue trước khi lấy seat map

Route backend:

- `GET /api/events/{eventId}/booking`
- File: `ticket/src/main/java/com/example/ticket/controller/EventController.java`
- Method: `getBookingEvent(...)`

Logic:

1. `CurrentUserService.resolve(...)` lấy userId thật từ security context
2. `virtualQueueService.requireAccess(eventId, viewerUserId)`:
  - nếu chưa được cấp slot queue -> throw `429 TOO_MANY_REQUESTS`
3. Mới cho gọi `eventService.getBookingEvent(...)`

Tác dụng:

- Không cho người dùng bypass queue bằng cách gọi API trực tiếp.
- Đồng bộ chính sách chống spam ở cả frontend và backend.

## 1.4 Dữ liệu booking event trả về được dựng như nào

File service:

- `ticket/src/main/java/com/example/ticket/service/EventServiceImpl.java`
- Method: `getBookingEvent(UUID eventId, UUID viewerUserId)`

Nó gọi repository:

- `EventQueryRepository#findPublishedEventForBooking`
- `EventQueryRepository#findSectionsByEvent`
- `EventQueryRepository#findSeatsByEvent`

Điểm SQL quan trọng:

- `findPublishedEventForBooking` chỉ lấy event còn bán:
  - `status='PUBLISHED'`
  - `sale_start_time <= now()`
  - `sale_end_time >= now()`
- `findSeatsByEvent` normalize ghế lock hết hạn thành `AVAILABLE` ngay khi query:
  - nếu `status='LOCKED'` nhưng `lock_expires_at <= now()` thì trả về status `AVAILABLE`
- Trả thêm:
  - `lock_owner_user_id`
  - `locked_by_current_user`

Tác dụng:

- UI có đủ dữ liệu để vẽ ghế theo trạng thái thực tế.
- Người dùng nhìn thấy lock cũ đã hết hạn như ghế trống.

## 1.5 Chọn ghế / bỏ ghế

Frontend:

- `BookView.vue`
- `toggleSeatUnit(unit)`:
  - nếu đang selected -> gọi `releaseBookingSeats`
  - nếu chưa selected -> gọi `lockBookingSeats`
  - sau đó `refreshSeatMap()`

Backend lock API:

- `POST /api/me/checkout/seats/lock`
- `CheckoutController#lockSeats` -> `CheckoutService#lockSeats`

Backend release API:

- `POST /api/me/checkout/seats/release`
- `CheckoutController#releaseSeats` -> `CheckoutService#releaseSeats`

Chi tiết `CheckoutService#lockSeats`:

1. `requireAccess` queue lần nữa
2. Chuẩn hóa danh sách ghế (loại duplicate)
3. Lấy ghế `FOR UPDATE` (`findSeatsByIdsForUpdate`)
4. `validateSeatOwnershipAndAvailability(..., LOCK)`
5. `setSeatLocks(...)` cập nhật status `LOCKED`, set `locked_by`, `lock_expires_at`
6. Nếu `lockedCount != seatIds.size()` -> ném `409 CONFLICT`

Chi tiết `CheckoutService#releaseSeats`:

1. `findSeatsByIdsForUpdate`
2. Validate mode `RELEASE`
3. `releaseSeatLocksForUser(...)` chỉ release ghế lock bởi chính user

Tác dụng:

- Tránh mất nhất quán khi nhiều người click cùng lúc.
- Không cho user B release lock của user A.

## 1.6 Chuyển sang checkout

Frontend:

- `BookView.vue` -> `continueToCheckout()`
- Lưu vào `sessionStorage` key `ticketrush_checkout_payload`:
  - `eventId`
  - `seatIds`
- Redirect `/checkout`

Tác dụng:

- Checkout dùng payload này để preview/confirm.
- Không phải truyền nhiều state qua URL.

---

## 2) Payment Flow (preview -> confirm -> payOS/webhook -> ticket)

## 2.1 Checkout page load và preview

Frontend file:

- `ticketFrontend/src/views/CheckoutView.vue`

Luồng:

1. `loadCheckoutPayload()` đọc `sessionStorage`
2. `loadPreview()` gọi `previewCheckout` API
3. Hiển thị:
  - seat codes
  - subtotal
  - service fee
  - membership discount
  - voucher discount
  - total
  - lock expiry countdown

Backend:

- `POST /api/me/checkout/preview`
- `CheckoutController#preview` -> `CheckoutService#preview`
- `preview()` gọi `evaluate(..., PREVIEW, forUpdate=false)`

`evaluate()` xử lý:

- validate event còn bookable
- validate seat trạng thái hợp lệ
- tính tiền vé + phí dịch vụ
- áp membership discount
- validate voucher + tính voucher discount
- trả `CheckoutSummaryResponse`

## 2.2 Confirm checkout

API:

- `POST /api/me/checkout/confirm`
- `CheckoutController#confirm` -> `CheckoutService#confirm`

Các bước `confirm()`:

1. `requireAccess` queue
2. `evaluate(..., CONFIRM, forUpdate=true)`:
  - dùng `FOR UPDATE` để khóa row ghế trong transaction
  - mode `CONFIRM` bắt buộc ghế phải `LOCKED` bởi chính user hiện tại
3. `resolveCheckoutExpiryForConfirm`:
  - nếu lock đã mất -> `409`
  - nếu lock sắp hết trong <=5s -> `409` yêu cầu chọn lại
4. Tạo `orderId`
5. Nếu method `card`:
  - sinh `payosOrderCode`
  - insert order status `PENDING`
  - tạo payment link payOS
  - trả `checkoutUrl`
6. Nếu method khác (`wallet`):
  - insert order status `SUCCESS`
  - mark ghế `SOLD`
  - issue ticket QR
  - trả kết quả thành công ngay

Lưu ý thiết kế hiện tại:

- Trong code hiện thời, `card` map sang luồng payOS.
- `wallet` đang là luồng giả lập success đồng bộ (không gọi cổng thanh toán ngoài).

## 2.3 Frontend redirect payment

`CheckoutView.vue`:

- `submitCheckout()` gọi `confirmCheckout`
- nếu method `card`:
  - đọc `checkoutUrl`
  - `window.location.href = checkoutUrl` để chuyển sang payOS

## 2.4 Callback sau khi payOS trả về frontend

File:

- `CheckoutSuccessView.vue`
- `CheckoutCancelView.vue`

`CheckoutSuccessView`:

1. Xóa `sessionStorage` checkout payload
2. Đọc `orderCode` và `code` từ query
3. Nếu `code != '00'`:
  - gọi `cancelPayOSCheckout(orderCode)` (best effort)
  - redirect Home
4. Nếu thành công:
  - gọi `completePayOSCheckout(orderCode)` (best effort)
  - redirect My Tickets

`CheckoutCancelView`:

1. Xóa payload
2. Gọi `cancelPayOSCheckout(orderCode)` (best effort)
3. Redirect Home

Tác dụng:

- Nếu webhook chậm, frontend vẫn có cơ chế finalize/cancel chủ động.
- Nếu webhook đã xử lý trước rồi thì API complete/cancel phải idempotent.

## 2.5 Webhook payOS (bất đồng bộ, nguồn sự thật từ cổng thanh toán)

Route:

- `POST /api/v1/payments/webhook`
- `PaymentWebhookController#receiveWebhook`

Service:

- `PaymentWebhookService#processWebhook`

Các bước:

1. Verify chữ ký webhook: `payOSPaymentService.verifyWebhookData`
2. Resolve order theo `orderCode` (ưu tiên) hoặc parse UUID từ `description`
3. Nếu order đã paid/success -> return "already processed" (idempotent)
4. Nếu `paymentCode != "00"`:
  - mark order cancelled if pending
  - release seat locks
5. Nếu thành công:
  - `markOrderPaidIfPending`
  - nếu update=0 thì kiểm tra lại status (idempotent)
  - `markOrderSeatsSold`
  - `issueTicketsForOrder`
  - gửi email xác nhận + QR

Tác dụng:

- Bảo toàn trạng thái khi callback frontend không chắc chắn.
- Dù user đóng trình duyệt, webhook vẫn finalize đơn.

## 2.6 Cấu hình payOS

Files:

- `config/PayOSProperties.java`
- `config/PayOSConfig.java`
- `application.yml` (`app.payos.*`)

Giá trị quan trọng:

- `return-url`: mặc định `http://localhost:5173/checkout/success`
- `cancel-url`: mặc định `http://localhost:5173/checkout/cancel`

---

## 3) Conflict khi 2 người cùng đặt một ghế

Đây là phần trọng tâm để thuyết trình kỹ thuật.

## 3.1 Cơ chế khóa transaction cấp DB

File:

- `CheckoutQueryRepository#findSeatsByIdsForUpdate`

SQL:

- `SELECT ... FROM event_seats WHERE id IN (...) FOR UPDATE`

Ý nghĩa:

- Row ghế được lock trong transaction hiện tại.
- Transaction khác phải chờ hoặc đọc trạng thái mới sau commit.

## 3.2 Validate trạng thái ghế theo mode nghiệp vụ

File:

- `CheckoutService#validateSeatOwnershipAndAvailability`

4 mode:

- `PREVIEW`
- `LOCK`
- `RELEASE`
- `CONFIRM`

Rule quan trọng:

- `LOCK`: ghế SOLD thì reject; ghế LOCKED bởi người khác (chưa hết hạn) thì reject
- `CONFIRM`: ghế phải đang LOCKED bởi chính user
- `RELEASE`: chỉ cho release lock của chính user

## 3.3 Kiểm chứng số dòng update để bắt race condition

File:

- `CheckoutService#lockSeats`

Sau `setSeatLocks(...)`:

- Nếu số dòng update khác số ghế yêu cầu -> `409`

Ý nghĩa:

- Dù đã validate, vẫn có thể gặp thay đổi cạnh tranh; check `lockedCount` là lớp phòng thủ cuối.

## 3.4 Chống finalize trùng khi complete/webhook chạy đồng thời

Files:

- `CheckoutService#completePayOSPayment`
- `PaymentWebhookService#processWebhook`

Cả 2 cùng dùng pattern:

- `markOrderPaidIfPending(orderId, now)`
- nếu update = 0 -> đọc status mới
- nếu đã SUCCESS/PAID thì coi là idempotent, không xử lý lại

Tác dụng:

- Tránh phát hành ticket trùng.
- Tránh ghi đè trạng thái khi request đến cùng lúc.

## 3.5 Constraint DB làm hàng rào cuối

Migration:

- `V1__init_schema.sql`

Ràng buộc quan trọng:

- `event_seats.ck_event_seats_lock_consistency`:
  - status LOCKED thì bắt buộc có `locked_by` + `lock_expires_at`
- Unique index `ux_order_items_one_issued_ticket_per_event_seat`:
  - mỗi `event_seat_id` chỉ có 1 ticket đã issued (`VALID`/`USED`)
- `orders` check trạng thái với `paid_at`, `cancelled_at`

Tác dụng:

- Nếu tầng service có bug, DB vẫn ngăn dữ liệu sai.

## 3.6 Mô phỏng race thực tế (A và B cùng click ghế A1)

1. A gọi lock A1 -> lấy row lock DB -> pass validate -> update LOCKED by A -> commit
2. B gọi lock A1:
  - chờ row lock hoặc đọc trạng thái mới
  - thấy LOCKED by A chưa hết hạn
  - bị reject `409 Seat ... was just locked by another user`

Kết quả:

- Chỉ một người giữ ghế tại một thời điểm.

---

## 4) Timeout hết thời gian + chống spam liên tục

## 4.1 TTL lock ghế

Config:

- `application.yml` -> `app.booking.lock-minutes: 10`

Áp dụng:

- `CheckoutService#lockSeats` set `lockExpiresAt = now + bookingLockMinutes`

Frontend:

- `BookView.vue#startTimer`
- Timer hiển thị countdown theo `activeLockExpiryMs`

## 4.2 Scheduler thu hồi lock và expire đơn pending

File:

- `SeatLifecycleScheduler#expirePendingOrdersAndReleaseSeatLocks`
- Chạy mỗi `15s` (config `app.booking.expire-interval-ms`)

Việc làm:

1. Tìm user có lock hết hạn: `findExpiredSeatLockOwners`
2. Đánh dấu order pending hết hạn: `expirePendingOrders`
3. Mở lock ghế hết hạn: `releaseExpiredSeatLocks`
4. Ghi strike penalty cho user bị hết hạn lock:
  - `virtualQueueService.registerExpiredSeatLockStrike(...)`

Tác dụng:

- Tự dọn ghế bị giữ mà không thanh toán.
- Không phụ thuộc user bấm refresh hay không.

## 4.3 Virtual queue + cooldown anti-spam

Files:

- `VirtualQueueService`
- `VirtualQueueController`
- Frontend `BookView.vue` (`ensureQueueAccess`, `startQueuePolling`)

Cơ chế:

1. User phải `join` queue để có slot `READY`
2. `requireAccess` được gọi ở backend trước các API nhạy cảm:
  - `/api/events/{id}/booking`
  - `/api/me/checkout/preview`
  - `/api/me/checkout/seats/lock`
  - `/api/me/checkout/confirm`
3. Nếu user có penalty cooldown:
  - backend trả `429` + message "Please wait ... seconds..."
4. Penalty tăng theo strike:
  - `penaltySecondsForStrike`: base * 2^(strike-1), capped max
  - mặc định base `120s`, max `3600s`

Tác dụng:

- Hành vi giữ ghế rồi bỏ liên tục bị phạt tăng dần.
- Giảm spam request lock ghế, bảo vệ fairness.

## 4.4 Redis key dùng cho queue/penalty

Trong `VirtualQueueService`:

- Active set: `virtual-queue:event:{eventId}:active`
- Waiting set: `virtual-queue:event:{eventId}:waiting`
- Sequence key: `virtual-queue:event:{eventId}:sequence`
- Penalty count key: `virtual-queue:penalty:count:event:{eventId}:user:{userId}`
- Penalty until key: `virtual-queue:penalty:until:event:{eventId}:user:{userId}`

Tác dụng:

- Tách bạch rõ ràng dữ liệu queue và penalty theo event/user.

---

## 5) Cache các API public

## 5.1 Bật caching và Redis cache manager

Files:

- `TicketApplication.java` (`@EnableCaching`)
- `config/CacheConfig.java`
- `application.yml`:
  - `spring.cache.type=redis`
  - `spring.data.redis.host/port`

Prefix key cache:

- `ticketrush:v3:{cacheName}::`

## 5.2 Danh sách cache name và TTL

Theo `CacheNames.java` + `CacheConfig.java`:

- `user:home` -> TTL 90s
- `user:events:grouped` -> TTL 90s
- `user:events:search` -> TTL 45s
- `user:events:slug` -> TTL 120s
- `user:events:legacy` -> TTL 60s
- Default fallback TTL cache khác: 60s

## 5.3 API nào đang dùng cache

1. `GET /api/home`
  - `HomeService#getHome`
  - `@Cacheable(user:home, condition = "@userCachePolicy.allowCache()")`

2. `GET /api/events/legacy`
  - `EventServiceImpl#publicEvents`
  - cache `user:events:legacy`

3. `GET /api/events/slug/{slug}`
  - `EventServiceImpl#eventBySlug`
  - cache `user:events:slug`, key = `slug`

4. `GET /api/events/grouped`
  - `EventServiceImpl#getGroupedEvents(limitPerCategory)`
  - cache `user:events:grouped`, key = `limitPerCategory`

5. `GET /api/events` (search/list)
  - `EventServiceImpl#searchEvents(categoryId, query, city, page, size)`
  - cache `user:events:search` (key mặc định theo args method)

## 5.4 Điều kiện cache theo role

File:

- `config/UserCachePolicy.java`

Rule:

- Không có auth context -> cho cache
- Có JWT principal:
  - chỉ role `CUSTOMER` được cache
  - `ADMIN`/`PROVIDER` không cache
- Principal lạ -> không cache

Tác dụng:

- Tránh cache nhầm dữ liệu theo góc nhìn admin/provider.
- Tập trung cache cho traffic public/customer.

## 5.5 Evict cache khi dữ liệu thay đổi

Files:

- `EventServiceImpl#createEvent` và `updateEvent`
- `AdminServiceImpl#deleteUser`

Cả các hàm này đều `@CacheEvict(allEntries=true)` cho toàn bộ cache public event/home.

Tác dụng:

- Khi dữ liệu nền thay đổi, cache không bị stale quá lâu.

## 5.6 Fallback khi Redis cache lỗi

File:

- `config/CacheFallbackConfig.java`

`CacheErrorHandler` chỉ log warning khi GET/PUT/EVICT/CLEAR lỗi, không làm request fail.

Tác dụng:

- Redis lỗi tạm thời thì API vẫn trả dữ liệu trực tiếp từ DB/service.

## 5.7 Endpoint public theo security config

File:

- `config/SecurityConfig.java`

`permitAll` cho:

- `GET /api/home`
- `GET /api/categories/**`
- `GET /api/events/**`

Lưu ý:

- Không phải endpoint public nào cũng có `@Cacheable` (ví dụ `categories` hiện chưa cache ở service layer).

---

## 6) Danh sách file cốt lõi để ôn theo luồng

### Booking

- Front:
  - `ticketFrontend/src/router/index.js`
  - `ticketFrontend/src/components/EventCard.vue`
  - `ticketFrontend/src/views/BookView.vue`
  - `ticketFrontend/src/api/ticketRushApi.js`
- Back:
  - `ticket/src/main/java/com/example/ticket/controller/EventController.java`
  - `ticket/src/main/java/com/example/ticket/controller/CheckoutController.java`
  - `ticket/src/main/java/com/example/ticket/service/EventServiceImpl.java`
  - `ticket/src/main/java/com/example/ticket/service/CheckoutService.java`
  - `ticket/src/main/java/com/example/ticket/repository/EventQueryRepository.java`
  - `ticket/src/main/java/com/example/ticket/repository/CheckoutQueryRepository.java`

### Payment

- Front:
  - `ticketFrontend/src/views/CheckoutView.vue`
  - `ticketFrontend/src/views/CheckoutSuccessView.vue`
  - `ticketFrontend/src/views/CheckoutCancelView.vue`
- Back:
  - `ticket/src/main/java/com/example/ticket/controller/CheckoutController.java`
  - `ticket/src/main/java/com/example/ticket/controller/PaymentWebhookController.java`
  - `ticket/src/main/java/com/example/ticket/service/CheckoutService.java`
  - `ticket/src/main/java/com/example/ticket/service/PayOSPaymentService.java`
  - `ticket/src/main/java/com/example/ticket/service/PaymentWebhookService.java`
  - `ticket/src/main/java/com/example/ticket/service/PaymentConfirmationEmailService.java`

### Conflict + Timeout + Queue

- `ticket/src/main/java/com/example/ticket/service/CheckoutService.java`
- `ticket/src/main/java/com/example/ticket/repository/CheckoutQueryRepository.java`
- `ticket/src/main/java/com/example/ticket/service/VirtualQueueService.java`
- `ticket/src/main/java/com/example/ticket/service/SeatLifecycleScheduler.java`
- `ticket/src/main/resources/application.yml`
- `ticket/src/main/resources/db/migration/V1__init_schema.sql`
- `ticket/src/main/resources/db/migration/V9__add_success_order_status_for_payos.sql`
- `ticket/src/main/resources/db/migration/V10__update_paid_constraint_for_success_status.sql`
- `ticket/src/main/resources/db/migration/V11__add_payos_order_code_to_orders.sql`

### Cache public API

- `ticket/src/main/java/com/example/ticket/TicketApplication.java`
- `ticket/src/main/java/com/example/ticket/config/CacheConfig.java`
- `ticket/src/main/java/com/example/ticket/config/CacheNames.java`
- `ticket/src/main/java/com/example/ticket/config/UserCachePolicy.java`
- `ticket/src/main/java/com/example/ticket/config/CacheFallbackConfig.java`
- `ticket/src/main/java/com/example/ticket/service/HomeService.java`
- `ticket/src/main/java/com/example/ticket/service/EventServiceImpl.java`

---

## 7) Gợi ý trình bày ngắn gọn khi thuyết trình

Bạn có thể nói theo format 1 slide/1 mục:

1. Mục tiêu nghiệp vụ của luồng
2. API contract chính
3. Kỹ thuật đảm bảo đúng dữ liệu (transaction/lock/idempotent/cache)
4. Kỹ thuật đảm bảo trải nghiệm (polling/timer/waiting room)
5. Failure cases và cách hệ thống tự phục hồi

Một câu chốt kỹ thuật dễ nhớ:

- Booking an toàn vì khóa row ghế bằng `FOR UPDATE` + validate ownership + check số dòng update.
- Payment an toàn vì finalize theo trạng thái `PENDING -> SUCCESS` có idempotent ở cả callback frontend và webhook.
- Chống spam bằng virtual queue + penalty cooldown tăng theo strike.
- API public giảm tải nhờ Redis cache TTL ngắn + evict khi dữ liệu thay đổi.

