# TicketRush - Project Summary & Development Roadmap

**Last Updated:** May 16, 2026  
**Project Type:** Full-Stack Web Application for Event Ticket Booking & Provider Management  
**Tech Stack:**
- **Backend:** Java 21, Spring Boot 3.5.14, PostgreSQL, Redis, Flyway (DB Migrations)
- **Frontend:** Vue 3, Vite, Vue Router, Pinia (State Management), Tailwind CSS, Axios
- **Payment:** PayOS Integration
- **Authentication:** JWT (HttpOnly Cookies), Google OAuth2, Email-based verification

---

## Phần 1: Luồng đăng nhập, đăng ký, xác thực và phân quyền

Phần này mô tả trạng thái hiện tại của module auth/role sau khi merge và refactor sang mô hình `CUSTOMER`, `PROVIDER`, `ADMIN`.

### 1.1 Mục tiêu nghiệp vụ

- Người dùng mới đăng ký hoặc đăng nhập Google OAuth lần đầu đều được tạo tài khoản mặc định là `CUSTOMER`.
- Tài khoản cần xác thực email để hệ thống có thể gửi các thông tin quan trọng như QR vé, thông báo đơn hàng, reset mật khẩu và thông báo xét duyệt provider.
- `CUSTOMER` vẫn dùng hệ thống để xem sự kiện, đặt vé, thanh toán và quản lý vé cá nhân.
- `CUSTOMER` muốn tạo/quản lý sự kiện phải gửi yêu cầu `Becoming Providers`.
- Admin duyệt yêu cầu provider. Chỉ khi được duyệt thì tài khoản mới chuyển role sang `PROVIDER`.
- `PROVIDER` có quyền vào màn `Creating` và `Managements` để tạo/quản lý sự kiện của chính mình.
- `ADMIN` là tài khoản quản trị hệ thống, trước mắt đã có trang xét duyệt provider để kiểm thử luồng phân quyền.
- Luồng organization/business email cũ đã bị loại khỏi luồng chính. Các endpoint organization cũ trả `410 Gone`.

### 1.2 Đăng ký bằng email/password

Frontend gửi `POST /api/auth/register` với:

- `fullName`
- `email`
- `phone`
- `password`
- `requestProviderAccess`
- `recaptchaToken`

Backend xử lý:

1. Chuẩn hóa email về lowercase.
2. Kiểm tra email chưa tồn tại.
3. Hash password bằng `PasswordEncoder`.
4. Tạo user với:
   - `role = CUSTOMER`
   - `status = ACTIVE`
   - `provider = LOCAL`
   - `emailVerified = false`
5. Nếu user tick chọn đăng ký provider ngay khi tạo tài khoản:
   - `providerRequestStatus = PENDING`
   - `providerRequestedAt = now`
   - gửi thông báo provider request cho user và admin nếu có `ADMIN_EMAIL`.
6. Gửi email xác thực tài khoản.
7. Tạo access token và refresh token.
8. Set token vào HttpOnly Cookie, không trả token cho frontend lưu localStorage.

Response trả về `UserResponse`, frontend chỉ lưu thông tin user trong Pinia.

### 1.3 Đăng nhập bằng email/password

Frontend gửi `POST /api/auth/login`.

Backend xử lý:

1. Tìm user theo email.
2. Chặn nếu user bị `BLOCKED`.
3. So khớp password hash.
4. Tạo access token và refresh token theo session policy của role.
5. Set cookie:
   - `access_token`
   - `refresh_token`
6. Trả `UserResponse` cho frontend.

Frontend sau login:

- Lưu user object trong Pinia.
- Không lưu JWT trong localStorage.
- Navbar thay đổi theo role:
  - `CUSTOMER`: Booking, My Tickets, Profile.
  - `PROVIDER`: Creating, Managements, Profile.
  - `ADMIN`: Provider Requests, Creating, Managements, Profile.

### 1.4 Google OAuth

Luồng bắt đầu từ `/oauth2/authorization/google`.

Backend xử lý khi Google login thành công:

1. Lấy `email`, `name`, `sub`, `picture` từ Google profile.
2. Nếu đã có user theo provider/providerId thì dùng user đó.
3. Nếu chưa có provider/providerId nhưng email đã tồn tại:
   - link Google provider vào account hiện có.
   - không tạo user trùng email.
4. Nếu email chưa tồn tại:
   - tạo user mới với `role = CUSTOMER`, `provider = GOOGLE`, `emailVerified = false`.
   - gửi email xác thực cho tài khoản Google mới.
5. Set HttpOnly Cookies giống local login.
6. Redirect frontend về `/oauth2/callback`.

Lưu ý: Google OAuth chứng minh email từ Google, nhưng hệ thống vẫn gửi email xác thực riêng để đồng bộ luồng gửi vé/QR và các thông báo nghiệp vụ.

### 1.5 Xác thực email

Endpoint:

- `POST /api/auth/verify-email?token=...`
- `POST /api/auth/resend-verification-email`

Backend dùng Redis để lưu verification token với TTL 24 giờ.

Luồng:

1. Khi đăng ký local hoặc tạo user Google mới, backend tạo token xác thực email.
2. EmailService gửi link `/verify-email?token=...`.
3. User bấm link, frontend gọi `POST /api/auth/verify-email`.
4. Backend consume token trong Redis.
5. Nếu token hợp lệ, set `emailVerified = true`.

Nếu token hết hạn hoặc sai, API trả lỗi để frontend hiển thị trạng thái thất bại.

### 1.6 Quên mật khẩu

Endpoint:

- `POST /api/auth/forgot-password`
- `POST /api/auth/reset-password`

Luồng:

1. User nhập email ở màn Forgot Password.
2. Backend kiểm tra rate limit bằng Redis để tránh spam gửi mail.
3. Nếu email tồn tại, tạo reset token TTL 15 phút.
4. EmailService gửi link reset password dạng HTML.
5. User nhập mật khẩu mới.
6. Backend validate token, kiểm tra chưa hết hạn, hash password mới và lưu lại.

API forgot-password luôn trả thông báo chung để tránh lộ email có tồn tại trong hệ thống hay không.

### 1.7 JWT, Cookie và session

Hệ thống đã chuyển JWT sang HttpOnly Cookie:

- Cookie mặc định:
  - `httpOnly(true)`
  - `secure(false)` cho môi trường dev
  - `path("/")`
  - `sameSite("Lax")`
- Frontend axios bật `withCredentials: true`.
- Frontend không tự gắn `Authorization` header.
- `JwtAuthenticationFilter` đọc token từ cookie `access_token`.
- Refresh token đọc từ cookie `refresh_token`.
- Logout clear cookie và blacklist JWT trong Redis theo TTL còn lại.

Session policy:

- `CUSTOMER`: dùng thời lượng ngắn theo `app.session.customer-duration-ms`.
- `PROVIDER` và `ADMIN`: dùng session duration tiêu chuẩn.
- Frontend hiển thị cảnh báo gần hết phiên cho customer và tự điều hướng về login khi session hết hạn.

### 1.8 Phân quyền hiện tại

#### CUSTOMER

- Xem home/events public.
- Đặt vé, checkout, xem My Tickets sau khi đăng nhập.
- Quản lý profile cá nhân.
- Gửi yêu cầu `Becoming Providers`.
- Không được vào `Creating` và `Managements`.
- Nếu đang pending provider, profile hiển thị `Waiting for admin's approval`.

#### PROVIDER

- Có toàn bộ quyền customer phù hợp.
- Được vào `Creating`.
- Được vào `Managements`.
- Được tạo event bằng `POST /api/events`.
- Chỉ quản lý event do chính mình tạo.
- Không bị giới hạn session ngắn như customer.

#### ADMIN

- Có quyền truy cập trang `/admin/provider-requests`.
- Xem danh sách provider request đang `PENDING`.
- Approve request để chuyển user thành `PROVIDER`.
- Reject request và lưu lý do từ chối.
- Có thể vào các màn provider để kiểm thử/tạm quản trị.

### 1.9 Luồng Becoming Providers

Frontend:

- Nút nằm ở `/profile`.
- Nếu user là `CUSTOMER` và chưa pending, hiển thị nút `Becoming Providers`.
- Nếu đã pending, hiển thị `Waiting for admin's approval`.
- Nếu bị reject, hiển thị lý do reject nếu có.

Backend endpoint:

- `POST /api/providers/request`

Backend xử lý:

1. Lấy current user từ JWT cookie.
2. Chặn `ADMIN` vì admin không cần request provider.
3. Chặn user đã là `PROVIDER`.
4. Nếu request đang `PENDING`, gửi lại email xác thực và trả trạng thái hiện tại.
5. Nếu chưa request hoặc đã từng bị reject:
   - set `providerRequestStatus = PENDING`
   - set `providerRequestedAt = now`
   - clear review fields cũ
   - gửi lại email verification
   - gửi email thông báo đã nhận request cho user
   - gửi email admin nếu `app.admin.email`/`ADMIN_EMAIL` được cấu hình

### 1.10 Luồng admin duyệt provider

Frontend route:

- `/admin/provider-requests`

Backend endpoints:

- `GET /api/admin/provider-requests`
- `POST /api/admin/provider-requests/{userId}/approve`
- `POST /api/admin/provider-requests/{userId}/reject`

Approve:

1. Chỉ `ADMIN` được gọi.
2. Chỉ xử lý request đang `PENDING`.
3. Set:
   - `role = PROVIDER`
   - `providerRequestStatus = APPROVED`
   - `providerReviewedAt = now`
   - `providerReviewedBy = admin.id`
4. Gửi email thông báo approve cho user.

Reject:

1. Chỉ `ADMIN` được gọi.
2. Chỉ xử lý request đang `PENDING`.
3. Giữ role là `CUSTOMER`.
4. Set:
   - `providerRequestStatus = REJECTED`
   - `providerReviewedAt = now`
   - `providerReviewedBy = admin.id`
   - `providerRejectionReason = reason`
5. Gửi email thông báo reject cho user.

### 1.11 Database liên quan

Các field chính trong bảng `users`:

- `role`: `CUSTOMER`, `PROVIDER`, `ADMIN`
- `status`: `ACTIVE`, `BLOCKED`
- `provider`: `LOCAL`, `GOOGLE`
- `provider_id`: Google subject id nếu link OAuth
- `email_verified`
- `provider_request_status`: `PENDING`, `APPROVED`, `REJECTED`
- `provider_requested_at`
- `provider_reviewed_at`
- `provider_reviewed_by`
- `provider_rejection_reason`

Migration mới nhất cho luồng này:

- `V15__provider_admin_review.sql`

### 1.12 API auth/role chính

Authentication:

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `GET /api/auth/me`
- `POST /api/auth/verify-email`
- `POST /api/auth/resend-verification-email`
- `POST /api/auth/forgot-password`
- `POST /api/auth/reset-password`

OAuth:

- `GET /oauth2/authorization/google`
- `GET /login/oauth2/code/google`
- Frontend callback: `/oauth2/callback`

Provider request:

- `POST /api/providers/request`

Admin provider approval:

- `GET /api/admin/provider-requests`
- `POST /api/admin/provider-requests/{userId}/approve`
- `POST /api/admin/provider-requests/{userId}/reject`

Legacy organization endpoints:

- `POST /api/providers/register`: deprecated, trả `410 Gone`
- `POST /api/providers/verify`: deprecated, trả `410 Gone`
- `POST /api/organizations/register`: deprecated, trả `410 Gone`
- `POST /api/organizations/verify`: deprecated, trả `410 Gone`

### 1.13 Cấu hình môi trường liên quan

Backend `application.yml`:

- `frontend.url`: URL frontend để tạo link verify/reset.
- `app.mail.from` hoặc `MAIL_FROM`: email sender.
- `app.admin.email` hoặc `ADMIN_EMAIL`: email nhận thông báo provider request.
- `app.session.customer-duration-ms`: thời lượng session customer.
- `app.recaptcha.enabled`: bật/tắt reCAPTCHA.
- `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`: cấu hình Google OAuth.
- Redis: dùng cho token verification/reset, blacklist JWT và rate limiting.

---

## 1. Current Implementation Status

### 1.1 Core Architecture

#### Database & Data Model
- **PostgreSQL** is the source of truth
- **Redis** used for caching, token management, and rate limiting
- **Flyway** database migrations for version control (14 migrations currently)
- Key entities: Users, Events, Organizations, Categories, Event Seats, Event Sections

#### Database Schema Overview

| Entity | Purpose | Status |
|--------|---------|--------|
| `users` | User accounts with roles | ✅ Implemented |
| `events` | Movie/event listings | ✅ Implemented |
| `event_sections` | Seat zones (VIP, Standard, etc.) | ✅ Implemented |
| `event_seats` | Individual seat data with locking | ✅ Implemented |
| `organizations` | Provider/cinema company profiles | ✅ Implemented |
| `categories` | Event categories (Music, Concert, etc.) | ✅ Implemented |
| `orders` | Ticket purchase orders | ✅ Implemented |
| `order_items` | Individual tickets in an order | ✅ Implemented |
| `memberships` | User membership plans | ✅ Implemented |

---

### 1.2 User Roles & Permissions

The system implements a **3-tier role architecture**:

#### CUSTOMER Role
- ✅ Browse all published events
- ✅ Search events by category, city, date
- ✅ View event details and seat availability
- ✅ Lock seats temporarily (15-minute window)
- ✅ Complete ticket purchase (with PayOS payment)
- ✅ View personal tickets (QR codes)
- ✅ Manage user profile
- ✅ Email verification required for ticket delivery
- ✅ Session timeout: 15 minutes
- ❌ Cannot create/manage events (unless upgraded to PROVIDER)

#### PROVIDER Role (Cinema/Movie Company)
- ✅ User upgrades from CUSTOMER by verifying a business email
- ✅ Create new events/movies with full configuration
- ✅ Define seat layout and pricing (multiple sections)
- ✅ View and manage own events
- ✅ Set event sale windows (when tickets open/close)
- ✅ Configure external seat mapping (SEATS_IO integration available)
- ✅ Enter payout bank account information
- ❌ Events publish immediately (MVP behavior)
- ❌ **Pending:** Require ADMIN approval before publishing

#### ADMIN Role
- ✅ Full system access (to be implemented)
- ❌ **Pending:** Approve/reject provider registrations
- ❌ **Pending:** Review and publish events
- ❌ **Pending:** Manage all users
- ❌ **Pending:** View system dashboard and analytics
- ❌ **Pending:** Manage categories
- ❌ **Pending:** Manage promotions/vouchers

---

### 1.3 Authentication & Authorization

#### Implemented Features
- ✅ **Local Registration:** Email + password with validation
- ✅ **Local Login:** Email + password authentication
- ✅ **Google OAuth2:** Third-party login with automatic account creation
- ✅ **JWT Tokens:** Access & refresh tokens in HttpOnly cookies
- ✅ **Email Verification:** Required for customers to purchase tickets
- ✅ **Provider Verification:** Business email verification before provider access
- ✅ **Password Reset:** Email-based reset link (15-minute expiration)
- ✅ **Session Management:** Different timeouts per role
- ✅ **reCAPTCHA v3:** Bot protection (configurable)
- ✅ **Token Refresh:** Automatic refresh mechanism
- ✅ **Logout:** Server-side JWT blacklisting via Redis

#### Auth Flow for Provider Upgrade
1. User (CUSTOMER) clicks "Register as Provider"
2. Enters organization name + business email
3. System sends verification email
4. User clicks link and verifies email
5. User role changes from CUSTOMER → PROVIDER
6. User can now create events
7. **[TODO]** ADMIN reviews and approves provider

---

### 1.4 Event Management (Movie/Cinema)

#### Event Creation Workflow (3-Step Wizard)
**Step 1: Event Information**
- ✅ Title, description, duration
- ✅ Banner image URL
- ✅ Category selection
- ✅ Location details (venue name, city, address)
- ✅ Start/end times
- ✅ Sales window (when tickets open/close)
- ✅ Listing type (NOW_SHOWING, COMING_SOON, etc.)

**Step 2: Seating Configuration**
- ✅ Support for multiple seat sections (VIP, Standard, Economy)
- ✅ Define row count and seats per row
- ✅ Set base price per section
- ✅ Auto-generate individual seat codes
- ✅ SEATS_IO integration for external seat mapping
- ✅ Real-time seat locking with atomic updates

**Step 3: Payment & Confirmation**
- ✅ Payout bank account information
- ✅ Terms acceptance
- ✅ Submit for publishing

#### Event Status Workflow
- DRAFT → PUBLISHED (immediately in MVP)
- PUBLISHED → Events visible to customers
- Can be CANCELLED or marked FINISHED

#### Event Display
- ✅ Home page with featured events
- ✅ Events page with grouping by category
- ✅ Event detail page with seat map visualization
- ✅ Search/filter by category, city, date

---

### 1.5 Booking & Checkout System

#### Seat Locking (Optimistic Lock)
- ✅ Seats locked for 15 minutes during browsing
- ✅ Atomic seat status updates (AVAILABLE → LOCKED → SOLD)
- ✅ Automatic unlock if checkout not completed
- ✅ Version control to prevent race conditions

#### Checkout Process
- ✅ Preview order with seat details
- ✅ Confirm and initiate payment
- ✅ PayOS payment gateway integration
- ✅ Payment webhook handling
- ✅ Order status tracking (PENDING → PAID → EXPIRED → CANCELLED)

#### Ticket Management
- ✅ QR code generation per ticket
- ✅ Ticket status tracking (NOT_ISSUED → VALID → USED → CANCELLED)
- ✅ View my tickets page
- ✅ Ticket delivery via email

---

### 1.6 Membership System

- ✅ Multiple membership tiers
- ✅ Subscribe to membership plans
- ✅ Membership status tracking (ACTIVE, EXPIRED, CANCELLED)
- ✅ Membership benefits (to be defined)

---

### 1.7 Frontend Views & Navigation

| Route | Component | Auth Required | Role | Status |
|-------|-----------|----------------|------|--------|
| `/` | HomeView | No | Public | ✅ Live |
| `/events` | EventsView | No | Public | ✅ Live |
| `/help` | HelpCenterView | No | Public | ✅ Live |
| `/login` | LoginView | No | Public | ✅ Live |
| `/register` | RegisterView | No | Public | ✅ Live |
| `/forgot-password` | ForgotPasswordView | No | Public | ✅ Live |
| `/reset-password` | ResetPasswordView | No | Public | ✅ Live |
| `/verify-email` | VerifyEmailView | Yes | CUSTOMER | ✅ Live |
| `/booking` | BookView | Yes | CUSTOMER | ✅ Live |
| `/checkout` | CheckoutView | Yes | CUSTOMER | ✅ Live |
| `/checkout/success` | CheckoutSuccessView | No | Public | ✅ Live |
| `/checkout/cancel` | CheckoutCancelView | No | Public | ✅ Live |
| `/my-tickets` | MyTicketsView | Yes | CUSTOMER | ✅ Live |
| `/profile` | ProfileView | Yes | All | ✅ Live |
| `/provider/register` | RegisterOrganizationView | Yes | CUSTOMER | ✅ Live |
| `/provider/verify` | VerifyOrganizationView | No | Public | ✅ Live |
| `/create-movie` | CreateEventView | Yes | PROVIDER/ADMIN | ✅ Live |
| `/cinemas-management` | MyEventsView | Yes | PROVIDER/ADMIN | ✅ Live |

---

### 1.8 API Endpoints

#### Authentication (`/api/auth`)
- `POST /register` - User registration
- `POST /login` - User login
- `POST /logout` - User logout
- `POST /forgot-password` - Request password reset
- `POST /reset-password` - Reset password with token
- `GET /users/me` - Get current user
- `POST /refresh` - Refresh JWT token

#### Events (`/api/events`)
- `GET /events` - List all published events (public)
- `GET /events/grouped` - Events grouped by category
- `GET /events/{eventId}/booking` - Booking details with seat map
- `POST /events` - Create new event (PROVIDER/ADMIN)
- `GET /my-events` - Provider's own events

#### Provider/Organization (`/api/providers`)
- `POST /providers/register` - Request provider verification
- `POST /providers/verify` - Verify business email
- `GET /providers/me` - Get my organizations

#### Checkout (`/api/checkout`)
- `POST /checkout/preview` - Preview order before payment
- `POST /checkout/confirm` - Confirm order and initiate payment
- `POST /checkout/payos/complete` - PayOS webhook handler

#### Membership (`/api/membership`)
- `GET /membership/plans` - List membership plans
- `GET /me/membership` - Current membership status
- `POST /me/membership/subscribe` - Subscribe to plan

#### User Profile (`/api/users`)
- `GET /users/me` - Current user details
- `PUT /users/{userId}` - Update user profile

---

## 2. Next Development Phases

### Phase 2: Admin Dashboard & Provider Approval System

#### 2.1 Database Changes Required
```sql
-- Provider approval workflow
ALTER TABLE users ADD COLUMN provider_approval_status VARCHAR(20) DEFAULT NULL;
-- Values: 'PENDING', 'APPROVED', 'REJECTED'
ALTER TABLE users ADD COLUMN provider_approved_at TIMESTAMP NULL;
ALTER TABLE users ADD COLUMN provider_approval_notes TEXT NULL;

-- Event publication workflow
ALTER TABLE events ADD COLUMN publish_status VARCHAR(20) DEFAULT 'AUTO_PUBLISHED';
-- Values: 'AUTO_PUBLISHED', 'PENDING_REVIEW', 'APPROVED', 'REJECTED'
ALTER TABLE events ADD COLUMN submitted_at TIMESTAMP NULL;
ALTER TABLE events ADD COLUMN reviewed_at TIMESTAMP NULL;
ALTER TABLE events ADD COLUMN review_notes TEXT NULL;
```

#### 2.2 Backend Implementation

**Provider Approval Feature**
- Endpoint: `POST /api/admin/providers/{userId}/approve` (ADMIN only)
- Endpoint: `POST /api/admin/providers/{userId}/reject` (ADMIN only)
- Endpoint: `GET /api/admin/providers/pending` (ADMIN only)
- Logic:
  - When user verifies business email → set `provider_approval_status = 'PENDING'`
  - ADMIN reviews pending providers
  - On approval → set `provider_approval_status = 'APPROVED'` and `provider_role = PROVIDER`
  - On rejection → set `provider_approval_status = 'REJECTED'` and notify user

**Event Approval Feature**
- Endpoint: `GET /api/admin/events/pending` (ADMIN only)
- Endpoint: `POST /api/admin/events/{eventId}/publish` (ADMIN only)
- Endpoint: `POST /api/admin/events/{eventId}/reject` (ADMIN only)
- Logic:
  - When PROVIDER creates event → set `publish_status = 'PENDING_REVIEW'`, don't publish
  - ADMIN reviews pending events
  - On approval → set `publish_status = 'APPROVED'` and `status = 'PUBLISHED'`
  - On rejection → set `publish_status = 'REJECTED'` and notify provider

**Admin Dashboard Endpoints**
- `GET /api/admin/dashboard/stats` - Overview stats (users, events, orders, revenue)
- `GET /api/admin/users` - User management list
- `GET /api/admin/users/{userId}/details` - User details
- `POST /api/admin/users/{userId}/block` - Block/unblock user
- `GET /api/admin/categories` - Manage categories
- `POST /api/admin/categories` - Create category
- `PUT /api/admin/categories/{categoryId}` - Update category
- `DELETE /api/admin/categories/{categoryId}` - Delete category
- `GET /api/admin/events` - All events with filters
- `GET /api/admin/events/{eventId}/details` - Event details
- `GET /api/admin/orders` - Order management
- `GET /api/admin/revenue` - Revenue analytics

#### 2.3 Frontend Implementation

**Admin Dashboard Structure**
```
/admin (new route)
├── /admin/dashboard - Overview & stats
├── /admin/providers/pending - Pending provider approvals
├── /admin/events/pending - Pending event reviews
├── /admin/users - User management
├── /admin/categories - Category management
├── /admin/orders - Order management & refunds
└── /admin/analytics - Revenue & insights
```

**Key Components to Create**
- `AdminLayout.vue` - Sidebar navigation
- `AdminDashboardView.vue` - Stats dashboard
- `PendingProvidersView.vue` - Provider approval queue
- `PendingEventsView.vue` - Event approval queue
- `UserManagementView.vue` - User list, search, block/unblock
- `CategoryManagementView.vue` - CRUD operations
- `OrderManagementView.vue` - Order details, refund processing
- `AnalyticsView.vue` - Charts and revenue tracking

**Admin Store Module**
```javascript
// Store for admin operations
const adminStore = defineStore('admin', () => {
  const pendingProviders = ref([]);
  const pendingEvents = ref([]);
  const dashboardStats = ref({});
  
  async function fetchPendingProviders() {...}
  async function approveProvider(userId) {...}
  async function rejectProvider(userId, notes) {...}
  async function fetchPendingEvents() {...}
  async function approveEvent(eventId) {...}
  async function rejectEvent(eventId, notes) {...}
});
```

---

### Phase 3: Provider Features Enhancement

#### 3.1 Provider Dashboard
- Provider statistics (events, revenue, ticket sales)
- Event analytics (booking trends, seat utilization)
- Upcoming events calendar
- Quick event creation
- Event editing and management
- Revenue reports

#### 3.2 Cinema Management
- Multiple cinema locations per provider
- Different showtimes per location
- Seat capacity management
- Pricing strategies
- Promo codes specific to events

---

### Phase 4: Advanced Features

#### 4.1 Promotions & Discounts
- ADMIN creates global promotions
- PROVIDER creates event-specific discounts
- Voucher codes
- Early bird pricing
- Volume discounts

#### 4.2 Reviews & Ratings
- Customer reviews for events
- Star ratings
- Review moderation by ADMIN

#### 4.3 Notifications
- Email notifications for order confirmations
- Reminder emails before event start
- In-app notifications
- SMS notifications (optional)

#### 4.4 Advanced Analytics
- ADMIN: System-wide analytics
- PROVIDER: Provider revenue dashboard
- CUSTOMER: Personal booking history

#### 4.5 Refund & Return Management
- CUSTOMER refund requests
- PROVIDER approval workflow
- ADMIN override capabilities

---

## 3. Security Considerations

### Current Implementation
- ✅ HttpOnly JWT cookies (prevents XSS attacks)
- ✅ CSRF protection via SameSite attribute
- ✅ Password hashing
- ✅ Email verification for accounts
- ✅ reCAPTCHA bot protection
- ✅ Rate limiting capability via Redis
- ✅ SQL injection prevention via Hibernate ORM

### To Implement
- ❌ API rate limiting per user
- ❌ ADMIN action audit logging
- ❌ Two-factor authentication
- ❌ Account lockout after failed attempts
- ❌ Event subscription fraud detection
- ❌ Payment verification logging

---

## 4. Deployment & Environment Setup

### Local Development
```bash
# Prerequisites
- Docker & Docker Compose
- Java 21
- Node.js (v20.19.0+)
- PostgreSQL (via Docker)
- Redis (via Docker)

# Start services
docker compose up -d

# Run backend
cd ticket && mvn spring-boot:run

# Run frontend
cd ticketFrontend && npm install && npm run dev

# Access
- Frontend: http://localhost:5173
- Backend: http://localhost:8080
- PostgreSQL: localhost:5432
- Redis: localhost:6379
```

### Environment Configuration
- Backend: `ticket/src/main/resources/application.yml`
- Frontend: `ticketFrontend/.env` (environment variables)
- Database migrations: `ticket/src/main/resources/db/migration/`

---

## 5. Database Migrations History

| Version | Description | Status |
|---------|-------------|--------|
| V1 | Initial schema (users, events, seats, orders) | ✅ |
| V1_1 | Add PROVIDER role enum | ✅ |
| V1_2 | Rename organizer_id → provider_id | ✅ |
| V2 | Seed sample data | ✅ |
| V3 | Enhanced cinema layout | ✅ |
| V4 | Cinema-like booking layout | ✅ |
| V5 | Rebrand seed to movies | ✅ |
| V6 | Expand movie catalog | ✅ |
| V7 | Backfill missing event columns | ✅ |
| V8 | Backfill organization_id | ✅ |
| V9 | Add success order status for PayOS | ✅ |
| V10 | Update paid constraint | ✅ |
| V11 | Add PayOS order code | ✅ |
| V12 | Normalize seat prices | ✅ |
| V13 | Add movie duration & listing type | ✅ |
| V14 | Harden movie listing defaults | ✅ |

---

## 6. Key Dependencies

### Backend
- Spring Boot 3.5.14
- Spring Security
- Spring Data JPA
- PostgreSQL Driver
- JWT (jjwt 0.12.6)
- PayOS SDK 1.0.3
- Flyway (DB Migrations)
- Spring Mail
- Spring OAuth2
- Redis

### Frontend
- Vue 3.5.32
- Vue Router 5.0.4
- Pinia 3.0.4
- Axios 1.16.1
- Tailwind CSS 3.4.19
- Vite 8.0.8

---

## 7. Performance & Scalability Notes

### Current Optimizations
- ✅ Database indexes on frequently queried columns
- ✅ Atomic seat locking with versioning
- ✅ Redis caching layer
- ✅ JWT refresh token mechanism
- ✅ Lazy loading of routes in frontend

### Future Improvements
- Add pagination to all list endpoints
- Implement database query optimization
- Add caching for categories and static data
- Consider microservices for payment processing
- CDN for media (banner images)

---

## 8. Timeline & Prioritization

### High Priority (Next Sprint)
1. **Admin role implementation** - Core feature
2. **Provider approval workflow** - Business requirement
3. **Event approval system** - Quality control
4. **Admin dashboard MVP** - Management tool
5. **User management panel** - Control system users

### Medium Priority (Following Sprint)
6. Provider dashboard & analytics
7. Cinema management features
8. Basic promotions system
9. Review & rating system
10. Email notification improvements

### Low Priority (Future)
11. Advanced analytics & reports
12. Two-factor authentication
13. Fraud detection system
14. Mobile app integration
15. Internationalization (i18n)

---

## 9. Testing Strategy

### Current Status
- ✅ API endpoint testing framework in place
- ❌ Comprehensive test suite needed

### Required Tests
- Unit tests for service layer
- Integration tests for API endpoints
- E2E tests for critical user journeys
  - Customer: Register → Browse → Book → Checkout
  - Provider: Register → Create Event → Publish
  - Admin: Approve Provider → Review Event
- Database migration tests
- Authentication/Authorization tests

---

## 10. Known Limitations & Technical Debt

### MVP Limitations
- Events publish immediately (no admin review)
- No provider approval workflow
- No refund system
- No analytics/reporting
- Single organization per provider (hardcoded in some views)
- No bulk operations for admin

### Technical Debt
- Missing comprehensive error handling
- Limited validation messages
- No audit logging
- Some code duplication in frontend
- Need more inline documentation

---

## 11. Contact & Support

- **Documentation:** `docs/` folder
- **Database Schema:** `docs/TicketRush.dbml`
- **Auth Endpoints:** `docs/PHAN_1_AUTH_ENDPOINTS.md`
- **Config:** `ticket/src/main/resources/application.yml`

---

**Document Version:** 1.0  
**Status:** In Active Development  
**Last Review:** May 16, 2026
