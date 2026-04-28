package ticketrush.services.seat;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

import ticketrush.database.DatabaseConnection;

import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

/**
 * SeatService - CRUD operations cho collection "seats"
 */
public class SeatService {

    private static final String COLLECTION_NAME = "seats";

    /**
     * Lấy MongoCollection seats
     */
    private static MongoCollection<Document> getCollection() {
        return DatabaseConnection.getDatabase().getCollection(COLLECTION_NAME);
    }

    /**
     * Tạo mới ghế
     */
    public static ObjectId createSeat(String hallId, String label, String type, double price) {
        try {
            Document seat = new Document()
                    .append("hall_id", new ObjectId(hallId))
                    .append("label", label) // A1, A2, B1...
                    .append("type", type) // standard, vip, couple
                    .append("price", price)
                    .append("created_at", System.currentTimeMillis());

            getCollection().insertOne(seat);
            return seat.getObjectId("_id");
        } catch (Exception e) {
            System.err.println("✗ Lỗi tạo ghế: " + e.getMessage());
            return null;
        }
    }

    /**
     * Lấy ghế theo ID
     */
    public static Document getSeatById(String seatId) {
        try {
            return getCollection().find(Filters.eq("_id", new ObjectId(seatId))).first();
        } catch (Exception e) {
            System.err.println("✗ Lỗi lấy ghế: " + e.getMessage());
            return null;
        }
    }

    /**
     * Lấy tất cả ghế
     */
    public static List<Document> getAllSeats() {
        List<Document> seats = new ArrayList<>();
        try {
            getCollection().find().into(seats);
        } catch (Exception e) {
            System.err.println("✗ Lỗi lấy danh sách ghế: " + e.getMessage());
        }
        return seats;
    }

    /**
     * Lấy ghế theo phòng
     */
    public static List<Document> getSeatsByHall(String hallId) {
        List<Document> seats = new ArrayList<>();
        try {
            getCollection().find(Filters.eq("hall_id", new ObjectId(hallId))).into(seats);
        } catch (Exception e) {
            System.err.println("✗ Lỗi lấy ghế theo phòng: " + e.getMessage());
        }
        return seats;
    }

    /**
     * Lấy ghế theo loại
     */
    public static List<Document> getSeatsByType(String type) {
        List<Document> seats = new ArrayList<>();
        try {
            getCollection().find(Filters.eq("type", type)).into(seats);
        } catch (Exception e) {
            System.err.println("✗ Lỗi lấy ghế theo loại: " + e.getMessage());
        }
        return seats;
    }

    /**
     * Lấy ghế theo phòng và loại
     */
    public static List<Document> getSeatsByHallAndType(String hallId, String type) {
        List<Document> seats = new ArrayList<>();
        try {
            getCollection().find(Filters.and(
                    Filters.eq("hall_id", new ObjectId(hallId)),
                    Filters.eq("type", type))).into(seats);
        } catch (Exception e) {
            System.err.println("✗ Lỗi lấy ghế theo phòng và loại: " + e.getMessage());
        }
        return seats;
    }

    /**
     * Cập nhật thông tin ghế
     */
    public static boolean updateSeat(String seatId, String label, String type, double price) {
        try {
            var result = getCollection().updateOne(
                    Filters.eq("_id", new ObjectId(seatId)),
                    Updates.combine(
                            Updates.set("label", label),
                            Updates.set("type", type),
                            Updates.set("price", price)));
            return result.getModifiedCount() > 0;
        } catch (Exception e) {
            System.err.println("✗ Lỗi cập nhật ghế: " + e.getMessage());
            return false;
        }
    }

    /**
     * Xóa ghế
     */
    public static boolean deleteSeat(String seatId) {
        try {
            var result = getCollection().deleteOne(
                    Filters.eq("_id", new ObjectId(seatId)));
            return result.getDeletedCount() > 0;
        } catch (Exception e) {
            System.err.println("✗ Lỗi xóa ghế: " + e.getMessage());
            return false;
        }
    }

    /**
     * Đếm tổng số ghế
     */
    public static long countSeats() {
        try {
            return getCollection().countDocuments();
        } catch (Exception e) {
            System.err.println("✗ Lỗi đếm ghế: " + e.getMessage());
            return 0;
        }
    }
}
