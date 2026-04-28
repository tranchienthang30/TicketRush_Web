package ticketrush.services.movie;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

import ticketrush.database.DatabaseConnection;

import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

/**
 * MovieService - CRUD operations cho collection "movies"
 */
public class MovieService {

    private static final String COLLECTION_NAME = "movies";

    /**
     * Lấy MongoCollection movies
     */
    private static MongoCollection<Document> getCollection() {
        return DatabaseConnection.getDatabase().getCollection(COLLECTION_NAME);
    }

    /**
     * Tạo mới phim
     */
    public static ObjectId createMovie(String title, String originalTitle, List<String> genre,
            int durationMin, String director, List<String> cast,
            String language, List<String> subtitles, String description,
            String posterUrl, String trailerUrl, double rating,
            String ageRestriction, long releaseDate, String status) {
        try {
            Document movie = new Document()
                    .append("title", title)
                    .append("original_title", originalTitle)
                    .append("genre", genre)
                    .append("duration_min", durationMin)
                    .append("director", director)
                    .append("cast", cast)
                    .append("language", language)
                    .append("subtitles", subtitles)
                    .append("description", description)
                    .append("poster_url", posterUrl)
                    .append("trailer_url", trailerUrl)
                    .append("rating", rating)
                    .append("age_restriction", ageRestriction)
                    .append("release_date", releaseDate)
                    .append("status", status)
                    .append("created_at", System.currentTimeMillis());

            getCollection().insertOne(movie);
            return movie.getObjectId("_id");
        } catch (Exception e) {
            System.err.println("✗ Lỗi tạo phim: " + e.getMessage());
            return null;
        }
    }

    /**
     * Lấy phim theo ID
     */
    public static Document getMovieById(String movieId) {
        try {
            return getCollection().find(Filters.eq("_id", new ObjectId(movieId))).first();
        } catch (Exception e) {
            System.err.println("✗ Lỗi lấy phim: " + e.getMessage());
            return null;
        }
    }

    /**
     * Lấy tất cả phim
     */
    public static List<Document> getAllMovies() {
        List<Document> movies = new ArrayList<>();
        try {
            getCollection().find().into(movies);
        } catch (Exception e) {
            System.err.println("✗ Lỗi lấy danh sách phim: " + e.getMessage());
        }
        return movies;
    }

    /**
     * Lấy phim theo trạng thái
     */
    public static List<Document> getMoviesByStatus(String status) {
        List<Document> movies = new ArrayList<>();
        try {
            getCollection().find(Filters.eq("status", status)).into(movies);
        } catch (Exception e) {
            System.err.println("✗ Lỗi lấy phim theo trạng thái: " + e.getMessage());
        }
        return movies;
    }

    /**
     * Tìm kiếm phim theo tên
     */
    public static List<Document> searchMoviesByTitle(String keyword) {
        List<Document> movies = new ArrayList<>();
        try {
            getCollection().find(Filters.regex("title", keyword, "i")).into(movies);
        } catch (Exception e) {
            System.err.println("✗ Lỗi tìm kiếm phim: " + e.getMessage());
        }
        return movies;
    }

    /**
     * Cập nhật thông tin phim
     */
    public static boolean updateMovie(String movieId, Document updateData) {
        try {
            var result = getCollection().updateOne(
                    Filters.eq("_id", new ObjectId(movieId)),
                    Updates.combine(
                            updateData.entrySet().stream()
                                    .map(e -> Updates.set(e.getKey(), e.getValue()))
                                    .toList()));
            return result.getModifiedCount() > 0;
        } catch (Exception e) {
            System.err.println("✗ Lỗi cập nhật phim: " + e.getMessage());
            return false;
        }
    }

    /**
     * Cập nhật trạng thái phim
     */
    public static boolean updateMovieStatus(String movieId, String newStatus) {
        try {
            var result = getCollection().updateOne(
                    Filters.eq("_id", new ObjectId(movieId)),
                    Updates.set("status", newStatus));
            return result.getModifiedCount() > 0;
        } catch (Exception e) {
            System.err.println("✗ Lỗi cập nhật trạng thái phim: " + e.getMessage());
            return false;
        }
    }

    /**
     * Xóa phim
     */
    public static boolean deleteMovie(String movieId) {
        try {
            var result = getCollection().deleteOne(
                    Filters.eq("_id", new ObjectId(movieId)));
            return result.getDeletedCount() > 0;
        } catch (Exception e) {
            System.err.println("✗ Lỗi xóa phim: " + e.getMessage());
            return false;
        }
    }

    /**
     * Đếm tổng số phim
     */
    public static long countMovies() {
        try {
            return getCollection().countDocuments();
        } catch (Exception e) {
            System.err.println("✗ Lỗi đếm phim: " + e.getMessage());
            return 0;
        }
    }
}
