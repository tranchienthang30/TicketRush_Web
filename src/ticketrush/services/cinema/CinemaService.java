package ticketrush.services.cinema;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

import ticketrush.database.DatabaseConnection;

import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

/**
 * CinemaService - CRUD operations cho collection "cinemas"
 */
public class CinemaService {

    private static final String COLLECTION_NAME = "cinemas";

    /**
     * Lấy MongoCollection cinemas
     */
    private static MongoCollection<Document> getCollection() {
        return DatabaseConnection.getDatabase().getCollection(COLLECTION_NAME);
    }

    /**
     * Tạo mới rạp chiếu phim
     */
    public static ObjectId createCinema(String name, String city, String address,
            double latitude, double longitude, String phone) {
        try {
            Document cinema = new Document()
                    .append("name", name)
                    .append("city", city)
                    .append("address", address)
                    .append("location", new Document()
                            .append("type", "Point")
                            .append("coordinates", new double[] { longitude, latitude }))
                    .append("phone", phone)
                    .append("created_at", System.currentTimeMillis());

            getCollection().insertOne(cinema);
            return cinema.getObjectId("_id");
        } catch (Exception e) {
            System.err.println("✗ Lỗi tạo rạp: " + e.getMessage());
            return null;
        }
    }

    /**
     * Lấy rạp theo ID
     */
    public static Document getCinemaById(String cinemaId) {
        try {
            return getCollection().find(Filters.eq("_id", new ObjectId(cinemaId))).first();
        } catch (Exception e) {
            System.err.println("✗ Lỗi lấy rạp: " + e.getMessage());
            return null;
        }
    }

    /**
     * Lấy tất cả rạp
     */
    public static List<Document> getAllCinemas() {
        List<Document> cinemas = new ArrayList<>();
        try {
            getCollection().find().into(cinemas);
        } catch (Exception e) {
            System.err.println("✗ Lỗi lấy danh sách rạp: " + e.getMessage());
        }
        return cinemas;
    }

    /**
     * Lấy rạp theo thành phố
     */
    public static List<Document> getCinemasByCity(String city) {
        List<Document> cinemas = new ArrayList<>();
        try {
            getCollection().find(Filters.eq("city", city)).into(cinemas);
        } catch (Exception e) {
            System.err.println("✗ Lỗi lấy rạp theo thành phố: " + e.getMessage());
        }
        return cinemas;
    }

    /**
     * Tìm kiếm rạp theo tên
     */
    public static List<Document> searchCinemasByName(String keyword) {
        List<Document> cinemas = new ArrayList<>();
        try {
            getCollection().find(Filters.regex("name", keyword, "i")).into(cinemas);
        } catch (Exception e) {
            System.err.println("✗ Lỗi tìm kiếm rạp: " + e.getMessage());
        }
        return cinemas;
    }

    /**
     * Cập nhật thông tin rạp
     */
    public static boolean updateCinema(String cinemaId, String name, String address, String phone) {
        try {
            var result = getCollection().updateOne(
                    Filters.eq("_id", new ObjectId(cinemaId)),
                    Updates.combine(
                            Updates.set("name", name),
                            Updates.set("address", address),
                            Updates.set("phone", phone)));
            return result.getModifiedCount() > 0;
        } catch (Exception e) {
            System.err.println("✗ Lỗi cập nhật rạp: " + e.getMessage());
            return false;
        }
    }

    /**
     * Xóa rạp
     */
    public static boolean deleteCinema(String cinemaId) {
        try {
            var result = getCollection().deleteOne(
                    Filters.eq("_id", new ObjectId(cinemaId)));
            return result.getDeletedCount() > 0;
        } catch (Exception e) {
            System.err.println("✗ Lỗi xóa rạp: " + e.getMessage());
            return false;
        }
    }

    /**
     * Đếm tổng số rạp
     */
    public static long countCinemas() {
        try {
            return getCollection().countDocuments();
        } catch (Exception e) {
            System.err.println("✗ Lỗi đếm rạp: " + e.getMessage());
            return 0;
        }
    }
}
