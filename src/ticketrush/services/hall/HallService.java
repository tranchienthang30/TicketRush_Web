package ticketrush.services.hall;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

import ticketrush.database.DatabaseConnection;

import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

/**
 * HallService - CRUD operations cho collection "halls"
 */
public class HallService {

    private static final String COLLECTION_NAME = "halls";

    /**
     * Lấy MongoCollection halls
     */
    private static MongoCollection<Document> getCollection() {
        return DatabaseConnection.getDatabase().getCollection(COLLECTION_NAME);
    }

    /**
     * Tạo mới phòng chiếu
     */
    public static ObjectId createHall(String cinemaId, String name, int totalSeats, String format) {
        try {
            Document hall = new Document()
                    .append("cinema_id", new ObjectId(cinemaId))
                    .append("name", name)
                    .append("total_seats", totalSeats)
                    .append("format", format) // 2D, 3D, IMAX...
                    .append("created_at", System.currentTimeMillis());

            getCollection().insertOne(hall);
            return hall.getObjectId("_id");
        } catch (Exception e) {
            System.err.println("✗ Lỗi tạo phòng: " + e.getMessage());
            return null;
        }
    }

    /**
     * Lấy phòng theo ID
     */
    public static Document getHallById(String hallId) {
        try {
            return getCollection().find(Filters.eq("_id", new ObjectId(hallId))).first();
        } catch (Exception e) {
            System.err.println("✗ Lỗi lấy phòng: " + e.getMessage());
            return null;
        }
    }

    /**
     * Lấy tất cả phòng
     */
    public static List<Document> getAllHalls() {
        List<Document> halls = new ArrayList<>();
        try {
            getCollection().find().into(halls);
        } catch (Exception e) {
            System.err.println("✗ Lỗi lấy danh sách phòng: " + e.getMessage());
        }
        return halls;
    }

    /**
     * Lấy phòng theo rạp
     */
    public static List<Document> getHallsByCinema(String cinemaId) {
        List<Document> halls = new ArrayList<>();
        try {
            getCollection().find(Filters.eq("cinema_id", new ObjectId(cinemaId))).into(halls);
        } catch (Exception e) {
            System.err.println("✗ Lỗi lấy phòng theo rạp: " + e.getMessage());
        }
        return halls;
    }

    /**
     * Lấy phòng theo định dạng (2D, 3D, IMAX...)
     */
    public static List<Document> getHallsByFormat(String format) {
        List<Document> halls = new ArrayList<>();
        try {
            getCollection().find(Filters.eq("format", format)).into(halls);
        } catch (Exception e) {
            System.err.println("✗ Lỗi lấy phòng theo định dạng: " + e.getMessage());
        }
        return halls;
    }

    /**
     * Cập nhật thông tin phòng
     */
    public static boolean updateHall(String hallId, String name, int totalSeats, String format) {
        try {
            var result = getCollection().updateOne(
                    Filters.eq("_id", new ObjectId(hallId)),
                    Updates.combine(
                            Updates.set("name", name),
                            Updates.set("total_seats", totalSeats),
                            Updates.set("format", format)));
            return result.getModifiedCount() > 0;
        } catch (Exception e) {
            System.err.println("✗ Lỗi cập nhật phòng: " + e.getMessage());
            return false;
        }
    }

    /**
     * Xóa phòng
     */
    public static boolean deleteHall(String hallId) {
        try {
            var result = getCollection().deleteOne(
                    Filters.eq("_id", new ObjectId(hallId)));
            return result.getDeletedCount() > 0;
        } catch (Exception e) {
            System.err.println("✗ Lỗi xóa phòng: " + e.getMessage());
            return false;
        }
    }

    /**
     * Đếm tổng số phòng
     */
    public static long countHalls() {
        try {
            return getCollection().countDocuments();
        } catch (Exception e) {
            System.err.println("✗ Lỗi đếm phòng: " + e.getMessage());
            return 0;
        }
    }
}
