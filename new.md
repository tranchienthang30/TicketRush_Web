# TicketRush - Latest Updates & Changes (May 18, 2026)

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

## Event Categories (Cập nhật May 18, 2026)

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

---

## Cinema Halls/Venues (Cập nhật May 18, 2026)

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

---

## Events Table Schema (Updated V17)

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

---

## Event Detail Page (🆕 May 18, 2026)

**Route:** `/events/:slug` (Public, no auth required)

**Component:** `EventDetailView.vue`

**File Location:** `d:\TicketRush\ticketFrontend\src\views\EventDetailView.vue`

### Features

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
   ```javascript
   canBook = event.listingType !== 'UPCOMING' 
             && (sale_start_time <= now)
             && (sale_end_time >= now)
   ```

### Data Loading

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

### Component Features

**Computed Properties:**
- `categoryName` - Lookup category name from categories array
- `heroImage` - Event banner URL with fallback to Unsplash
- `listingLabel` - "Now Showing", "Upcoming", or "Special"
- `durationText` - Format duration minutes to readable text (e.g., "2h 30m")
- `priceText` - Format minimum price with VND currency (e.g., "from 150,000 VND")
- `canBook` - Check if event is in sale window and not UPCOMING
- `keyFacts` - Array of event metadata (Category, Genre, Country, Duration, Price)
- `peopleFacts` - Array of credits (Author, Director, Cast, Performers, Singers)

**Methods:**
- `loadEvent()` - Fetch event details by slug via API
- `formatDateTime()` - Format timestamps to readable date/time
- `formatDuration()` - Convert minutes to "Xh Ym" format

### Code Example

```vue
<script setup>
import { computed, onMounted, ref } from "vue";
import { RouterLink, useRoute } from "vue-router";
import { getCategories, getEventBySlug } from "../api/ticketRushApi";

const eventDetail = ref(null);
const categories = ref([]);

const categoryName = computed(() => {
  const categoryId = Number(eventDetail.value?.categoryId);
  return categories.value.find((cat) => Number(cat.id) === categoryId)?.name || "Event";
});

const durationText = computed(() => formatDuration(eventDetail.value?.durationMinutes));

const canBook = computed(() => {
  const event = eventDetail.value;
  if (!event) return false;
  
  const listingType = String(event.listingType || "").toUpperCase();
  const now = Date.now();
  const saleStart = event.saleStartTime ? new Date(event.saleStartTime).getTime() : null;
  const saleEnd = event.saleEndTime ? new Date(event.saleEndTime).getTime() : null;

  return listingType !== "UPCOMING" && (!saleStart || saleStart <= now) && (!saleEnd || saleEnd >= now);
});

async function loadEvent() {
  try {
    const slug = String(route.params.slug || "");
    const [eventResponse, categoryResponse] = await Promise.all([
      getEventBySlug(slug),
      getCategories().catch(() => []),
    ]);
    eventDetail.value = eventResponse;
    categories.value = categoryResponse;
  } catch (err) {
    error.value = err.response?.data?.message || "Could not load this event.";
  }
}

function formatDuration(value) {
  const minutes = Number(value);
  if (!Number.isFinite(minutes) || minutes <= 0) return "TBA";
  
  const hours = Math.floor(minutes / 60);
  const remainingMinutes = minutes % 60;
  
  if (hours > 0 && remainingMinutes > 0) return `${hours}h ${remainingMinutes}m`;
  if (hours > 0) return `${hours}h`;
  return `${remainingMinutes}m`;
}

onMounted(loadEvent);
</script>
```

---

## 🔑 Provider Access Control (🆕 May 18-19, 2026)

### Provider Request Workflow
- **Customer** requests provider access → status = PENDING
- **Admin** reviews and approves/rejects requests
- **Approved** customer's role changes to PROVIDER, gains access to Creating & Managements
- **Rejected** customer stays CUSTOMER, receives optional rejection reason

### New Endpoints
| Method | Endpoint | Access | Purpose |
|--------|----------|--------|---------|
| POST | `/api/providers/request` | CUSTOMER | Submit provider access request |
| GET | `/api/admin/provider-requests` | ADMIN | List all pending requests |
| POST | `/api/admin/provider-requests/{userId}/approve` | ADMIN | Approve provider request |
| POST | `/api/admin/provider-requests/{userId}/reject` | ADMIN | Reject with optional reason |

### Database Changes (V15)
- `provider_reviewed_at` - When admin reviewed the request
- `provider_reviewed_by` - Which admin approved/rejected
- `provider_rejection_reason` - Reason if rejected

### Frontend Components
- **ProviderHomeView.vue** - Provider rules & workflow info
- **ProviderDashboardView.vue** - Stats (total events, published, seats configured)
- **AdminProviderRequestsView.vue** - Admin panel for reviewing requests with approve/reject UI

---

## 🪑 Seats.io Integration (🆕 May 19, 2026)

### New Table: provider_seat_workspaces
```sql
CREATE TABLE provider_seat_workspaces (
    provider_id UUID PRIMARY KEY,
    seatsio_workspace_id BIGINT,
    workspace_name VARCHAR(255) NOT NULL,
    workspace_key VARCHAR(255) NOT NULL UNIQUE,
    workspace_secret_key VARCHAR(255) NOT NULL,
    is_test BOOLEAN DEFAULT false,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ
)
```
- Each provider can have ONE Seats.io workspace (test or production)
- Supports both test and live modes

### Event Seat Provider Support
Events now support two seat providers:
- **INTERNAL** (default) - TicketRush manages seating
- **SEATS_IO** - External Seats.io service

New event columns:
- `external_seat_workspace_key` - Seats.io workspace key
- `external_seat_event_key` - Seats.io event chart key

---

## API Endpoints (Updated May 18-19)

### Event Detail API

**Endpoint:** `GET /api/events/slug/{slug}`

**Access:** Public (no authentication required)

**Response:** EventResponse
```json
{
  "id": "uuid",
  "title": "Indie Night Live",
  "slug": "indie-night-live",
  "description": "A live indie music night with local bands...",
  "bannerUrl": "https://...",
  "categoryId": 1,
  "genre": "Indie / Acoustic",
  "country": "Vietnam",
  "authorName": "Starlight Music Collective",
  "directorName": null,
  "castMembers": null,
  "performerNames": "The Velvet Days, Paper Lanterns, Mellow Park",
  "singerNames": "Minh An, Ha My",
  "locationName": "Starlight Main Hall",
  "city": "Ha Noi",
  "address": "72 Trang Tien, Hoan Kiem",
  "startTime": "2026-05-25T19:00:00Z",
  "endTime": "2026-05-25T21:30:00Z",
  "saleStartTime": "2026-05-18T00:00:00Z",
  "saleEndTime": "2026-05-25T18:00:00Z",
  "durationMinutes": 150,
  "listingType": "NOW_SHOWING",
  "minPrice": 250000,
  "availableSeats": 145,
  "soldSeats": 55
}
```

---

## Summary of Changes

### Database Layer (Migrations)
- **V16**: Rebrand to multi-event ticketing, add 8 categories, 8 venue halls, seed 26 sample events
- **V17**: Add 7 metadata columns for rich event discovery (genre, country, author, director, cast, performers, singers)

### Backend Layer (Java)
- **Event Entity**: Added 7 new fields for metadata
- **EventController**: New endpoint for fetching event by slug
- **EventServiceImpl**: New service method for event detail retrieval
- **EventQueryRepository**: Updated booking availability logic and duration calculation

### Frontend Layer (Vue)
- **Router**: New route `/events/:slug` for event detail page
- **API**: New `getEventBySlug()` function to fetch event details
- **Component**: New `EventDetailView.vue` with hero section, metadata display, and booking CTA

### Impact
- ✅ System now supports diverse event types (Music, Concert, Cinema, Sport, Theater, Workshop, Festival, Show)
- ✅ Customers can view rich event details with creator/performer information
- ✅ Event discovery improved with genre, country, and creator metadata
- ✅ Booking experience enhanced with clear "Buy Ticket" call-to-action
