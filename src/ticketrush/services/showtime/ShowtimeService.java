package ticketrush.services.showtime;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

import ticketrush.database.DatabaseConnection;

import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * ShowtimeService - CRUD operations cho collection "showtimes"
 */
public class ShowtimeService {

    private static final String COLLECTION_NAME = "showtimes";

    /**
     * Lấy MongoCollection showtimes
     */
    private static MongoCollection<Document> getCollection() {
        return DatabaseConnection.getDatabase().getCollection(COLLECTION_NAME);
    }

    /**
     * Tạo mới suất chiếu
     */
    public static ObjectId createShowtime(String movieId, String hallId, String cinemaId,
            long startTime, long endTime, int availableSeats) {
        try {
            Document showtime = new Document()
                    .append("movie_id", new ObjectId(movieId))
                    .append("hall_id", new ObjectId(hallId))
                    .append("cinema_id", new ObjectId(cinemaId))
                    .append("start_time", new Date(startTime))
                    .append("end_time", new Date(endTime))
                    .append("available_seats", availableSeats)
                    .append("created_at", System.currentTimeMillis());

            getCollection().insertOne(showtime);
            return showtime.getObjectId("_id");
        } catch (Exception e) {
            System.err.println("✗ Lỗi tạo suất chiếu: " + e.getMessage());
            return null;
        }
    }

    /**
     * Lấy suất chiếu theo ID
     */
    public static Document getShowtimeById(String showtimeId) {
        try {
            return getCollection().find(Filters.eq("_id", new ObjectId(showtimeId))).first();
        } catch (Exception e) {
            System.err.println("✗ Lỗi lấy suất chiếu: " + e.getMessage());
            return null;
        }
    }

    /**
     * Lấy tất cả suất chiếu
     */
    public static List<Document> getAllShowtimes() {
        List<Document> showtimes = new ArrayList<>();
        try {
            getCollection().find().into(showtimes);
        } catch (Exception e) {
            System.err.println("✗ Lỗi lấy danh sách suất chiếu: " + e.getMessage());
        }
        return showtimes;
    }

    /**
     * Lấy suất chiếu theo phim
     */
    public static List<Document> getShowtimesByMovie(String movieId) {
        List<Document> showtimes = new ArrayList<>();
        try {
            getCollection().find(Filters.eq("movie_id", new ObjectId(movieId))).into(showtimes);
        } catch (Exception e) {
            System.err.println("✗ Lỗi lấy suất chiếu theo phim: " + e.getMessage());
        }
        return showtimes;
    }

    /**
     * Lấy suất chiếu theo rạp
     */
    public static List<Document> getShowtimesByCinema(String cinemaId) {
        List<Document> showtimes = new ArrayList<>();
        try {
            getCollection().find(Filters.eq("cinema_id", new ObjectId(cinemaId))).into(showtimes);
        } catch (Exception e) {
            System.err.println("✗ Lỗi lấy suất chiếu theo rạp: " + e.getMessage());
        }
        return showtimes;
    }

    /**
     * Lấy suất chiếu theo phòng
     */
    public static List<Document> getShowtimesByHall(String hallId) {
        List<Document> showtimes = new ArrayList<>();
        try {
            getCollection().find(Filters.eq("hall_id", new ObjectId(hallId))).into(showtimes);
        } catch (Exception e) {
            System.err.println("✗ Lỗi lấy suất chiếu theo phòng: " + e.getMessage());
        }
        return showtimes;
    }

    /**
     * Lấy suất chiếu theo khoảng thời gian
     */
    public static List<Document> getShowtimesByTimeRange(long startTime, long endTime) {
        List<Document> showtimes = new ArrayList<>();
        try {
            getCollection().find(Filters.and(
                    Filters.gte("start_time", new Date(startTime)),
                    Filters.lte("start_time", new Date(endTime)))).into(showtimes);
        } catch (Exception e) {
            System.err.println("✗ Lỗi lấy suất chiếu theo thời gian: " + e.getMessage());
        }
        return showtimes;
    }

    /**
     * Cập nhật thông tin suất chiếu
     */
    public static boolean updateShowtime(String showtimeId, long startTime, long endTime, int availableSeats) {
        try {
            var result = getCollection().updateOne(
                    Filters.eq("_id", new ObjectId(showtimeId)),
                    Updates.combine(
                            Updates.set("start_time", new Date(startTime)),
                            Updates.set("end_time", new Date(endTime)),
                            Updates.set("available_seats", availableSeats)));
            return result.getModifiedCount() > 0;
        } catch (Exception e) {
            System.err.println("✗ Lỗi cập nhật suất chiếu: " + e.getMessage());
            return false;
        }
    }

    /**
     * Giảm ghế còn lại
     */
    public static boolean decreaseAvailableSeats(String showtimeId, int quantity) {
        try {
            var result = getCollection().updateOne(
                    Filters.eq("_id", new ObjectId(showtimeId)),
                    Updates.inc("available_seats", -quantity));
            return result.getModifiedCount() > 0;
        } catch (Exception e) {
            System.err.println("✗ Lỗi giảm ghế: " + e.getMessage());
            return false;
        }
    }

    /**
     * Xóa suất chiếu
     */
    public static boolean deleteShowtime(String showtimeId) {
        try {
            var result = getCollection().deleteOne(
                    Filters.eq("_id", new ObjectId(showtimeId)));
            return result.getDeletedCount() > 0;
        } catch (Exception e) {
            System.err.println("✗ Lỗi xóa suất chiếu: " + e.getMessage());
            return false;
        }
    }

    /**
     * Đếm tổng số suất chiếu
     */
    public static long countShowtimes() {
        try {
            return getCollection().countDocuments();
        } catch (Exception e) {
            System.err.println("✗ Lỗi đếm suất chiếu: " + e.getMessage());
            return 0;
        }
    }
}
