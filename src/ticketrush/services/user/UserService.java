package ticketrush.services.user;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

import ticketrush.database.DatabaseConnection;

import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

/**
 * UserService - CRUD operations cho collection "users"
 */
public class UserService {

    private static final String COLLECTION_NAME = "users";

    /**
     * Lấy MongoCollection users
     */
    private static MongoCollection<Document> getCollection() {
        return DatabaseConnection.getDatabase().getCollection(COLLECTION_NAME);
    }

    /**
     * Tạo mới người dùng
     */
    public static ObjectId createUser(String email, String phone, String fullName, String password) {
        try {
            Document user = new Document()
                    .append("email", email)
                    .append("phone", phone)
                    .append("full_name", fullName)
                    .append("password", password) // Nên hash password trong thực tế
                    .append("points", 0)
                    .append("membership_level", "bronze") // bronze, silver, gold, platinum
                    .append("is_active", true)
                    .append("created_at", System.currentTimeMillis());

            getCollection().insertOne(user);
            return user.getObjectId("_id");
        } catch (Exception e) {
            System.err.println("✗ Lỗi tạo người dùng: " + e.getMessage());
            return null;
        }
    }

    /**
     * Lấy người dùng theo ID
     */
    public static Document getUserById(String userId) {
        try {
            return getCollection().find(Filters.eq("_id", new ObjectId(userId))).first();
        } catch (Exception e) {
            System.err.println("✗ Lỗi lấy người dùng: " + e.getMessage());
            return null;
        }
    }

    /**
     * Lấy người dùng theo email
     */
    public static Document getUserByEmail(String email) {
        try {
            return getCollection().find(Filters.eq("email", email)).first();
        } catch (Exception e) {
            System.err.println("✗ Lỗi lấy người dùng theo email: " + e.getMessage());
            return null;
        }
    }

    /**
     * Lấy người dùng theo phone
     */
    public static Document getUserByPhone(String phone) {
        try {
            return getCollection().find(Filters.eq("phone", phone)).first();
        } catch (Exception e) {
            System.err.println("✗ Lỗi lấy người dùng theo phone: " + e.getMessage());
            return null;
        }
    }

    /**
     * Lấy tất cả người dùng
     */
    public static List<Document> getAllUsers() {
        List<Document> users = new ArrayList<>();
        try {
            getCollection().find().into(users);
        } catch (Exception e) {
            System.err.println("✗ Lỗi lấy danh sách người dùng: " + e.getMessage());
        }
        return users;
    }

    /**
     * Lấy người dùng theo cấp độ thành viên
     */
    public static List<Document> getUsersByMembershipLevel(String level) {
        List<Document> users = new ArrayList<>();
        try {
            getCollection().find(Filters.eq("membership_level", level)).into(users);
        } catch (Exception e) {
            System.err.println("✗ Lỗi lấy người dùng theo cấp độ: " + e.getMessage());
        }
        return users;
    }

    /**
     * Cập nhật thông tin người dùng
     */
    public static boolean updateUser(String userId, String fullName, String phone) {
        try {
            var result = getCollection().updateOne(
                    Filters.eq("_id", new ObjectId(userId)),
                    Updates.combine(
                            Updates.set("full_name", fullName),
                            Updates.set("phone", phone)));
            return result.getModifiedCount() > 0;
        } catch (Exception e) {
            System.err.println("✗ Lỗi cập nhật người dùng: " + e.getMessage());
            return false;
        }
    }

    /**
     * Cộng điểm thành viên
     */
    public static boolean addPoints(String userId, int points) {
        try {
            var result = getCollection().updateOne(
                    Filters.eq("_id", new ObjectId(userId)),
                    Updates.inc("points", points));
            return result.getModifiedCount() > 0;
        } catch (Exception e) {
            System.err.println("✗ Lỗi cộng điểm: " + e.getMessage());
            return false;
        }
    }

    /**
     * Cập nhật cấp độ thành viên
     */
    public static boolean updateMembershipLevel(String userId, String level) {
        try {
            var result = getCollection().updateOne(
                    Filters.eq("_id", new ObjectId(userId)),
                    Updates.set("membership_level", level));
            return result.getModifiedCount() > 0;
        } catch (Exception e) {
            System.err.println("✗ Lỗi cập nhật cấp độ: " + e.getMessage());
            return false;
        }
    }

    /**
     * Vô hiệu hóa người dùng
     */
    public static boolean deactivateUser(String userId) {
        try {
            var result = getCollection().updateOne(
                    Filters.eq("_id", new ObjectId(userId)),
                    Updates.set("is_active", false));
            return result.getModifiedCount() > 0;
        } catch (Exception e) {
            System.err.println("✗ Lỗi vô hiệu hóa người dùng: " + e.getMessage());
            return false;
        }
    }

    /**
     * Xóa người dùng
     */
    public static boolean deleteUser(String userId) {
        try {
            var result = getCollection().deleteOne(
                    Filters.eq("_id", new ObjectId(userId)));
            return result.getDeletedCount() > 0;
        } catch (Exception e) {
            System.err.println("✗ Lỗi xóa người dùng: " + e.getMessage());
            return false;
        }
    }

    /**
     * Đếm tổng số người dùng
     */
    public static long countUsers() {
        try {
            return getCollection().countDocuments();
        } catch (Exception e) {
            System.err.println("✗ Lỗi đếm người dùng: " + e.getMessage());
            return 0;
        }
    }
}
