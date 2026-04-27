package ticketrush;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public final class DataStore {
    public static final int LOCK_MINUTES = 10;
    public static final int QUEUE_LIMIT = 12;
    public static final int QUEUE_BATCH = 4;
    public static final int QUEUE_SECONDS = 180;

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC);

    public final List<MovieShow> shows = new CopyOnWriteArrayList<>();
    public final Map<String, User> usersByEmail = new ConcurrentHashMap<>();
    public final Map<String, String> sessions = new ConcurrentHashMap<>();
    public final List<Map<String, Object>> news = new CopyOnWriteArrayList<>();
    public final AtomicInteger showSeq = new AtomicInteger(1);
    public final AtomicInteger seatSeq = new AtomicInteger(1);
    public final AtomicInteger userSeq = new AtomicInteger(1);
    public final AtomicInteger orderSeq = new AtomicInteger(1);

    public DataStore() {
        seed();
    }

    public synchronized Map<String, Object> register(Map<String, Object> body) {
        String email = str(body.get("email")).toLowerCase();
        if (usersByEmail.containsKey(email)) throw new IllegalArgumentException("Email da ton tai");
        User user = new User();
        user.id = userSeq.getAndIncrement();
        user.name = str(body.get("name"));
        user.email = email;
        user.password = str(body.get("password"));
        user.points = 120;
        user.tier = "Silver";
        usersByEmail.put(email, user);
        return auth(user, issueToken(user));
    }

    public synchronized Map<String, Object> login(Map<String, Object> body) {
        User user = usersByEmail.get(str(body.get("email")).toLowerCase());
        if (user == null || !user.password.equals(str(body.get("password")))) {
            throw new IllegalArgumentException("Sai email hoac mat khau");
        }
        return auth(user, issueToken(user));
    }

    public synchronized Map<String, Object> session(String token) {
        User user = byToken(token);
        return user == null ? Map.of("authenticated", false) : auth(user, token);
    }

    public synchronized Map<String, Object> catalog() {
        cleanup();
        return Map.of(
            "shows", shows.stream().map(this::showCard).toList(),
            "theaters", theaterCards(),
            "news", news
        );
    }

    public synchronized Map<String, Object> showDetail(int showId, String clientId) {
        cleanup();
        MovieShow show = requireShow(showId);
        Map<String, Object> queue = touchQueue(show, clientId);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("show", showCard(show));
        payload.put("queue", queue);
        if (!"admitted".equals(queue.get("state"))) return payload;
        payload.put("zones", show.zones.stream().map(zone -> Map.of(
            "code", zone.code,
            "label", zone.label,
            "priceCents", zone.priceCents,
            "priceLabel", money(zone.priceCents)
        )).toList());
        payload.put("seats", show.seats.stream().map(seat -> seatMap(seat, clientId)).toList());
        List<Seat> mine = show.seats.stream()
            .filter(seat -> clientId.equals(seat.lockedBy) && "locked".equals(seat.status) && seat.lockedUntil != null && seat.lockedUntil.isAfter(Instant.now()))
            .toList();
        long total = mine.stream().mapToLong(seat -> seat.priceCents).sum();
        String expires = mine.isEmpty() ? "" : iso(mine.stream().map(seat -> seat.lockedUntil).min(Comparator.naturalOrder()).orElse(Instant.now()));
        payload.put("selection", Map.of("count", mine.size(), "totalCents", total, "expiresAt", expires));
        return payload;
    }

    public synchronized Map<String, Object> hold(int showId, String clientId, List<Object> seatIds) {
        cleanup();
        MovieShow show = requireShow(showId);
        ensureAdmitted(show, clientId);
        List<Integer> targetIds = seatIds.stream().map(item -> ((Number) item).intValue()).distinct().sorted().toList();
        if (targetIds.size() > 6) throw new IllegalArgumentException("Toi da 6 ghe moi don");
        Instant until = Instant.now().plus(Duration.ofMinutes(LOCK_MINUTES));
        for (Seat seat : show.seats) {
            if (targetIds.contains(seat.id)) {
                if ("sold".equals(seat.status)) throw new IllegalArgumentException("Ghe da ban");
                if ("locked".equals(seat.status) && !clientId.equals(seat.lockedBy) && seat.lockedUntil != null && seat.lockedUntil.isAfter(Instant.now())) {
                    throw new IllegalArgumentException("Ghe dang duoc nguoi khac giu");
                }
            }
        }
        for (Seat seat : show.seats) {
            boolean mine = clientId.equals(seat.lockedBy) && "locked".equals(seat.status);
            boolean wanted = targetIds.contains(seat.id);
            if (wanted) {
                seat.status = "locked";
                seat.lockedBy = clientId;
                seat.lockedUntil = until;
            } else if (mine) {
                seat.status = "available";
                seat.lockedBy = null;
                seat.lockedUntil = null;
            }
        }
        long total = show.seats.stream().filter(seat -> clientId.equals(seat.lockedBy) && "locked".equals(seat.status)).mapToLong(seat -> seat.priceCents).sum();
        return Map.of("ok", true, "totalCents", total, "expiresAt", iso(until));
    }

    public synchronized Map<String, Object> checkout(int showId, String clientId, Map<String, Object> body, String token) {
        cleanup();
        MovieShow show = requireShow(showId);
        ensureAdmitted(show, clientId);
        List<Seat> selected = show.seats.stream()
            .filter(seat -> clientId.equals(seat.lockedBy) && "locked".equals(seat.status) && seat.lockedUntil != null && seat.lockedUntil.isAfter(Instant.now()))
            .toList();
        if (selected.isEmpty()) throw new IllegalArgumentException("Khong co ghe hop le");
        Order order = new Order();
        order.id = orderSeq.getAndIncrement();
        order.showId = showId;
        order.clientId = clientId;
        order.customerName = str(body.get("name"));
        order.customerEmail = str(body.get("email"));
        order.age = ((Number) body.get("age")).intValue();
        order.gender = str(body.get("gender"));
        order.paidAt = Instant.now();
        long total = 0;
        for (Seat seat : selected) {
            total += seat.priceCents;
            seat.status = "sold";
            seat.lockedBy = null;
            seat.lockedUntil = null;
            String qr = "SR-" + order.id + "-" + seat.seatKey + "-" + UUID.randomUUID().toString().substring(0, 6);
            order.items.add(Map.of(
                "seatKey", seat.seatKey,
                "zoneLabel", seat.zoneLabel,
                "priceLabel", money(seat.priceCents),
                "qrPayload", qr,
                "qrSvg", qrSvg(qr)
            ));
        }
        order.totalCents = total;
        show.orders.add(order);
        User user = byToken(token);
        int earned = 0;
        if (user != null) {
            earned = (int) Math.max(20, total / 5_000_000);
            user.points += earned;
            user.tier = user.points >= 1200 ? "Platinum" : user.points >= 700 ? "Gold" : user.points >= 250 ? "Silver" : "Member";
        }
        return Map.of(
            "orderId", order.id,
            "eventTitle", show.title,
            "venue", show.venue,
            "startTime", iso(show.startTime),
            "customerName", order.customerName,
            "totalLabel", money(total),
            "pointsEarned", earned,
            "tickets", order.items
        );
    }

    public synchronized List<Object> orders(String token, String clientId) {
        cleanup();
        String email = token == null ? null : sessions.get(token);
        return new ArrayList<>(shows.stream()
            .flatMap(show -> show.orders.stream())
            .filter(order -> order.clientId.equals(clientId) || (email != null && email.equalsIgnoreCase(order.customerEmail)))
            .sorted(Comparator.comparing((Order order) -> order.paidAt).reversed())
            .map(order -> {
                MovieShow show = requireShow(order.showId);
                return Map.of(
                    "id", order.id,
                    "eventTitle", show.title,
                    "venue", show.venue,
                    "startTime", iso(show.startTime),
                    "customerName", order.customerName,
                    "customerEmail", order.customerEmail,
                    "totalLabel", money(order.totalCents),
                    "items", order.items
                );
            }).toList());
    }

    public synchronized Map<String, Object> dashboard() {
        cleanup();
        List<Object> audience = new ArrayList<>();
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (MovieShow show : shows) {
            for (Order order : show.orders) {
                String bucket = order.age < 18 ? "Under 18" : order.age <= 24 ? "18-24" : order.age <= 34 ? "25-34" : order.age <= 44 ? "35-44" : "45+";
                String key = order.gender + "|" + bucket;
                counts.put(key, counts.getOrDefault(key, 0) + 1);
            }
        }
        counts.forEach((key, value) -> {
            String[] parts = key.split("\\|");
            audience.add(Map.of("gender", parts[0], "ageBucket", parts[1], "count", value));
        });
        return Map.of(
            "events", shows.stream().map(show -> {
                long sold = show.seats.stream().filter(seat -> "sold".equals(seat.status)).count();
                long locked = show.seats.stream().filter(seat -> "locked".equals(seat.status) && seat.lockedUntil != null && seat.lockedUntil.isAfter(Instant.now())).count();
                long revenue = show.seats.stream().filter(seat -> "sold".equals(seat.status)).mapToLong(seat -> seat.priceCents).sum();
                return Map.of(
                    "id", show.id,
                    "title", show.title,
                    "startTime", iso(show.startTime),
                    "totalSeats", show.seats.size(),
                    "soldSeats", sold,
                    "lockedSeats", locked,
                    "fillRate", show.seats.isEmpty() ? 0 : Math.round((sold * 1000.0 / show.seats.size())) / 10.0,
                    "revenueLabel", money(revenue)
                );
            }).toList(),
            "audience", audience
        );
    }

    public synchronized Map<String, Object> createShow(Map<String, Object> body) {
        MovieShow show = new MovieShow();
        show.id = showSeq.getAndIncrement();
        show.title = str(body.get("title"));
        show.category = str(body.get("category"));
        show.venue = str(body.get("venue"));
        show.heroColor = str(body.get("heroColor"));
        show.description = str(body.get("description"));
        show.startTime = Instant.parse(str(body.get("startTime")));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> zones = (List<Map<String, Object>>) body.get("zones");
        for (Map<String, Object> item : zones) {
            Zone zone = new Zone();
            zone.code = str(item.get("code"));
            zone.label = str(item.get("label"));
            zone.rowsCount = ((Number) item.get("rowsCount")).intValue();
            zone.seatsPerRow = ((Number) item.get("seatsPerRow")).intValue();
            zone.priceCents = ((Number) item.get("priceCents")).longValue();
            show.zones.add(zone);
            createSeats(show, zone);
        }
        shows.add(show);
        return showCard(show);
    }

    private Map<String, Object> touchQueue(MovieShow show, String clientId) {
        Instant now = Instant.now();
        show.queue.removeIf(entry -> ("admitted".equals(entry.state) && entry.admittedUntil.isBefore(now)) || ("waiting".equals(entry.state) && entry.lastSeen.isBefore(now.minusSeconds(300))));
        QueueEntry entry = show.queue.stream().filter(item -> item.clientId.equals(clientId)).findFirst().orElse(null);
        if (entry == null) {
            long admitted = show.queue.stream().filter(item -> "admitted".equals(item.state) && item.admittedUntil.isAfter(now)).count();
            entry = new QueueEntry();
            entry.clientId = clientId;
            entry.createdAt = now;
            entry.lastSeen = now;
            entry.state = admitted < QUEUE_LIMIT ? "admitted" : "waiting";
            entry.admittedUntil = entry.state.equals("admitted") ? now.plusSeconds(QUEUE_SECONDS) : now;
            show.queue.add(entry);
        } else {
            entry.lastSeen = now;
        }
        admitWaiting(show);
        if ("admitted".equals(entry.state)) return Map.of("state", "admitted", "position", 0, "admittedUntil", iso(entry.admittedUntil));
        Instant createdAt = entry.createdAt;
        long ahead = show.queue.stream().filter(item -> "waiting".equals(item.state) && item.createdAt.isBefore(createdAt)).count();
        return Map.of("state", "waiting", "position", ahead + 1, "admittedUntil", "");
    }

    private void admitWaiting(MovieShow show) {
        Instant now = Instant.now();
        long admitted = show.queue.stream().filter(item -> "admitted".equals(item.state) && item.admittedUntil.isAfter(now)).count();
        int slots = (int) Math.max(0, QUEUE_LIMIT - admitted);
        if (slots == 0) return;
        show.queue.stream().filter(item -> "waiting".equals(item.state)).sorted(Comparator.comparing(item -> item.createdAt)).limit(Math.min(slots, QUEUE_BATCH)).forEach(item -> {
            item.state = "admitted";
            item.admittedUntil = now.plusSeconds(QUEUE_SECONDS);
            item.lastSeen = now;
        });
    }

    private void ensureAdmitted(MovieShow show, String clientId) {
        touchQueue(show, clientId);
        boolean admitted = show.queue.stream().anyMatch(item -> item.clientId.equals(clientId) && "admitted".equals(item.state) && item.admittedUntil.isAfter(Instant.now()));
        if (!admitted) throw new IllegalArgumentException("Queue admission required");
    }

    private void cleanup() {
        Instant now = Instant.now();
        for (MovieShow show : shows) {
            for (Seat seat : show.seats) {
                if ("locked".equals(seat.status) && seat.lockedUntil != null && seat.lockedUntil.isBefore(now)) {
                    seat.status = "available";
                    seat.lockedBy = null;
                    seat.lockedUntil = null;
                }
            }
            admitWaiting(show);
        }
    }

    private MovieShow requireShow(int id) {
        return shows.stream().filter(show -> show.id == id).findFirst().orElseThrow(() -> new IllegalArgumentException("Show not found"));
    }

    private User byToken(String token) {
        String email = token == null ? null : sessions.get(token);
        return email == null ? null : usersByEmail.get(email);
    }

    private String issueToken(User user) {
        String token = UUID.randomUUID().toString();
        sessions.put(token, user.email);
        return token;
    }

    private Map<String, Object> auth(User user, String token) {
        return Map.of(
            "authenticated", true,
            "token", token,
            "user", Map.of("name", user.name, "email", user.email, "points", user.points, "tier", user.tier)
        );
    }

    private List<Object> theaterCards() {
        Map<String, List<MovieShow>> grouped = new LinkedHashMap<>();
        for (MovieShow show : shows) grouped.computeIfAbsent(show.venue, key -> new ArrayList<>()).add(show);
        List<Object> items = new ArrayList<>();
        grouped.forEach((venue, venueShows) -> items.add(Map.of(
            "name", venue,
            "movies", venueShows.stream().map(show -> Map.of("title", show.title, "startTime", iso(show.startTime))).toList()
        )));
        return items;
    }

    private Map<String, Object> showCard(MovieShow show) {
        long sold = show.seats.stream().filter(seat -> "sold".equals(seat.status)).count();
        long locked = show.seats.stream().filter(seat -> "locked".equals(seat.status) && seat.lockedUntil != null && seat.lockedUntil.isAfter(Instant.now())).count();
        return Map.of(
            "id", show.id,
            "title", show.title,
            "category", show.category,
            "venue", show.venue,
            "startTime", iso(show.startTime),
            "heroColor", show.heroColor,
            "description", show.description,
            "pricing", show.zones.stream().map(zone -> Map.of(
                "code", zone.code,
                "label", zone.label,
                "priceCents", zone.priceCents,
                "priceLabel", money(zone.priceCents)
            )).toList(),
            "stats", Map.of("totalSeats", show.seats.size(), "soldSeats", sold, "lockedSeats", locked, "fillRate", show.seats.isEmpty() ? 0 : Math.round((sold * 1000.0 / show.seats.size())) / 10.0)
        );
    }

    private Map<String, Object> seatMap(Seat seat, String clientId) {
        String live = seat.status;
        if ("locked".equals(live) && seat.lockedUntil != null && seat.lockedUntil.isBefore(Instant.now())) live = "available";
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", seat.id);
        map.put("seatKey", seat.seatKey);
        map.put("zoneCode", seat.zoneCode);
        map.put("zoneLabel", seat.zoneLabel);
        map.put("rowLabel", seat.rowLabel);
        map.put("seatNumber", seat.seatNumber);
        map.put("priceCents", seat.priceCents);
        map.put("priceLabel", money(seat.priceCents));
        map.put("status", live);
        map.put("isMine", "locked".equals(live) && clientId.equals(seat.lockedBy));
        map.put("lockedUntil", seat.lockedUntil == null ? "" : iso(seat.lockedUntil));
        return map;
    }

    private void seed() {
        seedShow("Rain on the Ninth Floor", "Now Showing", "Starlight Downtown", "#f97316", "An intimate late-night romance built for premium seating and high-demand booking windows.", Instant.now().plus(Duration.ofDays(2)), List.of(
            zone("VIP", "VIP Hall", 4, 8, 160_000),
            zone("STD", "Standard Hall", 8, 14, 95_000),
            zone("BAL", "Balcony", 5, 12, 55_000)
        ));
        seedShow("Last Platform", "Now Showing", "Starlight Riverside", "#ef4444", "A tense action thriller timed for prime-time demand and rapid online seat selection.", Instant.now().plus(Duration.ofDays(3)), List.of(
            zone("VIP", "Premier Hall", 3, 8, 170_000),
            zone("STD", "Main Hall", 8, 15, 98_000),
            zone("ECO", "Eco Hall", 5, 10, 50_000)
        ));
        seedShow("Candy Planet", "Coming Soon", "Starlight West Lake", "#8b5cf6", "A family animation built around group seating, snack bundles, and real-time availability.", Instant.now().plus(Duration.ofDays(7)), List.of(
            zone("VIP", "Family Box", 3, 6, 150_000),
            zone("STD", "Rainbow Hall", 7, 12, 78_000),
            zone("BAL", "Sky Deck", 4, 10, 48_000)
        ));
        news.add(Map.of("title", "Riverside cinema expansion is now open", "summary", "A new branch with 4K projection, couple seats, and a faster online booking lane.", "tag", "Announcement"));
        news.add(Map.of("title", "Member Week is live", "summary", "Earn double reward points on confirmed web bookings throughout the campaign.", "tag", "Promotion"));
        news.add(Map.of("title", "Late-show snack hour", "summary", "Enjoy discounted combo bundles for screenings starting after 20:30.", "tag", "Update"));
        User demo = new User();
        demo.id = userSeq.getAndIncrement();
        demo.name = "Demo Member";
        demo.email = "member@starlightrush.vn";
        demo.password = "123456";
        demo.points = 540;
        demo.tier = "Gold";
        usersByEmail.put(demo.email, demo);
    }

    private void seedShow(String title, String category, String venue, String color, String description, Instant startTime, List<Zone> zones) {
        MovieShow show = new MovieShow();
        show.id = showSeq.getAndIncrement();
        show.title = title;
        show.category = category;
        show.venue = venue;
        show.heroColor = color;
        show.description = description;
        show.startTime = startTime;
        show.zones.addAll(zones);
        zones.forEach(zone -> createSeats(show, zone));
        shows.add(show);
    }

    private void createSeats(MovieShow show, Zone zone) {
        for (int row = 0; row < zone.rowsCount; row++) {
            String rowLabel = String.valueOf((char) ('A' + row));
            for (int num = 1; num <= zone.seatsPerRow; num++) {
                Seat seat = new Seat();
                seat.id = seatSeq.getAndIncrement();
                seat.seatKey = zone.code + "-" + rowLabel + String.format("%02d", num);
                seat.zoneCode = zone.code;
                seat.zoneLabel = zone.label;
                seat.rowLabel = rowLabel;
                seat.seatNumber = num;
                seat.priceCents = zone.priceCents;
                show.seats.add(seat);
            }
        }
    }

    private Zone zone(String code, String label, int rows, int perRow, long price) {
        Zone zone = new Zone();
        zone.code = code;
        zone.label = label;
        zone.rowsCount = rows;
        zone.seatsPerRow = perRow;
        zone.priceCents = price;
        return zone;
    }

    private static String money(long cents) {
        return String.format("%,d VND", cents).replace(",", ".");
    }

    private static String iso(Instant instant) {
        return ISO.format(instant);
    }

    private static String str(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String qrSvg(String payload) {
        int hash = Math.abs(payload.hashCode());
        StringBuilder rects = new StringBuilder();
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                if (((hash >> ((x + y) % 15)) & 1) == 1) rects.append("<rect x='").append(x).append("' y='").append(y).append("' width='1' height='1'/>");
            }
        }
        return "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 16 16' shape-rendering='crispEdges'><rect width='16' height='16' fill='#fff'/><g fill='#111827'>" + rects + "</g></svg>";
    }

    public static final class MovieShow {
        int id;
        String title;
        String category;
        String venue;
        String heroColor;
        String description;
        Instant startTime;
        List<Zone> zones = new ArrayList<>();
        List<Seat> seats = new ArrayList<>();
        List<QueueEntry> queue = new ArrayList<>();
        List<Order> orders = new ArrayList<>();
    }

    public static final class Zone {
        String code;
        String label;
        int rowsCount;
        int seatsPerRow;
        long priceCents;
    }

    public static final class Seat {
        int id;
        String seatKey;
        String zoneCode;
        String zoneLabel;
        String rowLabel;
        int seatNumber;
        long priceCents;
        String status = "available";
        String lockedBy;
        Instant lockedUntil;
    }

    public static final class QueueEntry {
        String clientId;
        String state;
        Instant createdAt;
        Instant admittedUntil;
        Instant lastSeen;
    }

    public static final class Order {
        int id;
        int showId;
        String clientId;
        String customerName;
        String customerEmail;
        int age;
        String gender;
        long totalCents;
        Instant paidAt;
        List<Map<String, Object>> items = new ArrayList<>();
    }

    public static final class User {
        int id;
        String name;
        String email;
        String password;
        int points;
        String tier;
    }
}
