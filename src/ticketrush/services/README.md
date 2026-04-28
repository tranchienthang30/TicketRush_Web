# TicketRush CRUD Services Documentation

## 📁 Cấu trúc Thư mục

```
ticketrush/
├── services/
│   ├── movie/           ✓ MovieService.java
│   ├── cinema/          ✓ CinemaService.java
│   ├── hall/            ✓ HallService.java
│   ├── seat/            ✓ SeatService.java
│   ├── showtime/        ✓ ShowtimeService.java
│   ├── user/            ✓ UserService.java
│   ├── booking/         ✓ BookingService.java
│   └── payment/         ✓ PaymentService.java
├── Database/
│   ├── DatabaseConnection.java
│   └── DatabaseConnectionTest.java
└── ... (các file khác)
```

---

## 📚 Các Services Chi Tiết

### 1️⃣ **MovieService** (`services/movie/`)

**Quản lý thông tin phim**

```java
// Create - Tạo phim mới
ObjectId movieId = MovieService.createMovie(
    "Inception", "Inception", Arrays.asList("Sci-Fi", "Action"),
    148, "Christopher Nolan", Arrays.asList("Leonardo DiCaprio"),
    "English", Arrays.asList("Vietnamese"), "...",
    "poster.jpg", "trailer.mp4", 8.8, "T13", 1279238400000, "now_showing"
);

// Read - Lấy thông tin
Document movie = MovieService.getMovieById(movieId.toString());
List<Document> allMovies = MovieService.getAllMovies();
List<Document> showing = MovieService.getMoviesByStatus("now_showing");
List<Document> search = MovieService.searchMoviesByTitle("Inception");

// Update - Cập nhật
MovieService.updateMovieStatus(movieId.toString(), "coming_soon");

// Delete - Xóa
MovieService.deleteMovie(movieId.toString());
```

---

### 2️⃣ **CinemaService** (`services/cinema/`)

**Quản lý thông tin rạp chiếu phim**

```java
// Create
ObjectId cinemaId = CinemaService.createCinema(
    "CGV Landmark 81", "Ho Chi Minh City", "81 Nguyen Hue Blvd",
    10.7752, 106.7033, "028-3822-1111"
);

// Read
Document cinema = CinemaService.getCinemaById(cinemaId.toString());
List<Document> hcmCinemas = CinemaService.getCinemasByCity("Ho Chi Minh City");
List<Document> search = CinemaService.searchCinemasByName("CGV");

// Update
CinemaService.updateCinema(cinemaId.toString(), "CGV Landmark 81",
    "81 Nguyen Hue", "028-3822-1111");

// Delete
CinemaService.deleteCinema(cinemaId.toString());
```

---

### 3️⃣ **HallService** (`services/hall/`)

**Quản lý phòng chiếu**

```java
// Create - Tạo phòng chiếu mới
ObjectId hallId = HallService.createHall(
    cinemaId.toString(), "Hall A", 100, "IMAX"
);

// Read
Document hall = HallService.getHallById(hallId.toString());
List<Document> halls = HallService.getHallsByCinema(cinemaId.toString());
List<Document> imaxHalls = HallService.getHallsByFormat("IMAX");

// Update
HallService.updateHall(hallId.toString(), "Hall A", 100, "3D");

// Delete
HallService.deleteHall(hallId.toString());
```

---

### 4️⃣ **SeatService** (`services/seat/`)

**Quản lý ghế ngồi**

```java
// Create - Tạo ghế
ObjectId seatId = SeatService.createSeat(
    hallId.toString(), "A1", "standard", 150000
);

// Read
Document seat = SeatService.getSeatById(seatId.toString());
List<Document> hallSeats = SeatService.getSeatsByHall(hallId.toString());
List<Document> vipSeats = SeatService.getSeatsByType("vip");
List<Document> vipInHall = SeatService.getSeatsByHallAndType(hallId.toString(), "vip");

// Update
SeatService.updateSeat(seatId.toString(), "A1", "vip", 250000);

// Delete
SeatService.deleteSeat(seatId.toString());
```

---

### 5️⃣ **ShowtimeService** (`services/showtime/`)

**Quản lý suất chiếu**

```java
// Create - Tạo suất chiếu
long startTime = System.currentTimeMillis() + 86400000; // Ngày mai
long endTime = startTime + 10800000; // +3 giờ
ObjectId showtimeId = ShowtimeService.createShowtime(
    movieId.toString(), hallId.toString(), cinemaId.toString(),
    startTime, endTime, 100
);

// Read
Document showtime = ShowtimeService.getShowtimeById(showtimeId.toString());
List<Document> movieShowtimes = ShowtimeService.getShowtimesByMovie(movieId.toString());
List<Document> cinemaShowtimes = ShowtimeService.getShowtimesByCinema(cinemaId.toString());

// Update - Cập nhật ghế còn lại
ShowtimeService.decreaseAvailableSeats(showtimeId.toString(), 5);

// Delete
ShowtimeService.deleteShowtime(showtimeId.toString());
```

---

### 6️⃣ **UserService** (`services/user/`)

**Quản lý người dùng**

```java
// Create - Đăng ký người dùng
ObjectId userId = UserService.createUser(
    "user@example.com", "0123456789", "John Doe", "hashedPassword"
);

// Read
Document user = UserService.getUserById(userId.toString());
Document userByEmail = UserService.getUserByEmail("user@example.com");
List<Document> goldMembers = UserService.getUsersByMembershipLevel("gold");

// Update
UserService.updateUser(userId.toString(), "Jane Doe", "0987654321");
UserService.addPoints(userId.toString(), 100);
UserService.updateMembershipLevel(userId.toString(), "platinum");

// Deactivate
UserService.deactivateUser(userId.toString());

// Delete
UserService.deleteUser(userId.toString());
```

---

### 7️⃣ **BookingService** (`services/booking/`)

**Quản lý đơn đặt vé (Snapshot Pattern)**

```java
// Create - Tạo đơn đặt vé
Document movieSnapshot = new Document()
    .append("title", "Inception")
    .append("poster_url", "poster.jpg");

Document showtimeSnapshot = new Document()
    .append("start_time", new Date(System.currentTimeMillis()))
    .append("cinema_name", "CGV Landmark 81")
    .append("hall_name", "Hall A");

List<Document> seats = Arrays.asList(
    new Document("seat_id", "A1").append("label", "A1").append("type", "standard").append("price", 150000),
    new Document("seat_id", "A2").append("label", "A2").append("type", "standard").append("price", 150000)
);

ObjectId bookingId = BookingService.createBooking(
    userId.toString(), showtimeId.toString(),
    movieSnapshot, showtimeSnapshot, seats, 300000, "confirmed"
);

// Read
Document booking = BookingService.getBookingById(bookingId.toString());
List<Document> userBookings = BookingService.getBookingsByUser(userId.toString());
List<Document> pendingBookings = BookingService.getBookingsByStatus("pending");

// Update
BookingService.updateBookingStatus(bookingId.toString(), "cancelled");
BookingService.updateTotalPrice(bookingId.toString(), 280000);

// Delete
BookingService.deleteBooking(bookingId.toString());
```

---

### 8️⃣ **PaymentService** (`services/payment/`)

**Quản lý thanh toán**

```java
// Create - Tạo giao dịch thanh toán
ObjectId paymentId = PaymentService.createPayment(
    bookingId.toString(), userId.toString(), 300000,
    "credit_card", "pending"
);

// Read
Document payment = PaymentService.getPaymentById(paymentId.toString());
Document bookingPayment = PaymentService.getPaymentByBooking(bookingId.toString());
List<Document> userPayments = PaymentService.getPaymentsByUser(userId.toString());
List<Document> successPayments = PaymentService.getPaymentsByStatus("success");

// Update
PaymentService.updatePaymentStatus(paymentId.toString(), "success");
PaymentService.updateTransactionId(paymentId.toString(), "TXN_12345");

// Analytics
long totalPayments = PaymentService.countPayments();
double totalRevenue = PaymentService.getTotalRevenue();

// Delete
PaymentService.deletePayment(paymentId.toString());
```

---

## 🚀 Cách Sử Dụng Trong Code

### Ví dụ 1: Tìm Phim và Xem Lịch Chiếu

```java
public class MovieSearchExample {
    public static void main(String[] args) {
        // Tìm phim
        List<Document> movies = MovieService.searchMoviesByTitle("Inception");

        if (!movies.isEmpty()) {
            String movieId = movies.get(0).getObjectId("_id").toString();

            // Lấy lịch chiếu của phim
            List<Document> showtimes = ShowtimeService.getShowtimesByMovie(movieId);

            for (Document showtime : showtimes) {
                System.out.println("Start Time: " + showtime.getDate("start_time"));
                System.out.println("Available Seats: " + showtime.getInteger("available_seats"));
            }
        }
    }
}
```

### Ví dụ 2: Đặt Vé và Thanh Toán

```java
public class BookingFlowExample {
    public static void main(String[] args) {
        String userId = "..."; // User đã login
        String showtimeId = "..."; // User đã chọn suất chiếu

        // 1. Tạo Booking (Snapshot Pattern)
        ObjectId bookingId = BookingService.createBooking(
            userId, showtimeId,
            movieSnapshot, showtimeSnapshot, seats, totalPrice, "pending"
        );

        // 2. Giảm ghế còn lại
        ShowtimeService.decreaseAvailableSeats(showtimeId, 2);

        // 3. Tạo thanh toán
        ObjectId paymentId = PaymentService.createPayment(
            bookingId.toString(), userId, totalPrice, "credit_card", "pending"
        );

        // 4. Xử lý thanh toán (gateway)
        // ... call payment gateway ...

        // 5. Cập nhật trạng thái
        PaymentService.updatePaymentStatus(paymentId.toString(), "success");
        BookingService.updateBookingStatus(bookingId.toString(), "confirmed");
    }
}
```

---

## ⚙️ Import Statements

```java
// Tất cả Services
import ticketrush.services.movie.MovieService;
import ticketrush.services.cinema.CinemaService;
import ticketrush.services.hall.HallService;
import ticketrush.services.seat.SeatService;
import ticketrush.services.showtime.ShowtimeService;
import ticketrush.services.user.UserService;
import ticketrush.services.booking.BookingService;
import ticketrush.services.payment.PaymentService;
```

---

## 📝 Các Method CRUD Chung

Mỗi Service đều có các method cơ bản:

- ✅ **Create** - `create*(...)` → ObjectId
- ✅ **Read** - `get*ById()`, `getAll*()`, `get*By*()`, `search*()`
- ✅ **Update** - `update*()`, `add*()`, etc.
- ✅ **Delete** - `delete*()`
- ✅ **Count** - `count*()`

---

## 🔒 Best Practices

1. **Snapshot Pattern** - BookingService lưu cứng dữ liệu tại thời điểm đặt vé
2. **Transaction Management** - Kiểm tra kết quả return của update/delete
3. **Error Handling** - Tất cả services đều có try-catch
4. **ObjectId Conversion** - Chuyển String → ObjectId khi cần

---

## 📞 Liên Hệ & Support

Nếu có vấn đề gì, kiểm tra:

1. MongoDB connection có bật không
2. Import statement có đúng không
3. ObjectId conversion có đúng không
