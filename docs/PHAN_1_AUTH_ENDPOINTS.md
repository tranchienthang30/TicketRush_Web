# TicketRush - Part 1: Auth API and Frontend Flow

This document describes the current Auth implementation. Tokens are stored only in `HttpOnly` cookies. The frontend must not read, write, or persist JWTs.

## Current Scope

- Local registration with email and password.
- Local login with email and password.
- `access_token` and `refresh_token` are set by the backend as `HttpOnly` cookies.
- Frontend sends cookies with requests through Axios `withCredentials: true`.
- Frontend keeps only the `user` object in Pinia memory.
- Auth state is restored by calling `GET /api/users/me`.
- Logout clears cookies and blacklists JWT JTI values in Redis with token TTL.
- Google OAuth2 creates or loads a Google user and sets the same auth cookies.
- Forgot password sends an HTML email with a 15-minute reset token stored in Redis.
- `CUSTOMER` sessions expire after 15 minutes. `ORGANIZER` and `ADMIN` use the normal JWT durations.
- reCAPTCHA v3 can be enabled for login, register, and forgot-password to reduce bot traffic.
- Local accounts can be linked to Google login when both use the same email address.
- Users must verify their account email for reliable ticket QR delivery.
- Customers can register an organization with a business email. After business email verification, the user becomes an organizer.

Google OAuth2 requires valid `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` values in `application.yml` or environment variables.

## Cookie Defaults

Both auth cookies use:

- `HttpOnly=true`
- `Secure=false` for local development
- `Path=/`
- `SameSite=Lax`

Cookie names:

- `access_token`
- `refresh_token`

## Session Duration Policy

- `CUSTOMER`: 15 minutes for both access and refresh cookies.
- `ORGANIZER`: `app.jwt.expiration-ms` / `app.jwt.refresh-expiration-ms`.
- `ADMIN`: `app.jwt.expiration-ms` / `app.jwt.refresh-expiration-ms`.

Frontend reads `sessionExpiresAt` from `UserResponse`. For `CUSTOMER`, the app shows a warning around the 10-minute and 5-minute remaining marks, then signs the user out when the session expires.

Config:

```yml
app:
  session:
    customer-duration-ms: 900000
```

## reCAPTCHA v3

Backend verification is disabled by default for local development.

```yml
app:
  recaptcha:
    enabled: ${RECAPTCHA_ENABLED:false}
    secret: ${RECAPTCHA_SECRET:}
    min-score: ${RECAPTCHA_MIN_SCORE:0.5}
```

Frontend:

```text
VITE_RECAPTCHA_SITE_KEY=your_site_key
```

Protected actions:

- `login`
- `register`
- `forgot_password`

## Backend Endpoints

### `POST /api/auth/register`

Register a new local account.

Auth: public

Request:

```json
{
  "email": "user@example.com",
  "password": "Secure123",
  "fullName": "Nguyen Van A",
  "phone": "0912345678"
}
```

Response `201 Created`:

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "fullName": "Nguyen Van A",
  "phone": "0912345678",
  "avatarUrl": null,
  "role": "CUSTOMER",
  "status": "ACTIVE",
  "provider": "LOCAL",
  "sessionExpiresAt": "2026-05-14T03:30:00Z",
  "emailVerified": false,
  "primaryOrganizationId": null
}
```

The response also includes two `Set-Cookie` headers for `access_token` and `refresh_token`.

### `POST /api/auth/login`

Log in with email and password.

Auth: public

Request:

```json
{
  "email": "user@example.com",
  "password": "Secure123"
}
```

Response `200 OK`: returns `UserResponse` and sets `access_token` / `refresh_token` cookies.

Common errors:

- `401`: email or password is incorrect.
- `403`: account is blocked.

### `POST /api/auth/refresh`

Issue a new cookie session from the `refresh_token` cookie.

Auth: public, requires valid `refresh_token` cookie

Request body: none

Response `200 OK`: returns `UserResponse` and resets both auth cookies.

### `POST /api/auth/logout`

Log out the current browser session.

Auth: public, uses cookies when present

Behavior:

- Clears `access_token` and `refresh_token` cookies.
- Blacklists the JTI of both JWTs in Redis until each token expires.

Response `200 OK`:

```json
{
  "success": true,
  "message": "Logged out successfully",
  "data": null
}
```

### `GET /oauth2/authorization/google`

Start the Google OAuth2 login flow.

Auth: public

Behavior:

- Redirects the browser to Google.
- On success, backend creates or loads a `GOOGLE` user.
- Backend sets `access_token` and `refresh_token` cookies.
- Browser is redirected to `/oauth2/callback` on the frontend.

Rules:

- If the Google email already belongs to a `LOCAL` account, the Google identity is linked to the same user. Password login remains available because `password_hash` is kept.
- Blocked users cannot sign in.

### `POST /api/auth/verify-email`

Verify an account email from an email link.

Auth: public

Query:

```text
token=verification-token-from-email
```

Response `200 OK`:

```json
{
  "success": true,
  "message": "Email has been verified successfully.",
  "data": null
}
```

### `POST /api/auth/resend-verification-email`

Send another account email verification link.

Auth: cookie session required

Response `200 OK`:

```json
{
  "success": true,
  "message": "Verification email has been sent.",
  "data": null
}
```

### `POST /api/auth/forgot-password`

Request a password reset email.

Auth: public

Request:

```json
{
  "email": "user@example.com"
}
```

Response `200 OK`:

```json
{
  "success": true,
  "message": "If this email exists, a password reset link has been sent.",
  "data": null
}
```

Security behavior:

- Response is generic to avoid user enumeration.
- Redis rate limits by email: 3 requests per 15 minutes.
- Redis rate limits by IP: 10 requests per hour.
- Reset tokens are random opaque values; Redis stores only a SHA-256 token key.
- Token TTL is 15 minutes.

### `POST /api/auth/reset-password`

Reset a password using a valid reset token.

Auth: public

Request:

```json
{
  "token": "reset-token-from-email",
  "password": "NewSecure123"
}
```

Validation:

- Token must exist in Redis and must not be expired.
- Password must be at least 8 characters and include letters and numbers.

Response `200 OK`:

```json
{
  "success": true,
  "message": "Password has been reset successfully.",
  "data": null
}
```

Error:

- `400`: reset token is invalid or expired.
- `429`: too many reset email requests.

### `GET /api/users/me`

Return the current authenticated user from the `access_token` cookie.

Auth: requires valid `access_token` cookie

Response `200 OK`:

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "fullName": "Nguyen Van A",
  "phone": "0912345678",
  "avatarUrl": null,
  "role": "CUSTOMER",
  "status": "ACTIVE",
  "provider": "LOCAL",
  "sessionExpiresAt": "2026-05-14T03:30:00Z",
  "emailVerified": true,
  "primaryOrganizationId": null
}
```

## Organization Endpoints

### `POST /api/organizations/register`

Request organizer access by verifying a business email.

Auth: cookie session required

## Event Creation MVP

### Organization Gate

`/create-event` first checks `UserResponse.role` and `UserResponse.primaryOrganizationId`.

- If the signed-in user is still `CUSTOMER`, the page shows the organization registration form.
- The user enters organization name and business email.
- Backend sends a verification link to the business email.
- After `/organization/verify?token=...`, backend creates the organization and upgrades the user to `ORGANIZER`.
- After refresh/check-auth, the user can return to `/create-event` and access the event wizard.

### Wizard Steps

1. Event information
   - title
   - description
   - banner image URL
   - category
   - venue, city, address
   - start/end time
   - sale start/end time

2. Seat setup
   - `INTERNAL`: create local sections and auto-generate seats in `event_seats`.
   - `SEATS_IO`: paste the seats.io chart key. TicketRush stores `external_seat_chart_key` and keeps ticket ownership in its own DB.

3. Payment and confirmation
   - payout bank information
   - organizer terms acceptance
   - MVP publishes immediately as `PUBLISHED`; admin review can later change this to `PENDING_REVIEW`.

### `GET /api/categories`

Public category list for event forms and browsing.

### `GET /api/events`

Public published events. Home and Events pages use this endpoint.

### `POST /api/events`

Create and publish an event for a verified organizer.

Auth: cookie session required, role `ORGANIZER` or `ADMIN`

Request example:

```json
{
  "title": "Rock Night Hanoi",
  "description": "Live rock concert",
  "bannerUrl": "https://example.com/banner.jpg",
  "categoryId": 1,
  "locationName": "Hanoi Opera House",
  "city": "Hanoi",
  "address": "1 Trang Tien",
  "startTime": "2026-07-10T12:00:00Z",
  "endTime": "2026-07-10T15:00:00Z",
  "saleStartTime": "2026-06-01T00:00:00Z",
  "saleEndTime": "2026-07-09T12:00:00Z",
  "seatProvider": "INTERNAL",
  "termsAccepted": true,
  "sections": [
    { "name": "VIP", "basePrice": 1200000, "rowCount": 3, "seatsPerRow": 20 },
    { "name": "Standard", "basePrice": 500000, "rowCount": 8, "seatsPerRow": 30 }
  ],
  "payoutBankName": "VCB",
  "payoutAccountName": "TicketRush Live",
  "payoutAccountNumber": "0123456789"
}
```

For seats.io:

```json
{
  "seatProvider": "SEATS_IO",
  "externalSeatChartKey": "chart-key-from-seats-io",
  "sections": [
    { "name": "VIP", "basePrice": 1200000, "rowCount": 1, "seatsPerRow": 1 }
  ]
}
```

### seats.io Integration Notes

Using seats.io or another specialized seat-map tool is acceptable if TicketRush remains the system of record.

Recommended boundary:

- Frontend uses seats.io Designer/Renderer SDK for chart drawing and selection UX.
- Backend stores `seat_provider = 'SEATS_IO'` and `external_seat_chart_key`.
- TicketRush still owns event, order, ticket, payment, QR, and final seat booking state.
- Import or sync seat labels/categories from seats.io into TicketRush before sale starts.
- Booking must still lock/pay/issue tickets through TicketRush transactions.

Before committing to the vendor, review:

- pricing and request limits
- data export
- webhook reliability
- availability/SLA
- whether charts can be migrated away later
- whether their SDK license allows your planned use

Request:

```json
{
  "name": "TicketRush Live",
  "businessEmail": "events@company.com"
}
```

Response `200 OK`:

```json
{
  "success": true,
  "message": "Verification email has been sent to the business email.",
  "data": null
}
```

Rules:

- Default users remain `CUSTOMER` and can buy tickets.
- Organization is created only after business email verification.
- If the business email already exists in `organizations`, the request is rejected with `409`.
- After verification, the user role becomes `ORGANIZER`.
- The organizer can create and manage events under `primaryOrganizationId`.
- Organizer sessions use the standard JWT durations, not the customer session cap.

### `POST /api/organizations/verify`

Verify the business email and create the organization.

Auth: public

Query:

```text
token=organization-verification-token
```

Response `200 OK`:

```json
{
  "success": true,
  "message": "Organization has been verified successfully.",
  "data": {
    "id": "org-uuid",
    "name": "TicketRush Live",
    "businessEmail": "events@company.com",
    "ownerId": "user-uuid",
    "verifiedAt": "2026-05-14T03:30:00Z",
    "createdAt": "2026-05-14T03:30:00Z"
  }
}
```

### `GET /api/organizations/me`

Return organizations owned by the signed-in user.

Auth: cookie session required

## Frontend Flow

1. User submits `/register` or `/login`.
2. Frontend calls the API with `withCredentials: true`.
3. Backend sets JWT cookies and returns `UserResponse`.
4. Pinia stores only the `user` object in memory.
5. On app startup and route guard checks, frontend calls `GET /api/users/me`.
6. Protected routes redirect to `/login` when `/me` cannot restore a session.
7. On any non-auth-check API `401`, Axios redirects to `/login`.
8. Logout calls `POST /api/auth/logout`, then clears the user state in Pinia.

## Frontend Files

- `src/api/client.js`: Axios client with `withCredentials: true` and `401` redirect handling.
- `src/api/auth.api.js`: Auth API calls.
- `src/stores/authStore.js`: Cookie-session aware Pinia store.
- `src/router/index.js`: protected-route session restore and redirects.
- `src/views/LoginView.vue`: local login form.
- `src/views/RegisterView.vue`: local registration form.
- `src/views/ForgotPasswordView.vue`: request reset email form.
- `src/views/ResetPasswordView.vue`: reset password form.
- `src/views/VerifyEmailView.vue`: account email verification callback.
- `src/views/RegisterOrganizationView.vue`: business email organization registration form.
- `src/views/VerifyOrganizationView.vue`: organization verification callback.
- `src/views/OAuthSuccessView.vue`: placeholder callback that validates the cookie session.
- `src/layout/Header.vue`: reads login state from the auth store.
