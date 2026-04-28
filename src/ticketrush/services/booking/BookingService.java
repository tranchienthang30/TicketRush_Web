package ticketrush.services.booking;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

import ticketrush.database.DatabaseConnection;

import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

/**
 * BookingService - CRUD operations cho collection "bookings"
 * Snapshot Pattern: Lưu cứng thông tin tại thời điểm đặt vé
 */
public class BookingService {

    private static final String COLLECTION_NAME = "bookings";

    /**
     * Lấy MongoCollection bookings
     */
    private static MongoCollection<Document> getCollection() {
        return DatabaseConnection.getDatabase().getCollection(COLLECTION_NAME);
    }

    /**
     * Tạo mới đơn đặt vé (Snapshot Pattern)
     */
    public static ObjectId createBooking(String userId, String showtimeId,
            Document movieSnapshot, Document showtimeSnapshot,
            List<Document> seats, double totalPrice, String status) {
        try {
            Document booking = new Document()
                    .append("user_id", new ObjectId(userId))
                    .append("showtime_id", new ObjectId(showtimeId))
                    .append("movie_snapshot", movieSnapshot)
                    .append("showtime_snapshot", showtimeSnapshot)
                    .append("seats", seats)
                    .append("total_price", totalPrice)
                    .append("status", status) // pending, confirmed, cancelled
                    .append("booking_date", System.currentTimeMillis());

            getCollection().insertOne(booking);
            return booking.getObjectId("_id");
        } catch (Exception e) {
            System.err.println("✗ Lỗi tạo đơn đặt vé: " + e.getMessage());
            return null;
        }
    }

    /**
     * Lấy đơn đặt vé theo ID
     */
    public static Document getBookingById(String bookingId) {
        try {
            return getCollection().find(Filters.eq("_id", new ObjectId(bookingId))).first();
        } catch (Exception e) {
            System.err.println("✗ Lỗi lấy đơn đặt vé: " + e.getMessage());
            return null;
        }
    }

    /**
     * Lấy tất cả đơn đặt vé
     */
    public static List<Document> getAllBookings() {
        List<Document> bookings = new ArrayList<>();
        try {
            getCollection().find().into(bookings);
        } catch (Exception e) {
            System.err.println("✗ Lỗi lấy danh sách đơn đặt vé: " + e.getMessage());
        }
        return bookings;
    }

    /**
     * Lấy đơn đặt vé theo người dùng
     */
    public static List<Document> getBookingsByUser(String userId) {
        List<Document> bookings = new ArrayList<>();
        try {
            getCollection().find(Filters.eq("user_id", new ObjectId(userId))).into(bookings);
        } catch (Exception e) {
            System.err.println("✗ Lỗi lấy đơn đặt vé theo người dùng: " + e.getMessage());
        }
        return bookings;
    }

    /**
     * Lấy đơn đặt vé theo suất chiếu
     */
    public static List<Document> getBookingsByShowtime(String showtimeId) {
        List<Document> bookings = new ArrayList<>();
        try {
            getCollection().find(Filters.eq("showtime_id", new ObjectId(showtimeId))).into(bookings);
        } catch (Exception e) {
            System.err.println("✗ Lỗi lấy đơn đặt vé theo suất chiếu: " + e.getMessage());
        }
        return bookings;
    }

    /**
     * Lấy đơn đặt vé theo trạng thái
     */
    public static List<Document> getBookingsByStatus(String status) {
        List<Document> bookings = new ArrayList<>();
        try {
            getCollection().find(Filters.eq("status", status)).into(bookings);
        } catch (Exception e) {
            System.err.println("✗ Lỗi lấy đơn đặt vé theo trạng thái: " + e.getMessage());
        }
        return bookings;
    }

    /**
     * Cập nhật trạng thái đơn đặt vé
     */
    public static boolean updateBookingStatus(String bookingId, String newStatus) {
        try {
            var result = getCollection().updateOne(
                    Filters.eq("_id", new ObjectId(bookingId)),
                    Updates.set("status", newStatus));
            return result.getModifiedCount() > 0;
        } catch (Exception e) {
            System.err.println("✗ Lỗi cập nhật trạng thái: " + e.getMessage());
            return false;
        }
    }

    /**
     * Cập nhật danh sách ghế
     */
    public static boolean updateSeats(String bookingId, List<Document> newSeats) {
        try {
            var result = getCollection().updateOne(
                    Filters.eq("_id", new ObjectId(bookingId)),
                    Updates.set("seats", newSeats));
            return result.getModifiedCount() > 0;
        } catch (Exception e) {
            System.err.println("✗ Lỗi cập nhật ghế: " + e.getMessage());
            return false;
        }
    }

    /**
     * Cập nhật giá tổng
     */
    public static boolean updateTotalPrice(String bookingId, double newPrice) {
        try {
            var result = getCollection().updateOne(
                    Filters.eq("_id", new ObjectId(bookingId)),
                    Updates.set("total_price", newPrice));
            return result.getModifiedCount() > 0;
        } catch (Exception e) {
            System.err.println("✗ Lỗi cập nhật giá: " + e.getMessage());
            return false;
        }
    }

    /**
     * Xóa đơn đặt vé
     */
    public static boolean deleteBooking(String bookingId) {
        try {
            var result = getCollection().deleteOne(
                    Filters.eq("_id", new ObjectId(bookingId)));
            return result.getDeletedCount() > 0;
        } catch (Exception e) {
            System.err.println("✗ Lỗi xóa đơn đặt vé: " + e.getMessage());
            return false;
        }
    }

    /**
     * Đếm tổng số đơn đặt vé
     */
    public static long countBookings() {
        try {
            return getCollection().countDocuments();
        } catch (Exception e) {
            System.err.println("✗ Lỗi đếm đơn đặt vé: " + e.getMessage());
            return 0;
        }
    }
}
