# TicketRush - Project Summary & Development Roadmap

**Last Updated:** May 18, 2026  
**Project Type:** Full-Stack Web Application for Multi-Event Ticket Booking & Provider Management  
**Tech Stack:**
- **Backend:** Java 21, Spring Boot 3.5.14, PostgreSQL, Redis, Flyway (DB Migrations)
- **Frontend:** Vue 3, Vite, Vue Router, Pinia (State Management), Tailwind CSS, Axios
- **Payment:** PayOS Integration
- **Authentication:** JWT (HttpOnly Cookies), Google OAuth2, Email-based verification

---

## 🆕 Latest Updates (May 18, 2026)

### **Database Migrations (V16 & V17)**

**V16__restore_multi_event_catalog.sql** - Rebrand từ Movie-Only sang Multi-Event Ticketing:
- ✅ Cập nhật 8 Categories: Music, Show, Concert, Cinema, Sport, Festival, Theater, Workshop
- ✅ Cập nhật 8 Cinema Halls với venue types: SEATED, ARENA, VIP
- ✅ Cập nhật Membership Plans descriptions
- ✅ Seed **26 sample events** (xem danh sách dưới đây)

**26 Sample Events Seeded (by Category):**

| # | Event Name | Category | City | Hall | Type |
|----|-----------|----------|------|------|------|
| 1 | Indie Night Live | Music | Ha Noi | Main Hall | NOW_SHOWING |
| 2 | Little Moon Adventure | Cinema | Ha Noi | Starlight Cinema | NOW_SHOWING |
| 3 | Midnight Stage Mystery | Theater | Ha Noi | Grand Stage | NOW_SHOWING |
| 4 | Illusion Night VIP | Show | Ha Noi | VIP Lounge | SPECIAL |
| 5 | Saigon Basketball Cup | Sport | Ho Chi Minh | Saigon Arena | NOW_SHOWING |
| 6 | Starship Pop Concert | Concert | Ha Noi | Concert Arena | NOW_SHOWING |
| 7 | Creative Design Workshop | Workshop | Ha Noi | Innovation Hub | NOW_SHOWING |
| 8 | Laughing Saigon Comedy | Show | Ho Chi Minh | Saigon Stage | NOW_SHOWING |
| 9 | Ocean Food Festival | Festival | Ho Chi Minh | Festival Yard | NOW_SHOWING |
| 10 | Planet Blue Talk | Workshop | Ha Noi | Innovation Hub | NOW_SHOWING |
| 11 | Dragon School Cinema Day | Cinema | Ha Noi | Starlight Cinema | NOW_SHOWING |
| 12 | Night Corridor Play | Theater | Ha Noi | Main Hall | NOW_SHOWING |
| 13 | Fast Lane Esports Final | Sport | Ho Chi Minh | Saigon Arena | UPCOMING |
| 14 | Parallel Beats Concert | Concert | Ha Noi | Concert Arena | UPCOMING |
| 15 | Golden Kitchen Food Fair | Festival | Ho Chi Minh | Festival Yard | UPCOMING |
| 16 | The Silent Bridge VIP Show | Show | Ha Noi | VIP Lounge | SPECIAL |
| 17 | Wild Mekong Marathon | Sport | Ho Chi Minh | Saigon Arena | UPCOMING |
| 18 | Robot Cat Holiday Cinema | Cinema | Ha Noi | Starlight Cinema | UPCOMING |
| 19 | After Midnight DJ Set | Music | Ho Chi Minh | Gold Lounge | SPECIAL |
| 20 | Love on Platform 9 Musical | Theater | Ha Noi | Grand Stage | UPCOMING |
| 21 | Skyfall District Bike Race | Sport | Ho Chi Minh | Saigon Arena | UPCOMING |
| 22 | Tiny Theatre Club | Show | Ho Chi Minh | Gold Lounge | SPECIAL |
| 23 | Deep Space Arena Concert | Concert | Ha Noi | Concert Arena | UPCOMING |
| 24 | River of Light Festival | Festival | Ho Chi Minh | Festival Yard | UPCOMING |
| 25 | Penguins in Hanoi Cinema | Cinema | Ha Noi | Starlight Cinema | UPCOMING |
| 26 | The Last Reef Masterclass | Workshop | Ho Chi Minh | Innovation Hub | Ho Chi Minh | UPCOMING |

**V17__add_event_detail_metadata.sql** - Thêm metadata fields cho events:
- ✅ **7 new columns:**
  - `genre` - Thể loại/thể loại (e.g., "Pop Concert", "Comedy Show")
  - `country` - Quốc gia sản xuất (e.g., "Vietnam", "South Korea")
  - `author_name` - Tác giả/Nhà sáng lập
  - `director_name` - Đạo diễn
  - `cast_members` - Diễn viên (comma-separated)
  - `performer_names` - Nghệ sĩ biểu diễn
  - `singer_names` - Ca sĩ/Thợ hát
- ✅ Populated metadata cho tất cả 26 events

### **Backend Changes (Java)**

**Event Entity Enhancement** (`Event.java`):
- ✅ Thêm 7 metadata fields (genre, country, authorName, directorName, castMembers, performerNames, singerNames)
- ✅ `durationMinutes` - Thời lượng sự kiện (phút)
- ✅ `listingType` - Loại hiển thị: NOW_SHOWING | UPCOMING | SPECIAL

**API Endpoint** (`EventController.java`):
- ✅ New endpoint: `GET /api/events/slug/{slug}` - Lấy chi tiết sự kiện theo slug

**Service Layer** (`EventServiceImpl.java`):
- ✅ New method: `eventBySlug(String slug)` - Trả về EventResponse đầy đủ

**Query Repository** (`EventQueryRepository.java`):
- ✅ Updated: `findPublishedEventForBooking()` - Now filters out UPCOMING events
- ✅ Duration calculation: `COALESCE(e.duration_minutes, GREATEST(1, EXTRACT(EPOCH FROM (e.end_time - e.start_time)) / 60))`

### **Frontend Changes (Vue)**

**New Route** (`router/index.js`):
- ✅ `/events/:slug` → EventDetailView.vue (customer surface, public)

**New API Function** (`ticketRushApi.js`):
- ✅ `getEventBySlug(slug)` - Fetch event details by slug

**New Component** (`EventDetailView.vue`) - Event Detail Page:
- ✅ **Hero Section:**
  - Event banner image
  - Category badge + Listing type badge (Now Showing/Upcoming/Special)
  - Event title + Description
  - Start time + Venue location

- ✅ **Key Facts Section:**
  - Category, Genre, Country, Duration, Price range (from minimum section price)

- ✅ **People Facts Section:**
  - Author / Creator, Director, Cast / Speakers, Performers, Singers

- ✅ **Booking Actions:**
  - "Buy ticket" button (nếu event đang sale)
  - "Coming soon" badge (nếu chưa sale)
  - Back to events link

---

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

---

## Phần 2: Chi Tiết Database Schema, Customer/Provider Models, và Luồng Tạo/Hiển Thị Sự Kiện

### 2.0 Event Categories (Cập nhật May 18, 2026)

Hệ thống hiện hỗ trợ **8 event categories** (không chỉ cinema):

| Category | Slug | Description | Use Case |
|----------|------|-------------|----------|
| **Music** | music | Live music nights, DJ sets, and acoustic sessions | Concerts, DJ nights, live bands |
| **Show** | show | Comedy, magic, variety, and limited stage shows | Comedy shows, magic shows, variety acts |
| **Concert** | concert | Large concerts, arena tours, and premium live performances | Large concerts, arena tours |
| **Cinema** | cinema | Movie screenings and cinema ticket bookings | Movie screenings |
| **Sport** | sport | Sports matches, tournaments, and esports finals | Basketball, bike races, esports |
| **Festival** | festival | Food, art, outdoor, and community festivals | Food festivals, lantern festivals |
| **Theater** | theater | Stage plays, musicals, and live theater | Plays, musicals |
| **Workshop** | workshop | Workshops, talks, classes, and professional events | Design workshops, talks, masterclasses |

### 2.1 Cinema Halls/Venues (Cập nhật May 18, 2026)

Hệ thống quản lý **8 venue types** với khác nhau screen types:

| Hall Name | Slug | Screen Type | Capacity Purpose |
|-----------|------|-------------|-------------------|
| Main Hall | main-hall | SEATED | Standard theater seating |
| Premium Hall | premium-hall | SEATED | Premium/VIP seating |
| Arena Floor | arena-floor | ARENA | Standing room arena |
| Grand Stage | grand-stage | SEATED | Theater stage |
| Concert Arena | concert-arena | ARENA | Large concert arena |
| Saigon Stage | saigon-stage | SEATED | Stage performance venue |
| VIP Lounge | vip-lounge | VIP | VIP exclusive lounge |
| Gold Lounge | gold-lounge | VIP | Premium gold class lounge |

### 2.1 Database Schema Tổng Quan

Hệ thống dùng PostgreSQL làm nguồn chân lý chính (source of truth) với Redis hỗ trợ caching, token management, rate limiting.

#### Users Table Schema

```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255),                    -- NULL nếu dùng Google OAuth
    full_name VARCHAR(255) NOT NULL,
    avatar_url TEXT,
    phone VARCHAR(30),
    gender VARCHAR(20),
    date_of_birth DATE,
    
    -- Role & Status
    role user_role NOT NULL DEFAULT 'CUSTOMER',   -- CUSTOMER | PROVIDER | ADMIN
    status user_status NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE | BLOCKED
    
    -- Authentication Provider
    provider auth_provider NOT NULL DEFAULT 'LOCAL', -- LOCAL | GOOGLE
    provider_id VARCHAR(255),                      -- Google subject ID nếu OAuth
    
    -- Email Verification
    email_verified BOOLEAN NOT NULL DEFAULT false,
    
    -- Provider Upgrade Flow
    primary_organization_id UUID,                  -- Tổ chức chính
    provider_request_status VARCHAR(30),           -- PENDING | APPROVED | REJECTED
    provider_requested_at TIMESTAMPTZ,
    provider_reviewed_at TIMESTAMPTZ,
    provider_reviewed_by UUID,
    provider_rejection_reason TEXT,
    
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

**Key Fields for Customer/Provider Distinction:**
- **CUSTOMER**: `role = 'CUSTOMER'`, có thể có `provider_request_status = 'PENDING'` khi chờ duyệt
- **PROVIDER**: `role = 'PROVIDER'`, `provider_request_status = 'APPROVED'`, có `primary_organization_id`
- **ADMIN**: `role = 'ADMIN'`, quản lý hệ thống

---

### 2.2 Organizations Table (Provider Profiles)

```sql
CREATE TABLE organizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,                    -- Tên tổ chức (Rạp chiếu phim)
    business_email VARCHAR(255) NOT NULL UNIQUE,
    owner_id UUID NOT NULL REFERENCES users(id),
    verified_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

**Purpose:** Mỗi PROVIDER có ít nhất 1 organization để quản lý sự kiện.

---

### 2.3 Events Table (Sự Kiện/Bộ Phim)

```sql
CREATE TABLE events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Provider/Ownership
    provider_id UUID NOT NULL REFERENCES users(id),
    organization_id UUID REFERENCES organizations(id),
    category_id BIGINT NOT NULL REFERENCES categories(id),
    
    -- Event Information
    title VARCHAR(255) NOT NULL,
    slug VARCHAR(300) NOT NULL UNIQUE,             -- URL-friendly slug
    description TEXT,
    banner_url TEXT,
    
    -- 🆕 Event Metadata (May 18, 2026)
    genre VARCHAR(120),                            -- "Pop Concert", "Comedy Show", "Basketball"
    country VARCHAR(120),                          -- "Vietnam", "South Korea", "United States"
    author_name VARCHAR(255),                      -- Author/Creator/Organizer
    director_name VARCHAR(255),                    -- Director
    cast_members TEXT,                             -- Cast (comma-separated)
    performer_names TEXT,                          -- Performers (comma-separated)
    singer_names TEXT,                             -- Singers (comma-separated)
    
    -- Location & Venue
    location_name VARCHAR(255),                    -- Tên địa điểm (Rạp Lotte Cinema)
    address TEXT,
    city VARCHAR(100),
    
    -- Timing
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    duration_minutes INT,                          -- 🆕 Duration in minutes
    
    -- Ticket Sales Window
    sale_start_time TIMESTAMPTZ,                   -- Khi nào bắt đầu bán vé
    sale_end_time TIMESTAMPTZ,                     -- Khi nào dừng bán vé
    
    -- Event Metadata
    listing_type VARCHAR(30) NOT NULL,             -- NOW_SHOWING | UPCOMING | SPECIAL
    status event_status NOT NULL DEFAULT 'DRAFT',  -- DRAFT | PUBLISHED | CANCELLED | FINISHED
    
    -- Seat Management Provider
    seat_provider VARCHAR(30) NOT NULL DEFAULT 'INTERNAL', -- INTERNAL | SEATS_IO
    external_seat_chart_key VARCHAR(255),          -- Key từ seats.io
    
    -- Payout Information
    payout_bank_name VARCHAR(120),
    payout_account_name VARCHAR(120),
    payout_account_number VARCHAR(60),
    provider_terms_accepted_at TIMESTAMPTZ,
    
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

**Notable Fields (May 18 Update):**
- **genre**: Thể loại event (e.g., "Pop Concert", "Stand-up Comedy", "Basketball")
- **country**: Quốc gia sản xuất/tổ chức
- **authorName / directorName**: Tác giả/Đạo diễn
- **castMembers / performerNames / singerNames**: Nhân sự tham gia
- **durationMinutes**: Thời lượng sự kiện
- **listingType**: NOW_SHOWING (đang bán vé) | UPCOMING (sắp diễn ra) | SPECIAL (sự kiện đặc biệt)

**Indexes cho Performance:**
- `idx_events_provider_id`: Tìm sự kiện của provider
- `idx_events_status_start_time`: Tìm PUBLISHED events sắp tới
- `idx_events_city`: Tìm theo thành phố
- `idx_events_title_trgm`: Full-text search tiếng Việt

---

### 2.4 Event Sections Table (Khu Ghế)

```sql
CREATE TABLE event_sections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    
    name VARCHAR(100) NOT NULL,                    -- "VIP", "Standard", "Economy"
    base_price NUMERIC(12,2) NOT NULL,             -- Giá cơ bản của khu này
    
    -- Seat Grid
    row_count INT NOT NULL,                        -- Số hàng (A-J = 10 hàng)
    seats_per_row INT NOT NULL,                    -- Ghế mỗi hàng
    
    display_order INT NOT NULL DEFAULT 0,
    
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    
    CONSTRAINT uq_event_sections_event_name UNIQUE (event_id, name)
);
```

**Example:**
```
Event: "Đất của chúng ta" at Rạp Galaxy
├─ Section "VIP"
│  └─ base_price: 170.000 VND
│  └─ rows: 6 (D-I), 10 ghế/hàng
├─ Section "Standard"  
│  └─ base_price: 120.000 VND
│  └─ rows: 10 (A-C, J), 14 ghế/hàng
└─ Section "Couple"
   └─ base_price: 250.000 VND
   └─ rows: 1 (K), 12 ghế/hàng (ghế đôi)
```

---

### 2.5 Event Seats Table (Ghế Cụ Thể)

```sql
CREATE TABLE event_seats (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    event_id UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    section_id UUID NOT NULL REFERENCES event_sections(id) ON DELETE CASCADE,
    
    -- Seat Position
    row_label VARCHAR(10) NOT NULL,                -- "A", "B", ... "K"
    seat_number INT NOT NULL,                      -- 1, 2, 3, ..., 14
    seat_code VARCHAR(40) NOT NULL,                -- "A1", "B5", "VIP-D3"
    
    -- Price (có thể khác với base_price của section)
    price NUMERIC(12,2) NOT NULL,
    
    -- Seat Status
    status seat_status NOT NULL DEFAULT 'AVAILABLE', -- AVAILABLE | LOCKED | SOLD
    
    -- Locking Mechanism (để 15 phút khi customer chọn ghế)
    locked_by UUID REFERENCES users(id),
    lock_expires_at TIMESTAMPTZ,
    
    -- Optimistic Locking (ngăn race condition)
    version INT NOT NULL DEFAULT 0,
    
    -- Seat Type & Layout
    seat_type_code VARCHAR(30),                    -- "STANDARD" | "VIP" | "COUPLE" | "WHEELCHAIR"
    layout_x INT,                                  -- Tọa độ X trên UI
    layout_y INT,                                  -- Tọa độ Y trên UI
    is_hidden BOOLEAN DEFAULT false,
    is_accessible BOOLEAN DEFAULT false,
    
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    
    CONSTRAINT uq_event_seats_event_seat_code UNIQUE (event_id, seat_code),
    CONSTRAINT uq_event_seats_event_section_row_seat UNIQUE (event_id, section_id, row_label, seat_number)
);
```

**Seat Status Flow:**
```
AVAILABLE (mặc định)
    ↓ (Customer chọn trong BookView)
LOCKED (15 phút, `lock_expires_at` = now + 15min)
    ↓ (Customer thanh toán thành công)
SOLD (đã bán, order_items tham chiếu)
    
Nếu 15 phút hết + chưa thanh toán → LOCKED → AVAILABLE (scheduler mở khóa)
```

**Seat Type Multipliers:**
| Type | Display Name | Price Multiple | Pair? | Accessible? |
|------|-------------|-----------------|-------|-------------|
| STANDARD | Standard Seat | 1.000 | No | No |
| VIP | Premium Seat | 1.350 | No | No |
| COUPLE | Couple Seat | 1.700 | Yes | No |
| SWEETBOX | Luxury Pair | 1.900 | Yes | No |
| WHEELCHAIR | Accessible | 0.950 | No | Yes |

---

### 2.6 Orders & Order Items (Hóa Đơn & Vé)

```sql
CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    user_id UUID NOT NULL REFERENCES users(id),
    event_id UUID NOT NULL REFERENCES events(id),
    
    -- Order Amounts
    subtotal NUMERIC(12,2) NOT NULL DEFAULT 0,        -- Tổng giá ghế
    discount_amount NUMERIC(12,2) NOT NULL DEFAULT 0,  -- Chiết khấu
    total_amount NUMERIC(12,2) NOT NULL DEFAULT 0,     -- = subtotal - discount
    
    voucher_id UUID REFERENCES vouchers(id),
    
    -- Order Status
    status order_status NOT NULL DEFAULT 'PENDING', -- PENDING | PAID | EXPIRED | CANCELLED
    
    -- Timing
    expires_at TIMESTAMPTZ NOT NULL,               -- Order hết hạn nếu không thanh toán
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    paid_at TIMESTAMPTZ,                           -- Khi nào thanh toán xong
    cancelled_at TIMESTAMPTZ
);

CREATE TABLE order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    event_seat_id UUID NOT NULL REFERENCES event_seats(id),
    
    -- Price snapshot (lưu giá tại thời điểm đặt vé)
    price_snapshot NUMERIC(12,2) NOT NULL,
    
    -- Ticket Details
    qr_code VARCHAR(255) UNIQUE,                   -- QR code của vé
    ticket_status ticket_status NOT NULL DEFAULT 'NOT_ISSUED', -- NOT_ISSUED | VALID | USED | CANCELLED
    
    issued_at TIMESTAMPTZ,                         -- Khi nào vé được phát hành
    checked_in_at TIMESTAMPTZ,                     -- Khi nào vé được sử dụng tại venue
    
    CONSTRAINT uq_order_items_order_event_seat UNIQUE (order_id, event_seat_id)
);
```

**Order Status Flow:**
```
PENDING (mới tạo, chờ thanh toán, expire trong 30 phút)
    ↓ (Customer thanh toán thành công via PayOS webhook)
PAID (thanh toán xong, order_items.ticket_status = VALID, QR được phát hành)
    
Nếu 30 phút hết + chưa PAID → EXPIRED (scheduler)
Customer có thể CANCEL bất kỳ lúc nào
```

---

### 2.7 Seat Pricing Model (Mô Hình Giá Vé)

**Giá cuối cùng của 1 ghế = base_price × seat_type_multiplier**

**Example:**
```
Section "VIP": base_price = 170.000 VND
  - STANDARD seat: 170.000 × 1.000 = 170.000 VND
  - VIP seat: 170.000 × 1.350 = 229.500 VND  
  - COUPLE seat: 170.000 × 1.700 = 289.000 VND
  - WHEELCHAIR: 170.000 × 0.950 = 161.500 VND

Section "Standard": base_price = 120.000 VND
  - STANDARD seat: 120.000 × 1.000 = 120.000 VND
  - VIP seat: 120.000 × 1.350 = 162.000 VND
  - COUPLE seat: 120.000 × 1.700 = 204.000 VND
```

**Lưu ý:** Khi tạo sự kiện, backend tính giá từ `event_seats.price` (đã tính multiplier sẵn).

---

## Phần 3: Luồng Tạo Sự Kiện (Event Creation Flow)

### 3.1 Frontend - Create Event Wizard (3 bước)

**Route:** `/create-movie` (Chỉ PROVIDER/ADMIN)

#### Bước 1: Event Information

```javascript
form = {
  title: "Đất của chúng ta",
  description: "Bộ phim tài liệu...",
  bannerUrl: "https://...",
  categoryId: "1",                    // ID category
  durationMinutes: 120,
  listingType: "NOW_SHOWING",         // NOW_SHOWING | COMING_SOON
  locationName: "Rạp Lotte Galaxy",
  city: "Hà Nội",
  address: "Tầng 6, Lotte Center",
  startTime: "2026-06-20T19:00:00Z",
  endTime: "2026-06-20T20:30:00Z",
  saleStartTime: "2026-06-01T00:00:00Z",
  saleEndTime: "2026-06-20T18:00:00Z"  // Dừng bán vé 1h trước
}
```

#### Bước 2: Seating Configuration

**2 Options:**

**A. Internal Seat Map (Mặc định)**
```javascript
form.seatProvider = "INTERNAL";
form.sections = [
  { 
    name: "VIP", 
    basePrice: 170000, 
    rowCount: 6,        // Hàng D-I
    seatsPerRow: 10 
  },
  { 
    name: "Standard", 
    basePrice: 120000, 
    rowCount: 10,       // Hàng A-C, J
    seatsPerRow: 14 
  },
  { 
    name: "Couple", 
    basePrice: 250000, 
    rowCount: 1,        // Hàng K (ghế đôi)
    seatsPerRow: 12 
  }
];
```

**B. Seats.io Integration**
```javascript
form.seatProvider = "SEATS_IO";
form.externalSeatChartKey = "chart-key-abc123";
form.sections = [
  { 
    name: "VIP", 
    basePrice: 170000,
    rowCount: 1,   // Dummy, seats.io sẽ quản lý
    seatsPerRow: 1
  }
];
```

#### Bước 3: Payout & Confirmation

```javascript
form = {
  payoutBankName: "VCB",
  payoutAccountName: "TicketRush Live",
  payoutAccountNumber: "0123456789",
  termsAccepted: true
}
```

---

### 3.2 Backend - Create Event Process

**Endpoint:** `POST /api/events`

**Permission Check:**
```
if (user.role != PROVIDER && user.role != ADMIN) {
    throw ForbiddenException("Provider access required")
}
```

**Validation:**
1. Category exists
2. Seat provider is valid (INTERNAL or SEATS_IO)
3. Nếu INTERNAL: ít nhất 1 section
4. Nếu SEATS_IO: external_seat_chart_key bắt buộc

**Event Creation Logic:**

```
1. Create Event entity
   - provider_id = current user
   - category_id = request
   - status = PUBLISHED (MVP, nhưng có thể sửa thành DRAFT sau)
   - seat_provider = INTERNAL | SEATS_IO
   - start_time, end_time, etc.

2. Create EventSections
   For each section in request.sections:
   - name, base_price, row_count, seats_per_row

3. Create EventSeats (nếu INTERNAL)
   For each section:
     For row 0 to row_count-1:
       rowLabel = intToRowLabel(row)  // 0→A, 1→B, ...
       For seatNumber 1 to seats_per_row:
         Create EventSeat:
           - seat_code = "{section.name}-{rowLabel}{seatNumber}"
           - price = section.base_price
           - status = AVAILABLE
           - row_label, seat_number

4. Nếu SEATS_IO:
   - Không tạo ghế cụ thể
   - Backend lưu external_seat_chart_key
   - Khi booking, frontend/backend sync với seats.io SDK

5. Return EventResponse
```

**Database Queries:**
```sql
-- Insert event
INSERT INTO events (...) VALUES (...)

-- Insert sections
INSERT INTO event_sections (...) VALUES (...)

-- Insert seats (bulk) - nếu INTERNAL
INSERT INTO event_seats (...) SELECT ... (massive bulk insert)
```

---

## Phần 4: Luồng Hiển Thị Sự Kiện & Chọn Ghế (Event Display & Booking Flow)

### 4.0 Event Detail Page (🆕 May 18, 2026)

**Route:** `/events/:slug` (Public, no auth required)

**Component:** `EventDetailView.vue`

**Features:**
1. **Hero Section:**
   - Event banner image (backdrop + overlay gradient)
   - Category badge (e.g., "Music", "Cinema", "Sport")
   - Listing type badge (Now Showing, Upcoming, Special)
   - Event title + Description
   - Start time + Venue location with city
   - "Buy Ticket" CTA button (if in sale window)

2. **Key Facts Section:**
   ```
   [Category] [Genre] [Country] [Duration] [Price from XXX VND]
   ```
   Displayed as interactive cards, filtered to show only available data

3. **People Facts Section:**
   ```
   Author/Creator → Director → Cast/Speakers → Performers → Singers
   ```
   Shows credits for event creators and performers

4. **Sale Window Logic:**
   ```
   canBook = event.listingType !== 'UPCOMING' 
             && (sale_start_time <= now)
             && (sale_end_time >= now)
   ```

**Data Loading:**
```javascript
GET /api/events/slug/{slug}
↓
EventResponse {
  id, title, slug, description, bannerUrl,
  categoryId, genre, country, durationMinutes, listingType,
  locationName, city, address,
  startTime, endTime, saleStartTime, saleEndTime,
  authorName, directorName, castMembers, 
  performerNames, singerNames,
  minPrice, availableSeats, soldSeats
}
```

---

### 4.1 Event Detail Page (Luồng Hiển Thị Sự Kiện & Chọn Ghế)

#### 4.1.1 Trang Danh Sách Sự Kiện (`/events`)

**Endpoint:** `GET /api/events?categoryId=...&query=...&city=...&page=0&size=12`

**Response:**
```json
{
  "content": [
    {
      "id": "uuid",
      "title": "Đất của chúng ta",
      "bannerUrl": "https://...",
      "city": "Hà Nội",
      "startTime": "2026-06-20T19:00:00Z",
      "availableSeats": 120,
      "totalSeats": 180
    }
  ],
  "page": 0,
  "size": 12,
  "totalElements": 450
}
```

**Frontend Rendering:**
- Hiển thị grid events
- Khi click → chuyển đến `/booking?eventId={eventId}`

#### 4.1.2 Trang Chọn Ghế (`/booking`)

**Endpoint:** `GET /api/events/{eventId}/booking`

**Response - BookingEventResponse:**
```json
{
  "id": "uuid",
  "slug": "dat-cua-chung-ta",
  "title": "Đất của chúng ta",
  "bannerUrl": "https://...",
  "location": "Rạp Lotte Galaxy, Hà Nội",
  "startTime": "2026-06-20T19:00:00Z",
  "saleStartTime": "2026-06-01T00:00:00Z",
  "saleEndTime": "2026-06-20T18:00:00Z",
  "availableSeats": 120,
  "soldSeats": 60,
  "sections": [
    {
      "id": "uuid-section-1",
      "name": "VIP",
      "basePrice": 170000,
      "rowCount": 6,
      "seatsPerRow": 10,
      "displayOrder": 1,
      "seats": [
        {
          "id": "uuid-seat-d1",
          "rowLabel": "D",
          "seatNumber": 1,
          "seatCode": "D1",
          "price": 170000,
          "status": "AVAILABLE",      // AVAILABLE | LOCKED | SOLD
          "seatTypeCode": "STANDARD",
          "layoutX": 1,
          "layoutY": 4,
          "hidden": false,
          "accessible": false
        },
        // ... more seats
      ]
    }
  ]
}
```

### 4.2 Frontend - Seat Selection UI

**Component:** `BookView.vue`

#### 4.2.1 Seat Map Rendering

```vue
<template>
  <!-- Display seats grouped by row -->
  <div v-for="rowGroup in rowGroups" class="flex gap-2">
    <span class="row-label">{{ rowGroup.rowLabel }}</span>
    <div class="seat-units">
      <div
        v-for="unit in rowGroup.units"
        :class="getSeatUnitClass(unit)"
        @click="toggleSeatUnit(unit)"
      >
        {{ unit.label }}
      </div>
    </div>
  </div>
</template>

<script>
// rowGroups được compute từ sections > seats
// Ghế COUPLE tự động pair lại (consecutive C1-C2, C3-C4, ...)
const rowGroups = computed(() => {
  // Group seats by rowLabel
  // Sort by layoutY, layoutX
  // Pair consecutive COUPLE seats
  // Calculate offset cho centered layout
})

function getSeatUnitClass(unit) {
  const leadSeat = unit.seats[0]
  
  if (isSelected(unit)) return "bg-blue-500 text-white"  // Selected
  
  if (!canSelect(unit)) return "bg-gray-200"             // SOLD/LOCKED
  
  if (leadSeat.seatTypeCode === "COUPLE") return "bg-rose-100"
  if (leadSeat.seatTypeCode === "VIP") return "bg-orange-100"
  return "bg-slate-100"
}

function toggleSeatUnit(unit) {
  if (!canSelect(unit)) return
  
  // Thêm/xóa tất cả ghế trong unit khỏi selectedSeats
  const allSelected = unit.seats.every(s => isSeatSelected(s.id))
  if (allSelected) {
    selectedSeats = selectedSeats.filter(s => !unit.seats.map(u => u.id).includes(s.id))
  } else {
    selectedSeats.push(...unit.seats.filter(s => !isSeatSelected(s.id)))
  }
}
```

#### 4.2.2 Timeout & Price Calculation

```javascript
// Timer: 10 phút để chọn ghế (nếu chọn, sẽ lock ghế)
const SELECT_TIMEOUT_SECONDS = 10 * 60

const totalPrice = computed(() => {
  return selectedSeats.reduce((sum, seat) => sum + seat.price, 0)
})

function continueToCheckout() {
  // Lưu selectedSeats vào sessionStorage
  sessionStorage.setItem(CHECKOUT_STORAGE_KEY, JSON.stringify({
    eventId: bookingEvent.id,
    seatIds: selectedSeats.map(s => s.id),
    selectedSeatCodes: selectedSeatCodes  // ["A1", "A2", "D3"]
  }))
  
  router.push('/checkout')
}
```

---

### 4.3 Frontend - Checkout Page

**Route:** `/checkout`

**Endpoint Call 1:** `POST /api/checkout/preview`

```json
{
  "eventId": "uuid",
  "seatIds": ["uuid-seat-1", "uuid-seat-2"],
  "voucherCode": "SUMMER20"  // Optional
}
```

**Response - CheckoutSummary:**
```json
{
  "eventId": "uuid",
  "eventTitle": "Đất của chúng ta",
  "seatCodes": ["A1", "A2", "D3"],
  "subtotal": 510000,
  "discountAmount": 50000,  // If voucher applied
  "totalAmount": 460000,
  "voucherCode": "SUMMER20"
}
```

**Endpoint Call 2:** `POST /api/checkout/confirm`

```json
{
  "eventId": "uuid",
  "seatIds": ["uuid-seat-1", "uuid-seat-2"],
  "voucherCode": "SUMMER20",
  "contact": {
    "fullName": "Nguyễn Văn A",
    "email": "user@example.com",
    "phone": "0987654321"
  },
  "paymentMethod": "card"  // "card" | "wallet"
}
```

**Backend Process:**
```
1. Validate seats still available
2. Lock seats: event_seats.status = LOCKED, lock_expires_at = now + 15min
3. Create Order (status = PENDING, expires_at = now + 30min)
4. Create OrderItems for each seat
5. Call PayOS API to initiate payment
6. Return PayOS payment URL
```

**PayOS Callback Webhook:**

```
PayOS redirects user back to:
  - /checkout/success?transactionId=... (if payment OK)
  - /checkout/cancel?transactionId=... (if user cancel)

Backend receives webhook:
  POST /api/checkout/payos/complete
  {
    "code": "00",
    "data": {
      "orderCode": "order-uuid",
      "paymentLinkCode": "...",
      "transactionDateTime": "...",
      "amount": 460000,
      "accountNumber": "..."
    }
  }

Backend process:
  1. Verify webhook signature
  2. Mark order.status = PAID
  3. Create order_items with ticket_status = VALID
  4. Generate QR code for each ticket
  5. Mark event_seats.status = SOLD
  6. Send email với QR code và vé
```

---

### 4.4 Seat Locking Mechanism

**Scenario 1: Customer chọn ghế trong BookView**

```
- Frontend: User click ghế A1, A2
- selectedSeats array hold references, chưa lock
- 10 phút timeout: nếu user không proceed → các ghế không bị lock
```

**Scenario 2: Customer proceed to checkout**

```
- Frontend: sessionStorage save seatIds
- Backend (POST /checkout/preview):
  - Query event_seats with seatIds
  - Check status all AVAILABLE
  - (Optional) Lock seats ở bước này với short TTL (~5 phút)

- Backend (POST /checkout/confirm):
  - Verify seats còn AVAILABLE hoặc chính user đã lock
  - Lock seats: UPDATE event_seats SET status='LOCKED', lock_expires_at=now+15min
  - Create order + order_items
  - Initiate PayOS payment

- If payment successful:
  - Seats→SOLD (permanent)
  
- If 15 phút hết mà chưa thanh toán:
  - Scheduler unlock: LOCKED→AVAILABLE
```

**Optimistic Locking (Version Control):**

```sql
UPDATE event_seats
SET status = 'LOCKED',
    lock_expires_at = now() + interval '15 minutes',
    version = version + 1
WHERE id = ?
AND version = ?   -- Kiểm tra version hiện tại
```

Điều này ngăn race condition nếu 2 request cùng update ghế.

---

## Phần 5: SEATS.IO Integration (Tích hợp Seats.io)

### 5.1 Architecture Overview

**Seats.io** là external service cho seat map designer & SDK.

**TicketRush Ownership:**
- TicketRush vẫn sở hữu event, order, ticket, seat, payment, QR
- Seats.io chỉ quản lý **visual chart** và **seat selection UI** ở phía frontend
- TicketRush vẫn là **source of truth** cho seat status & pricing

### 5.2 Event Creation với Seats.io

**Frontend - Step 2 (Seating):**

```javascript
form = {
  seatProvider: "SEATS_IO",
  externalSeatChartKey: "chart-key-from-seats.io",
  sections: [
    { 
      name: "Tất cả", 
      basePrice: 150000,
      rowCount: 1,      // Dummy values
      seatsPerRow: 1
    }
  ]
}
```

Provider tạo chart tại seats.io dashboard, sau đó paste chart key.

**Backend - POST /api/events:**

```java
if ("SEATS_IO".equals(seatProvider)) {
    // Không tạo event_seats
    // Lưu event.external_seat_chart_key
    // Tính toán totalSeats từ sections (hay lấy từ seats.io API sau)
    
    event.setSeatProvider("SEATS_IO");
    event.setExternalSeatChartKey(externalSeatChartKey);
}

// Tạo sections (để lưu base_price per section)
// Seats.io SDK sẽ render chart từ frontend
```

### 5.3 Booking với Seats.io

**Frontend:**

```vue
<!-- Import Seats.io SDK -->
<script src="https://cdn.seats.io/chart.js"></script>

<template>
  <div v-if="event.seatProvider === 'SEATS_IO'">
    <div id="chart"></div>
  </div>
</template>

<script>
new seatsio.SeatsChart({
  divId: "chart",
  chartKey: event.externalSeatChartKey,
  onObjectClick: (objectClickedEvent) => {
    // User click ghế trong seats.io chart
    selectedSeats.push({
      id: objectClickedEvent.object.id,  // Seats.io ID
      label: objectClickedEvent.object.label,  // "A1"
      price: calculatePrice(objectClickedEvent.object)
    })
  }
})

function calculatePrice(seatsioObject) {
  // Mapping seats.io object → TicketRush seat_type_code
  // Lấy base_price từ section
  // Return final price = base_price * multiplier
}
</script>
```

### 5.4 Checkout với Seats.io

**POST /api/checkout/preview:**

```json
{
  "eventId": "uuid",
  "seatsioObjectIds": ["obj-1", "obj-2"],   // Seats.io IDs
  "voucherCode": "SUMMER20"
}
```

**Backend:**

```java
// Resolve seats.io object IDs → TicketRush seat data
// Có thể sync từ seats.io API hoặc maintain mapping table

List<EventSeat> seats = resolveSeatsFromSeatsIO(event, seatsioObjectIds);
// Calculate subtotal từ seats[].price
// Apply voucher
// Return summary
```

**POST /api/checkout/confirm:**

```json
{
  "eventId": "uuid",
  "seatsioObjectIds": ["obj-1", "obj-2"],
  "contact": { ... }
}
```

**Backend - Important:**

```
1. Validate seats từ seats.io (not just local DB)
2. Sync với seats.io API: check availability/status
3. Lock seats: CREATE order_items with seatsio object ID
4. On payment success:
   - Mark order_items as PAID/VALID
   - Notify seats.io (via webhook) để update visual
   - Generate QR code
```

### 5.5 Key Considerations for Seats.io

**Pros:**
✅ Professional, customizable chart UI
✅ Support complex seat layouts
✅ Mobile-friendly
✅ Built-in analytics
✅ Webhook support

**Cons:**
⚠ Third-party dependency (pricing, SLA, data export)
⚠ Need to maintain mapping between seats.io IDs ↔ TicketRush seat data
⚠ Rate limits on API calls
⚠ Vendor lock-in risk

**Recommended Practices:**
1. Maintain local `event_seats` table as backup/mapping
2. Periodically sync chart from seats.io to verify seat counts
3. Use seats.io webhooks to keep UI in sync
4. Store `external_seat_chart_key` + `external_seat_object_id` mapping in DB
5. Implement fallback UI if seats.io is unavailable
6. Test webhook delivery reliability

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
